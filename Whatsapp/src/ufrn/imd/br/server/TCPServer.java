package ufrn.imd.br.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ufrn.imd.br.http.HTTPRequest;
import ufrn.imd.br.http.HTTPResponse;
import ufrn.imd.br.http.HTTPUtils;
import ufrn.imd.br.server.strategy.ServerStrategy;
import ufrn.imd.br.service.Service;
import ufrn.imd.br.service.ServiceResponse;

public class TCPServer implements ServerStrategy {
    private int port;
    private Service service;
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

    private HTTPResponse getServiceResponse(HTTPRequest request){
		System.out.println("INSIDE GET SERVER RESPONSE");
	  	ServiceResponse serviceResponse = this.service.processMessage(request.getMethod(), request.getBody(), request.getQueryParams());

	  	String protocol = "HTTP/1.1";
	  	int code = serviceResponse.getStatusCode();
	  	String status = HTTPUtils.mapStatus(code);
	  	String contentType = "application/json";
	  	String body = serviceResponse.getBody();
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
                HTTPResponse response = getServiceResponse(request);
                // System.out.println("REQUEST METHOD: " + request.getMethod());
                // System.out.println("REQUEST PATH: " + request.getPath());
                // System.out.println("REQUEST QUERY: " + request.getQueryString());

                if (response == null) {
                    System.out.println("Não foi possível processar a resposta!");
                    //retornar erro sla
                }
                else {
                    PrintWriter serverResponse = new PrintWriter(connection.getOutputStream());

                    serverResponse.println(response.getStatusLine());
                    serverResponse.println(response.getHeaders());
                    if (response.getContentLength() > 0 ) {
                        serverResponse.print(response.getBody());
                    }
                    serverResponse.flush();
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void sendHeartbeat() {
		//Loop do HeartBeatSender
		try {
			//pode ser que mude depois
			String gatewayAdressString = "127.0.0.1";
			InetAddress gatewayAddress = InetAddress.getByName(gatewayAdressString);
            // Socket socket = new Socket(gatewayAddress, heartBeatPort);
            // PrintWriter heartBeatMessage = new PrintWriter(socket.getOutputStream(), true);

			while (true) {
				// System.out.println("Enviando heartbeat, tum tum: " + gatewayAddress + ", " + this.heartBeatPort);
				// InetAddress address = serverSocket.getLocalAddress();

				///// assembling request
				HTTPRequest request = new HTTPRequest("POST /heartbeat HTTP/1.1");
				request.setHeader("Host: localhost");
				request.setHeader("Content-Type: application/json");
				// Use if I ever switch to JSON
				// String body = "{\"service_type\":\"" + this.service.getType() + "\";" +
				//             	"\"ip\": \"" + gatewayAdressString  + "\";" +
				// 				"\"port\": \"" + this.serverSocket.getLocalPort()  + "\";" +
				//             	"}";
				String body = this.service.getType() + ":" + gatewayAdressString + ":" + this.port;
				int length = body.length();
				request.setHeader("Content-Length: " + length);
				request.setContentLength(length);
				request.setBody(body);
				/////

				String msg = request.toString();
				// System.out.println("Enviando heartbeat, tum tum: ");
				// System.out.println(msg);

				Socket socket = null;
                PrintWriter heartBeatMessage = null;

                try {
                    socket = new Socket(gatewayAddress, heartBeatPort);
                    heartBeatMessage = new PrintWriter(socket.getOutputStream(), true);

                    heartBeatMessage.println(msg);
                } finally {
                    if (heartBeatMessage != null) heartBeatMessage.close();
                    if (socket != null) socket.close();
                }

				// Interval for sending heartbeat (every 1s)
				Thread.sleep(heartBeatInterval);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException nfe) {
			System.out.println("Erro ao converter numero: " + nfe.getMessage());

		} catch (Exception e) {
			System.out.println("Erro inesperado: " + e.getMessage());
		}
	}

    public void interfaceMethod(Service service){
        this.service = service;
        System.out.println("This is the TCP Server Strategy!");

        new Thread(() -> sendHeartbeat()).start();

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
