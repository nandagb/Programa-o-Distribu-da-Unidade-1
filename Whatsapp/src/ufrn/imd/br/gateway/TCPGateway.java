package ufrn.imd.br.gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import ufrn.imd.br.gateway.strategy.GatewayStrategy;
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

    private void processHTTPRequest(BufferedReader request) {
        String headerLine;
        try {
            headerLine = request.readLine();
            StringTokenizer tokenizer = new StringTokenizer(headerLine);
            String httpMethod = tokenizer.nextToken();
            String httpPath = tokenizer.nextToken();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void processRequest(Socket connection) {
        System.out.println("Conection accepted!");

        BufferedReader clientRequest;
        try {
            clientRequest = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            PrintWriter gatewayResponse = new PrintWriter(connection.getOutputStream(), true);
            String headerLine = clientRequest.readLine();

            System.out.println("Message received: " + headerLine);

            StringTokenizer tokenizer = new StringTokenizer(headerLine);
            String httpMethod = tokenizer.nextToken();

            String httpPath = tokenizer.nextToken();

            System.out.println("HTTP METHOD: " + httpMethod);
            System.out.println("HTTP PATH: " + httpPath);

            switch( httpPath) {
                case "/messages":
                    //rotear para porta 9004 (enquanto não tem heartbeat)
                    // System.out.println("Creating connection with messages server 1");
                    // Socket serviceSocket = new Socket("localhost", 9004);

                    // BufferedReader serverResponse = new BufferedReader(new InputStreamReader(serviceSocket.getInputStream()));
                    // System.out.println("2");
                    // PrintWriter gatewayRequest = new PrintWriter(serviceSocket.getOutputStream(), true);

                    //// Read HTTP Headers
                    // int contentLength = 0;
                    // String line;
                    // System.out.println("3");

                    // while (!(line = clientRequest.readLine()).isEmpty()) {
                    //     System.out.println("4");
                    //     gatewayRequest.println(line);

                    //     if (line.startsWith("Content-Length:")) {
                    //         contentLength = Integer.parseInt(line.split(":")[1].trim());
                    //     }
                    // }
                    // gatewayRequest.println();
                    //// Read HTTP Headers
                    
                    /// Read HTTP Body
                    // char[] body = new char[contentLength];
                    // clientRequest.read(body, 0, contentLength);

                    // gatewayRequest.print(body);
                    // gatewayRequest.flush();
                    // /// 


                    // System.out.println("Calling messages server");
                    // break;
                // case "users";
                // default:

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
