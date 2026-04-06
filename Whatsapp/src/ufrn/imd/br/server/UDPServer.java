package ufrn.imd.br.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ufrn.imd.br.http.HTTPRequest;
import ufrn.imd.br.http.HTTPResponse;
import ufrn.imd.br.model.Message;
import ufrn.imd.br.server.strategy.ServerStrategy;
import ufrn.imd.br.service.Service;
import ufrn.imd.br.service.ServiceResponse;

public class UDPServer implements ServerStrategy{
	private int port;
	private DatagramSocket serverSocket;
	private Service service;
	ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public UDPServer(int port){
		this.port = port;
    }

	private String mapStatus(int code) {
        switch (code) {
            case 200: return "OK";
            case 201: return "Created";
            case 400: return "Bad Request";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            default: return "Unknown";
        }
    }

	private HTTPResponse getServiceResponse(HTTPRequest request){
	  	ServiceResponse serviceResponse = this.service.processMessage(request.getMethod(), request.getBody(), request.getQueryParams());

	  	String protocol = "HTTP/1.1";
	  	int code = serviceResponse.getStatusCode();
	  	String status = mapStatus(code);
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

   private HTTPRequest getHTTPRequest(BufferedReader clientRequest) {
        StringBuilder headersBuilder = new StringBuilder();
        String firstHeader;

        try {
            firstHeader = clientRequest.readLine();
            HTTPRequest request = new HTTPRequest(firstHeader);

            String line;
            while ((line = clientRequest.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    request.setContentLength(line);
                }

                headersBuilder.append(line).append("\r\n");
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

	private DatagramPacket processRequest(DatagramPacket packet) {
		System.out.println("Conection accepted!");
		//converte mensagem do cliente em bytes para texto
                                            // dados,              posição inicial, quantidade de bytes
        String message = new String(packet.getData(), 0,       packet.getLength());
		BufferedReader messageReader = new BufferedReader(new StringReader(message));

		System.out.println("Servidor recebeu essa mensagem: " + message);

		HTTPRequest request = getHTTPRequest(messageReader);

		if (request == null) {
            System.out.println("Não foi possível processar a requisição!");
            //retornar erro sla
			return null;
        }
		else {
			HTTPResponse response = getServiceResponse(request);

            if (response == null) {
                System.out.println("Não foi possível processar a resposta!");
                //retornar erro sla
				return null;
            }
			else {
				response.setHeader("X-Client-IP" + ": " + request.getHeader("X-Client-IP"));
				response.setHeader("X-Client-Port" + ": " + request.getHeader("X-Client-Port"));

				String reply = response.toString();
				byte[] replymsg = reply.getBytes();

				return new DatagramPacket(replymsg, replymsg.length, packet.getAddress(), packet.getPort());
			}
		}
	}

	public void interfaceMethod(Service service){
		this.service = service;

        System.out.println("UDP Server Messenger started");

		try {
			serverSocket = new DatagramSocket(this.port);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		new Thread(() -> sendHeartbeat()).start();

		try {
			while (true) {
				byte[] clientMessage = new byte[1024];
				DatagramPacket clientPacket = new DatagramPacket(clientMessage, clientMessage.length);
				serverSocket.receive(clientPacket);

				DatagramPacket packetCopy = new DatagramPacket(
                    clientPacket.getData().clone(),
                    clientPacket.getLength(),
                    clientPacket.getAddress(),
                    clientPacket.getPort()
                );

				executor.execute(() -> {
                    DatagramPacket serverPacket = processRequest(packetCopy);

                    if (serverPacket == null) {
                        System.out.println("Não foi possível processar o pacote!");
                    }
                    else {
                        try {
                            serverSocket.send(serverPacket);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                });
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException nfe) {
			System.out.println("Erro ao converter numero: " + nfe.getMessage());

		} catch (Exception e) {
			System.out.println("Erro inesperado: " + e.getMessage());
		}

    }

	private void sendHeartbeat() {
		//Loop do HeartBeatSender
		try {
			//pode ser que mude depois
			String gatewayAdressString = "127.0.0.1";
			InetAddress gatewayAddress = InetAddress.getByName(gatewayAdressString);

			while (true) {
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

				byte[] heartBeatMessage = msg.getBytes();
				DatagramPacket heartBeatPacket = new DatagramPacket(heartBeatMessage, heartBeatMessage.length, gatewayAddress, this.heartBeatPort);

				this.serverSocket.send(heartBeatPacket);
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

    public static void main(String[] args) {
		// new UDPServer(9004);
	}
}
