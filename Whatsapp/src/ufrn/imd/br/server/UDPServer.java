package ufrn.imd.br.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.StringTokenizer;

import ufrn.imd.br.model.Message;
import ufrn.imd.br.server.strategy.ServerStrategy;
import ufrn.imd.br.service.Service;

public class UDPServer implements ServerStrategy{
    private Message wppMessage;
	private String port;
	private DatagramSocket serverSocket;
	private Service service;

    public UDPServer(String port){
		this.port = port;
    }

	private DatagramPacket processRequest(DatagramPacket packet) {
		//converte mensagem do cliente em bytes para texto
                                            // dados,              posição inicial, quantidade de bytes
        String message = new String(packet.getData(), 0,       packet.getLength());
        System.out.println("Server received this message: " + message);

		////
		String[] tokens = message.split(";", 3);
		String clientId = tokens[0] + ";" + tokens[1];
		////
		// apenas a parte importante da mensagem
		this.service.processMessage(tokens[2]);

		// responder cliente
		String reply = clientId + ";" + "OK";
		byte[] replymsg = reply.getBytes();

		// String clientAddress = tokens[0].startsWith("/") ? tokens[0].substring(1) : tokens[0];
		return new DatagramPacket(replymsg, replymsg.length, packet.getAddress(), packet.getPort());
	}

	public void interfaceMethod(Service service){
		this.service = service;

		wppMessage = new Message();
        System.out.println("UDP Server Messenger started");

		try {
			serverSocket = new DatagramSocket(Integer.parseInt(this.port));
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

				///PROCESS
				DatagramPacket serverPacket = processRequest(packetCopy);
				///PROCESS
				serverSocket.send(serverPacket);
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
				System.out.println("Enviando heartbeat, tum tum: " + gatewayAddress + ", " + gatewayPort);
				// InetAddress address = serverSocket.getLocalAddress();

				String msg = this.service.getType() + ":" + "127.0.0.1" + ":" + this.serverSocket.getLocalPort();
				byte[] heartBeatMessage = msg.getBytes();
				DatagramPacket heartBeatPacket = new DatagramPacket(heartBeatMessage, heartBeatMessage.length, gatewayAddress, this.gatewayPort);

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
		new UDPServer("9004");
	}
}
