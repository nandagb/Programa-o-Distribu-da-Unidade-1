package ufrn.imd.br.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import ufrn.imd.br.server.strategy.ServerStrategy;

public class UDPClient {
    private int gatewayPort = 9001;

    public UDPClient() {
        System.out.println("UDP Client Messenger");

        try {
            // Enviar requisição para o servidor
            DatagramSocket clientSocket = new DatagramSocket();
            DatagramPacket clientPacket;
            InetAddress inetAddress = InetAddress.getByName("localhost");

            byte[] clientMessage;

            System.out.println("Enviando um oiee da conta 1 para a conta 2\n");
            String message = "messages;send;1;2;oiee";

            clientMessage = message.getBytes();

                                            //mensagem em bytes,      tam da mensagem,      endereço de destino, porta de destino
            clientPacket = new DatagramPacket(clientMessage, clientMessage.length, inetAddress,         gatewayPort);
            clientSocket.send(clientPacket);

            // Receber resposta do servidor
            System.out.println("Cliente esperando resposta do servidor...");
            byte[] serverMessage = new byte[1024];
            DatagramPacket serverPacket = new DatagramPacket(serverMessage, serverMessage.length);
			clientSocket.receive(serverPacket);
			message = new String(serverPacket.getData());
			System.out.println("Resposta do servidor: " + message);

        
            clientSocket.close();
        } catch (IOException ex) {
		}

    }

    public static void main(String args[]){
        new UDPClient();
    }
    
}