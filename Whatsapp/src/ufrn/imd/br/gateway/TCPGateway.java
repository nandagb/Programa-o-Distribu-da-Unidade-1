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
import ufrn.imd.br.http.HTTPUtils;
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

        int size = services.size();
        int i = index.getAndUpdate(v -> (v + 1) % size);

        // old value of index
        return services.get(i);
    }

    private void updateService(String key){
        StringTokenizer tokenizer = new StringTokenizer(key, ":");
        ServiceRecord service;

        String serviceType = tokenizer.nextToken();

        switch(serviceType){
            case "users":
                synchronized (userServicesTable) {
                    service = userServicesTable.get(key);

                    if (service == null){
                        InetAddress address;
                        String addressName = tokenizer.nextToken();

                        try {
                            address = InetAddress.getByName(addressName);
                        } catch (UnknownHostException e) {
                            System.out.println("UnknownHostException: Não foi possível salvar o serviço com host: " + addressName);
                            // e.printStackTrace();
                            return;
                        }

                        int port = Integer.parseInt(tokenizer.nextToken());
                        userServicesTable.put(key, new ServiceRecord(address, port));
                        System.out.println("Servidor de Port: " + port + " iniciado");

                        return;
                    }
                }

                break;
            default:
                synchronized (messageServicesTable) {
                    service = messageServicesTable.get(key);

                    if (service == null){
                        String addressName = tokenizer.nextToken();
                        InetAddress address;

                        try {
                            address = InetAddress.getByName(addressName);
                        } catch (UnknownHostException e) {
                            System.out.println("UnknownHostException: Não foi possível salvar o serviço com host: " + addressName);
                            // e.printStackTrace();
                            return;
                        }

                        int port = Integer.parseInt(tokenizer.nextToken());
                        messageServicesTable.put(key, new ServiceRecord(address, port));
                        System.out.println("Servidor de Port: " + port + " iniciado");

                        return;
                    }
                }
        }

        if (!service.getStatus()) {
            System.out.println("Servidor de Port: " + service.getPort() + " iniciado");
        }

        service.refreshHeartBeat();
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
        } catch (IOException e) {
            System.out.println("IOException: Não foi possível ler a requestLine da requisição em getHTTPRequest");
            // e.printStackTrace();
            return null;
        }

        HTTPRequest request = new HTTPRequest(firstHeader);

        String line;
        try {
            while ((line = clientRequest.readLine()) != null && !line.isEmpty()) {
                headersBuilder.append(line).append("\r\n");
                if (line.startsWith("Content-Length:")) {
                    request.setContentLength(line);
                }
            }
        } catch (IOException e) {
            System.out.println("IOException: Não foi possível ler os headers da requisição em getHTTPRequest");
            // e.printStackTrace();
            return null;
        }

        request.setHeaders(headersBuilder.toString());
        if (request.getContentLength() > 0) {
            char[] body = new char[request.getContentLength()];
            try {
                clientRequest.read(body, 0, request.getContentLength());
            } catch (IOException e) {
                System.out.println("IOException: Não foi possível ler oo body da requisição em getHTTPRequest");
                // e.printStackTrace();
                return null;
            }

            request.setBody(body);
        }

        return request;
    }

    private HTTPResponse getHTTPResponse(BufferedReader serverResponse) {
        StringBuilder headersBuilder = new StringBuilder();
        String firstHeader;
        try {
            firstHeader = serverResponse.readLine();
        } catch (IOException e) {
            // e.printStackTrace();
            return getHTTPErrorResponse(503, "{\"error\":\"IOException: Não foi possível ler a statusLine da resposta em getHTTPResponse\"}");
        }

        HTTPResponse response = new HTTPResponse(firstHeader);

        String line;
        try {
            while ((line = serverResponse.readLine()) != null && !line.isEmpty()) {
                headersBuilder.append(line).append("\r\n");
                if (line.startsWith("Content-Length:")) {
                    response.setContentLength(line);
                }
            }
        } catch (IOException e) {
            // e.printStackTrace();
            return getHTTPErrorResponse(503, "{\"error\":\"IOException: Não foi possível ler os headers da resposta em getHTTPResponse\"}");
        }

        response.setHeaders(headersBuilder.toString());

        if (response.getContentLength() > 0) {
            char[] body = new char[response.getContentLength()];
            try {
                serverResponse.read(body, 0, response.getContentLength());
            } catch (IOException e) {
                // e.printStackTrace();
                return getHTTPErrorResponse(503, "{\"error\":\"IOException: Não foi possível ler o body da resposta em getHTTPResponse\"}");
            }
            response.setBody(body);
        }

        return response;
    }

    private HTTPResponse getHTTPErrorResponse(int code, String body) {
        String protocol = "HTTP/1.1";
        String status = HTTPUtils.mapStatus(code);
        String contentType = "application/json";
        int contentLength = body.length();

        StringBuilder headersBuilder = new StringBuilder();
        headersBuilder.append(protocol + " " + code + " " + status).append("\r\n");

        HTTPResponse response = new HTTPResponse(protocol, code, status);

        headersBuilder.append("Content-Type: " + contentType).append("\r\n");
        headersBuilder.append("Content-Length: " + contentLength).append("\r\n");

        response.setHeaders(headersBuilder.toString());
        response.setContentLength(contentLength);
        response.setBody(body);

        return response;
    }

    private void handleConnectionError(Socket connection, int code, String body) {
        HTTPResponse response = getHTTPErrorResponse(503, body);

        try {
            PrintWriter gatewayResponse = new PrintWriter(connection.getOutputStream());

            gatewayResponse.println(response.getStatusLine());
            gatewayResponse.println(response.getHeaders());

            if (response.getContentLength() > 0 ) {
                System.out.println("Body do Erro sendo retornado para o cliente: "  + response.getBody());
                gatewayResponse.print(response.getBody());
            }

            gatewayResponse.flush();
        } catch (IOException e) {
            System.out.println("Não foi possível retornar o erro para o cliente: " + body);
            // e.printStackTrace();
        }
    }

    private void processRequest(Socket connection) {
        // System.out.println("Conection accepted!");

        BufferedReader clientRequest = null;
        Socket serviceSocket = null;
        PrintWriter gatewayRequest = null;
        PrintWriter gatewayResponse = null;
        BufferedReader serverResponse = null;

        try {
            try {
                clientRequest = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } catch (IOException e) {
                handleConnectionError(connection, 503, "{\"error\":\"IOException: Não foi possível instanciar o clientRequest\"}");
                // e.printStackTrace();
                return;
            }

            HTTPRequest request = getHTTPRequest(clientRequest);
            if (request == null) {
                System.out.println("Não foi possível processar a requisição do Cliente!");
                return;
            }

            String path = request.getPath();
            ServiceRecord nextService = null;
            switch(path) {
                case "/messages":
                    synchronized (messageServicesTable) {
                        nextService = getNextService(messageServicesTable, messagesIndex);
                    }

                    break;
                case "/users":
                    synchronized (userServicesTable) {
                        nextService = getNextService(userServicesTable, usersIndex);
                    }

                    break;
                default:
                    handleConnectionError(connection, 503, "{\"error\":\" Serviço " + path + "não implementado!\"}");
                    return;
            }

            if (nextService == null) {
                System.out.println("Nenhum servidor disponível!");
                return;
            }

            System.out.println("Enviando requisição para servidor com porta: " + nextService.getPort());

            try {
                serviceSocket = new Socket("localhost", nextService.getPort());
            } catch (UnknownHostException e) {
                handleConnectionError(connection, 503, "{\"error\":\"UnknownHostException: Não foi possível instanciar o serviceSocket com a porta " + nextService.getPort() + "\"}");
                // e.printStackTrace();
                return;
            } catch (IOException e) {
                handleConnectionError(connection, 503, "{\"error\":\"IOException: Não foi possível instanciar o serviceSocket com a porta " + nextService.getPort() + "\"}");
                // e.printStackTrace();
                return;
            }

            try {
                gatewayRequest = new PrintWriter(serviceSocket.getOutputStream());
                // System.out.println("Client Request");
                // System.out.println("REQUEST HEADERS: " + request.getHeaders());

                gatewayRequest.println(request.getRequestLine());
                gatewayRequest.println(request.getHeaders());

                if (request.getContentLength() > 0 ) {
                    System.out.println("REQUEST BODY: " + request.getBody());
                    gatewayRequest.print(request.getBody());
                }

                gatewayRequest.flush();
            } catch (IOException e) {
                handleConnectionError(connection, 503, "{\"error\":\"IOException: Não foi possível instanciar o gatewayRequest\"}");
                // e.printStackTrace();
                return;
            }

            try {
                serverResponse = new BufferedReader(new InputStreamReader(serviceSocket.getInputStream()));
            } catch (IOException e) {
                handleConnectionError(connection, 503, "{\"error\":\"IOException: Não foi possível instanciar o serverResponse\"}");
                // e.printStackTrace();
                return;
            }

            try {
                gatewayResponse = new PrintWriter(connection.getOutputStream());
            } catch (IOException e) {
                handleConnectionError(connection, 503, "{\"error\":\"IOException: Não foi possível instanciar o gatewayResponse\"}");
                // e.printStackTrace();
                return;
            }

            HTTPResponse response = getHTTPResponse(serverResponse);

            if (response == null) {
                System.out.println("Não foi possível processar a resposta do Servidor!");
                return;
            }

            // System.out.println("Server response");
            // System.out.println("RESPONSE HEADERS: " + response.getHeaders());
            gatewayResponse.println(response.getStatusLine());
            gatewayResponse.println(response.getHeaders());

            if (response.getContentLength() > 0 ) {
                System.out.println("RESPONSE BODY FROM " + serviceSocket.getPort() + ": " + response.getBody());
                gatewayResponse.print(response.getBody());
            }

            gatewayResponse.flush();
        } catch (Exception e) {
            handleConnectionError(connection, 503, "{\"error\":\"Erro inesperado\"}");
            // e.printStackTrace();
        } finally {
            try { connection.close(); } catch (Exception ignored) {}
            try { if (clientRequest != null) clientRequest.close(); } catch (Exception ignored) {}
            try { if (serviceSocket != null) serviceSocket.close(); } catch (Exception ignored) {}
            try { if (gatewayRequest != null) gatewayRequest.close(); } catch (Exception ignored) {}
            try { if (gatewayResponse != null) gatewayResponse.close(); } catch (Exception ignored) {}
            try { if (serverResponse != null) serverResponse.close(); } catch (Exception ignored) {}
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
                System.out.println("IOException: Erro ao iniciar o servidor");
                // e.printStackTrace();
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
                        System.out.println("Não foi possível processar a requisição de Heartbeat do Servidor!");
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
                System.out.println("IOException: Erro ao iniciar o HeartBeat Listener");
                // e.printStackTrace();
            }
    }

    @Override
    public void failureDetector() {
        while(true) {
            // logServicesStatus();

            synchronized (messageServicesTable) {
                for (HashMap.Entry<String, ServiceRecord> entry : messageServicesTable.entrySet()) {
                    String key = entry.getKey();
                    ServiceRecord service = entry.getValue();

                    if (System.currentTimeMillis() - service.getLastHeartbeat() > heartBeatTimeout && service.getStatus()) {
                        System.out.println("Servidor de Port: " + service.getPort() + " morreu");
                        service.setStatus(false);
                    }
                }
            }

            synchronized (userServicesTable) {
                for (HashMap.Entry<String, ServiceRecord> entry : userServicesTable.entrySet()) {
                    String key = entry.getKey();
                    ServiceRecord service = entry.getValue();

                    if (System.currentTimeMillis() - service.getLastHeartbeat() > heartBeatTimeout && service.getStatus()) {
                        System.out.println("Servidor de Port: " + service.getPort() + " morreu");
                        service.setStatus(false);
                    }
                }
            }

            try {
                Thread.sleep(failureDetectorInterval);
            } catch (InterruptedException e) {
                System.out.println("InterruptedException: Erro ao iniciar o failureDetector!");
                // e.printStackTrace();
            }
        }
    }
    
}
