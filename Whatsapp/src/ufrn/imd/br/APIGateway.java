package ufrn.imd.br;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;

import ufrn.imd.br.model.ServiceRecord;

public class APIGateway {
    private HashMap<String, ServiceRecord> servicesTable;
    static DatagramSocket heartBeatSocket;

    public APIGateway(){
        servicesTable = new HashMap<>();
    }

    public String getServiceKey(InetAddress address, int port){
        return address.toString() + port;
    }

    public void addServiceRecord(String key, ServiceRecord service) {
        servicesTable.put(key, service);
    }

    public void updateService(InetAddress address, int port){
        String key = getServiceKey(address, port);

        ServiceRecord service = servicesTable.get(key);

        if (service == null){
            addServiceRecord(key, new ServiceRecord(address, port));
            return;
        }

        service.setStatus(true);
    }

    public static void listen(DatagramSocket socket) {
        try {
			while (true) {
				byte[] serverMessage = new byte[1024];
				DatagramPacket serverPacket = new DatagramPacket(serverMessage, serverMessage.length);
				socket.receive(serverPacket);

				//converte mensagem do servidor em bytes para texto
                                            // dados,              posição inicial, quantidade de bytes
                String message = new String(serverPacket.getData(), 0,       serverPacket.getLength());
                System.out.println("Gateway received this message: " + message);
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
        try {
			heartBeatSocket = new DatagramSocket(9000);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        new Thread(() -> listen(heartBeatSocket)).start();
    }
}
