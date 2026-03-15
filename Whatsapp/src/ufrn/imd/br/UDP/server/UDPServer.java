package ufrn.imd.br.UDP.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import ufrn.imd.br.Strategy;
import ufrn.imd.br.model.Message;
import ufrn.imd.br.service.Service;

public class UDPServer implements Strategy{
    private Message wppMessage;
	private String port;

    public UDPServer(String port){
		this.port = port;
    }

	public void interfaceMethod(Service service){
        System.out.println("This is the UDP Server Strategy!");


		wppMessage = new Message();
        System.out.println("UDP Server Messenger started");

        try {
			DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(port));
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

    public static void main(String[] args) {
		new UDPServer("9004");
	}
}
