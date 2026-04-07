package ufrn.imd.br.gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import ufrn.imd.br.gateway.strategy.GatewayStrategy;
import ufrn.imd.br.http.HTTPRequest;
import ufrn.imd.br.http.HTTPResponse;
import ufrn.imd.br.model.ServiceRecord;

public class TCPGateway implements GatewayStrategy{
    private ConcurrentHashMap<String, ServiceRecord> messageServicesTable;
    private ConcurrentHashMap<String, ServiceRecord> userServicesTable;
    AtomicInteger messagesIndex = new AtomicInteger(0);
    AtomicInteger usersIndex = new AtomicInteger(0);
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public TCPGateway() {
        messageServicesTable = new ConcurrentHashMap<>();
        userServicesTable = new ConcurrentHashMap<>();
    }

    private ServiceRecord getNextService(ConcurrentHashMap<String, ServiceRecord> table, AtomicInteger index) {
        List<ServiceRecord> services = table.values()
        .stream()
        .filter(ServiceRecord::getStatus)
        .collect(Collectors.toList());

        if (services.isEmpty()) return null;

        int i = index.getAndUpdate(v -> (v + 1) % services.size());

        // old value of index
        return services.get(i);
    }

    private void updateService(String key){
        StringTokenizer tokenizer = new StringTokenizer(key, ":");
        ServiceRecord service;

        try {
            String serviceType = tokenizer.nextToken();

            switch(serviceType){
                case "users":
                    service = userServicesTable.get(key);
                    if (service == null){
                        InetAddress address = InetAddress.getByName(tokenizer.nextToken());
                        int port = Integer.parseInt(tokenizer.nextToken());
                        userServicesTable.put(key, new ServiceRecord(address, port));
                        System.out.println("Servidor de Port: " + port + " iniciado");
                        return;
                    }

                    break;
                default:
                    service = messageServicesTable.get(key);

                    if (service == null){
                        InetAddress address = InetAddress.getByName(tokenizer.nextToken());
                        int port = Integer.parseInt(tokenizer.nextToken());
                        messageServicesTable.put(key, new ServiceRecord(address, port));
                        System.out.println("Servidor de Port: " + port + " iniciado");
                        return;
                    }
            }

            service.refreshHeartBeat();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void logServicesStatus() {
        System.out.println("---Serviços de Mensagens registrados---");
        for (HashMap.Entry<String, ServiceRecord> entry : messageServicesTable.entrySet()) {
            String key = entry.getKey();
            ServiceRecord service = entry.getValue();

            System.out.println("Port: " + service.getPort() + ", Status: " + service.getStatus());
        }
        System.out.println("---------------------------------------");

        System.out.println("---Serviços de Usuários registrados---");
        for (HashMap.Entry<String, ServiceRecord> entry : userServicesTable.entrySet()) {
            String key = entry.getKey();
            ServiceRecord service = entry.getValue();

            System.out.println("Port: " + service.getPort() + ", Status: " + service.getStatus());
        }
        System.out.println("---------------------------------------");
    }

    private HTTPRequest getHTTPRequest(BufferedReader clientRequest) {
        StringBuilder headersBuilder = new StringBuilder();
        String firstHeader;

        try {
            firstHeader = clientRequest.readLine();
            HTTPRequest request = new HTTPRequest(firstHeader);

            String line;
            while ((line = clientRequest.readLine()) != null && !line.isEmpty()) {
                headersBuilder.append(line).append("\r\n");
                if (line.startsWith("Content-Length:")) {
                    request.setContentLength(line);
                }
            }
            request.setHeaders(headersBuilder.toString());
            if (request.getContentLength() > 0) {
                char[] body = new char[request.getContentLength()];
                clientRequest.read(body, 0, request.getContentLength());
                request.setBody(body);
            }

            return request;

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private HTTPResponse getHTTPResponse(BufferedReader serverResponse) {
        StringBuilder headersBuilder = new StringBuilder();
        String firstHeader;

        try {
            firstHeader = serverResponse.readLine();
            HTTPResponse response = new HTTPResponse(firstHeader);

            String line;
            while ((line = serverResponse.readLine()) != null && !line.isEmpty()) {
                headersBuilder.append(line).append("\r\n");
                if (line.startsWith("Content-Length:")) {
                    response.setContentLength(line);
                }
            }

            response.setHeaders(headersBuilder.toString());

            if (response.getContentLength() > 0) {
                char[] body = new char[response.getContentLength()];
                serverResponse.read(body, 0, response.getContentLength());
                response.setBody(body);
            }

            return response;

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private void processRequest(Socket connection) {
        System.out.println("Conection accepted!");

        BufferedReader clientRequest;
        try {
            clientRequest = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            HTTPRequest request = getHTTPRequest(clientRequest);

            if (request == null) {
                System.out.println("Não foi possível processar a requisição!");
                //retornar erro sla
            }
            else {
                String path = request.getPath();
                ServiceRecord nextService = null;
                switch(path) {
                    case "/messages":
                        nextService = getNextService(messageServicesTable, messagesIndex);
                        break;
                    case "/users":
                        nextService = getNextService(userServicesTable, messagesIndex);
                        break;
                    default:
                        System.out.println("Serviço não implementado!");
                }

                if (nextService != null) {
                    System.out.println("Sending request to messages server with port: " + nextService.getPort());

                    Socket serviceSocket = new Socket("localhost", nextService.getPort());
                    PrintWriter gatewayRequest = new PrintWriter(serviceSocket.getOutputStream());

                    System.out.println("Client Request");
                    System.out.println("REQUEST HEADERS: " + request.getHeaders());

                    gatewayRequest.println(request.getRequestLine());
                    gatewayRequest.println(request.getHeaders());

                    if (request.getContentLength() > 0 ) {
                        System.out.println("REQUEST BODY: " + request.getBody());
                        gatewayRequest.print(request.getBody());
                    }

                    gatewayRequest.flush();

                    BufferedReader serverResponse = new BufferedReader(new InputStreamReader(serviceSocket.getInputStream()));
                    PrintWriter gatewayResponse = new PrintWriter(connection.getOutputStream());

                    HTTPResponse response = getHTTPResponse(serverResponse);

                    if (response == null) {
                        System.out.println("Não foi possível processar a resposta!");
                        //retornar erro sla
                    }
                    else {
                        System.out.println("Server response");
                        System.out.println("RESPONSE HEADERS: " + response.getHeaders());
                        gatewayResponse.println(response.getStatusLine());
                        gatewayResponse.println(response.getHeaders());

                        if (response.getContentLength() > 0 ) {
                            System.out.println("RESPONSE BODY: " + response.getBody());
                            gatewayResponse.print(response.getBody());
                        }

                        gatewayResponse.flush();
                    }

                }

            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void server() {
            try {
                                                            //porta,  tamanho da fila
                ServerSocket serverSocket = new ServerSocket(serverPort, 1000);

                while(true) {
                    // System.out.println("TCP Gateway waiting for conection on port " + serverPort + "...");
                    Socket connection = serverSocket.accept();

                    executor.execute(() -> processRequest(connection));

                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

    @Override
    public void listenHeartBeat() {
        try {
                                                            //porta,  tamanho da fila
                ServerSocket heartBeatSocket = new ServerSocket(heartBeatPort, 1000);

                while(true) {
                    // System.out.println("TCP Gateway HeartBeat waiting for conection on port " + heartBeatPort + "...");
                    Socket connection = heartBeatSocket.accept();

                    // System.out.println("HeartBeat Connection accepted!");

                    BufferedReader serverRequest = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    HTTPRequest request = getHTTPRequest(serverRequest);
                    // HTTPRequest request = null;

                    if (request == null) {
                        System.out.println("Não foi possível processar a requisição!");
                        //retornar erro sla
                    }
                    else {
                        // System.out.println("Client Request");
                        // System.out.println(request.getRequestLine());
                        // System.out.println(request.getHeaders());

                        // if (request.getContentLength() > 0 ) {
                        //     System.out.println("REQUEST BODY: " + request.getBody());
                        // }

                        updateService(request.getBody());
                    }

                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

    @Override
    public void failureDetector() {
        while(true) {
            // logServicesStatus();

            for (HashMap.Entry<String, ServiceRecord> entry : messageServicesTable.entrySet()) {
                String key = entry.getKey();
                ServiceRecord service = entry.getValue();

                if (System.currentTimeMillis() - service.getLastHeartbeat() > heartBeatTimeout) {
                    System.out.println("Servidor de Port: " + service.getPort() + " morreu");
                    service.setStatus(false);
                }
            }

            for (HashMap.Entry<String, ServiceRecord> entry : userServicesTable.entrySet()) {
                String key = entry.getKey();
                ServiceRecord service = entry.getValue();

                if (System.currentTimeMillis() - service.getLastHeartbeat() > heartBeatTimeout) {
                    System.out.println("Servidor de Port: " + service.getPort() + " morreu");
                    service.setStatus(false);
                }
            }

            try {
                Thread.sleep(failureDetectorInterval);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
}
