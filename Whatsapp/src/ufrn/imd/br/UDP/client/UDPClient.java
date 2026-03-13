package ufrn.imd.br.UDP.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class UDPClient {
    public UDPClient() {
        System.out.println("UDP Client Messenger");

        try {
         DatagramSocket clientSocket = new DatagramSocket();
        DatagramPacket clientPacket;
        InetAddress inetAddress = InetAddress.getByName("localhost");

        byte[] clientMessage;
		// byte[] serverMessage = new byte[1024];

        System.out.println("Enviando um oiee da conta 1 para a conta 2\n");
        String message = "1;2;oiee";

        clientMessage = message.getBytes();

                                        //mensagem em bytes,      tam da mensagem,      endereço de destino, porta de destino
        clientPacket = new DatagramPacket(clientMessage, clientMessage.length, inetAddress,         9004);
        clientSocket.send(clientPacket);

        
        clientSocket.close();
        } catch (IOException ex) {
		}

    }

    public static void main(String args[]){
        new UDPClient();
    }
    
}