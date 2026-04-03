package ufrn.imd.br.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ufrn.imd.br.http.HTTPRequest;
import ufrn.imd.br.server.strategy.ServerStrategy;
import ufrn.imd.br.service.Service;

public class TCPServer implements ServerStrategy {
    private int port;
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public TCPServer(int port){
        this.port = port;
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
                System.out.println("REQUEST METHOD: " + request.getMethod());
                System.out.println("REQUEST PATH: " + request.getPath());
                System.out.println("REQUEST QUERY: " + request.getQueryString());
            }

            PrintWriter serverResponse = new PrintWriter(connection.getOutputStream(), true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void interfaceMethod(Service service){
        System.out.println("This is the TCP Server Strategy!");

        try {
                                                         //porta,  tamanho da fila
            ServerSocket serverSocket = new ServerSocket(this.port, 1000);

            while(true) {
                System.out.println("TCP Server waiting for conection on port " + this.port + "...");
                Socket connection = serverSocket.accept();

                executor.execute(() -> processRequest(connection));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // service.processMessage("random message tcp");
    }
}
