package ufrn.imd.br.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import ufrn.imd.br.model.Message;
import ufrn.imd.br.server.strategy.ServerStrategy;
import ufrn.imd.br.service.Service;

public class UDPServer implements ServerStrategy{
    private Message wppMessage;
	private String port;
	private DatagramSocket serverSocket;
	private int heartBeatInterval = 1000;
	private int gatewayPort = 9000;
	private Service service;

    public UDPServer(String port){
		this.port = port;
    }

	public void interfaceMethod() {

	}

	public void interfaceMethod(Service service){
		this.service = service;
        System.out.println("This is the UDP Server Strategy!");

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

				//converte mensagem do cliente em bytes para texto
                                            // dados,              posição inicial, quantidade de bytes
                String message = new String(clientPacket.getData(), 0,       clientPacket.getLength());
                System.out.println("Server received this message: " + message);
				//call this inside the while loop of the server
				this.service.processMessage(message);


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
				System.out.println("Enviando heartbeat, tum tum: " + gatewayAddress + ", " + this.gatewayPort);
				// InetAddress address = serverSocket.getLocalAddress();

				String msg = this.service.getType() + ":" + "127.0.0.1" + ":" + this.serverSocket.getLocalPort();
				byte[] heartBeatMessage = msg.getBytes();
				DatagramPacket heartBeatPacket = new DatagramPacket(heartBeatMessage, heartBeatMessage.length, gatewayAddress, this.gatewayPort);

				this.serverSocket.send(heartBeatPacket);
				//Intervalo para envio de heartbeat (a cada 1s)
				Thread.sleep(this.heartBeatInterval);
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
