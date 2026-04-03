package ufrn.imd.br.gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import ufrn.imd.br.gateway.strategy.GatewayStrategy;
import ufrn.imd.br.http.HTTPRequest;
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

    private HTTPRequest getHTTPRequest(BufferedReader clientRequest) {
        StringBuilder headersBuilder = new StringBuilder();
        String firstHeader;

        try {
            firstHeader = clientRequest.readLine();
            HTTPRequest request = new HTTPRequest(firstHeader);

            headersBuilder.append(firstHeader).append("\r\n");

            String line;
            while ((line = clientRequest.readLine()) != null && !line.isEmpty()) {
                headersBuilder.append(line).append("\r\n");
                if (line.startsWith("Content-Length:")) {
                    request.setLength(line);
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
                //roteia apenas para 9004 (messages 1) por enquanto
                Socket serviceSocket = new Socket("localhost", 9004);
                PrintWriter gatewayRequest = new PrintWriter(serviceSocket.getOutputStream());
                BufferedReader serverResponse = new BufferedReader(new InputStreamReader(serviceSocket.getInputStream()));


                gatewayRequest.println(request.getHeaders());
                if (request.getContentLength() > 0 ) {
                    gatewayRequest.print(request.getBody());
                }
                gatewayRequest.flush();
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
                    System.out.println("TCP Gateway waiting for conection on port " + serverPort + "...");
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listenHeartBeat'");
    }

    @Override
    public void failureDetector() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'failureDetector'");
    }
    
}
