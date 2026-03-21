package ufrn.imd.br.UDP.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import ufrn.imd.br.Strategy;
import ufrn.imd.br.model.Message;
import ufrn.imd.br.service.Service;

public class UDPServer implements Strategy{
    private Message wppMessage;
	private String port;
	DatagramSocket serverSocket;

    public UDPServer(String port){
		this.port = port;
    }

	public void interfaceMethod(Service service){
        System.out.println("This is the UDP Server Strategy!");

		wppMessage = new Message();
        System.out.println("UDP Server Messenger started");

		try {
			serverSocket = new DatagramSocket(Integer.parseInt(port));
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		new Thread(() -> sendHeartbeat(serverSocket)).start();

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
				service.processMessage(message);


			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException nfe) {
			System.out.println("Erro ao converter numero: " + nfe.getMessage());

		} catch (Exception e) {
			System.out.println("Erro inesperado: " + e.getMessage());
		}

    }

	private void sendHeartbeat(DatagramSocket serverSocket) {
		//Loop do HeartBeatSender
		try {
			//pode ser que mude depois
			InetAddress gatewayAddress = InetAddress.getByName("127.0.0.1");
			int gatewayPort = 9000;

			while (true) {
				System.out.println("Enviando heartbeat, tum tum");
				String msg = "HEARTBEAT";
				byte[] heartBeatMessage = msg.getBytes();
				DatagramPacket heartBeatPacket = new DatagramPacket(heartBeatMessage, heartBeatMessage.length, gatewayAddress, gatewayPort);

				serverSocket.send(heartBeatPacket);
				//Intervalo para envio de heartbeat (a cada 1s)
				Thread.sleep(1000);
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
