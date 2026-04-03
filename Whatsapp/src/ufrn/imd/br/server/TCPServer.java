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

import ufrn.imd.br.server.strategy.ServerStrategy;
import ufrn.imd.br.service.Service;

public class TCPServer implements ServerStrategy {
    private int port;
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public TCPServer(int port){
        this.port = port;
    }

    private void processRequest(Socket connection) {
        System.out.println("Conection accepted!");

        BufferedReader clientMessage;
        try {
            clientMessage = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            PrintWriter serverMessage = new PrintWriter(connection.getOutputStream(), true);
            String headerLine = clientMessage.readLine();

            System.out.println("Message received: " + headerLine);

            StringTokenizer tokenizer = new StringTokenizer(headerLine);
            String httpMethod = tokenizer.nextToken();

            System.out.println("HTTP METHOD: " + httpMethod);

            String contentLenght = clientMessage.readLine();
            System.out.println("SOMETHING: " + contentLenght);

            String something = clientMessage.readLine();
            System.out.println("SOMETHING: " + something);
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
