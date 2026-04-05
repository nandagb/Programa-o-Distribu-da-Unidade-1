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

public class UDPServer implements ServerStrategy{
	private int port;
	private DatagramSocket serverSocket;
	private Service service;
	ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public UDPServer(int port){
		this.port = port;
    }

	private void processMessage(String message){
      System.out.println("processing message in Message Service: " + message);

      String[] tokens = message.split(";");
      String method = tokens[0];
      System.out.println("METHOD: " + tokens[0]);

      switch(method) {
         case "send":
			//calls service here
			// this.service.sendMessage(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), tokens[2])
            // sendMessage(tokens);
            break;
         case "delete":
            break;
         default:
            System.out.println("\"Method No Allowed\"");
      }
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

	private HTTPResponse assembleHTTPResponse() {
        String protocol = "HTTP/1.1";
        int code = 200;
        String status = "OK";
        String contentType = "application/json";
        String body = "{\"status\":\"ok\"}";
        int contentLength = body.length();

        StringBuilder headersBuilder = new StringBuilder();
        headersBuilder.append(protocol + " " + code + " " + status).append("\r\n");

        HTTPResponse response = new HTTPResponse("HTTP/1.1", 200, "OK");

        headersBuilder.append("Content-Type: " + contentType).append("\r\n");
        headersBuilder.append("Content-Length: " + contentLength).append("\r\n");

        response.setHeaders(headersBuilder.toString());
        response.setContentLength(contentLength);
        response.setBody(body);

        return response;
    }

	private DatagramPacket processRequest(DatagramPacket packet) {
		//converte mensagem do cliente em bytes para texto
                                            // dados,              posição inicial, quantidade de bytes
        String message = new String(packet.getData(), 0,       packet.getLength());
		BufferedReader messageReader = new BufferedReader(new StringReader(message));

		System.out.println("Server received this message: " + message);

		HTTPRequest request = getHTTPRequest(messageReader);

		if (request == null) {
            System.out.println("Não foi possível processar a requisição!");
            //retornar erro sla
        }
		else {
			// this.service.processMessage(tokens[2]);

			System.out.println("CLIENT IP RECEIVED FROM GATEWAY IN UDP SERVER: " + request.getHeader("X-Client-IP"));
			System.out.println("CLIENT PORT RECEIVED FROM GATEWAY IN UDP SERVER: " + request.getHeader("X-Client-Port"));
			System.out.println("CLIENT HEADERS: " + request.getHeaders());

			HTTPResponse response = assembleHTTPResponse();

            if (response == null) {
                System.out.println("Não foi possível processar a resposta!");
                //retornar erro sla
            }
			else {
				response.setHeader("X-Client-IP" + ": " + request.getHeader("X-Client-IP"));
				response.setHeader("X-Client-Port" + ": " + request.getHeader("X-Client-Port"));

				StringBuilder messageBuilder = new StringBuilder();

				messageBuilder.append(response.getStatusLine()).append("\r\n");
				messageBuilder.append(response.getHeaders()).append("\r\n");
                messageBuilder.append("\r\n");
                if (response.getContentLength() > 0) {
                    messageBuilder.append(response.getBody()).append("\r\n");
                }

                String reply = messageBuilder.toString();
				System.out.println("REPLY MESSAGE FOR GATEWAY: " + reply);
				byte[] replymsg = reply.getBytes();

				return new DatagramPacket(replymsg, replymsg.length, packet.getAddress(), packet.getPort());
			}
		}

		////
		String[] tokens = message.split(";", 3);
		String clientId = tokens[0] + ";" + tokens[1];
		////
		// apenas a parte importante da mensagem
		// this.service.processMessage(tokens[2]);
		processMessage(tokens[2]);

		// responder cliente
		String reply = clientId + ";" + "OK";
		byte[] replymsg = reply.getBytes();

		// String clientAddress = tokens[0].startsWith("/") ? tokens[0].substring(1) : tokens[0];
		return new DatagramPacket(replymsg, replymsg.length, packet.getAddress(), packet.getPort());
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
			InetAddress gatewayAddress = InetAddress.getByName("127.0.0.1");

			while (true) {
				// System.out.println("Enviando heartbeat, tum tum: " + gatewayAddress + ", " + this.heartBeatPort);
				// InetAddress address = serverSocket.getLocalAddress();

				String msg = this.service.getType() + ":" + "127.0.0.1" + ":" + this.serverSocket.getLocalPort();
				byte[] heartBeatMessage = msg.getBytes();
				DatagramPacket heartBeatPacket = new DatagramPacket(heartBeatMessage, heartBeatMessage.length, gatewayAddress, this.heartBeatPort);

				this.serverSocket.send(heartBeatPacket);
				//Intervalo para envio de heartbeat (a cada 1s)
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
