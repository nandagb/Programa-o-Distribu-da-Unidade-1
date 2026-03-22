package ufrn.imd.br;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import ufrn.imd.br.model.ServiceRecord;

public class APIGateway {
    private ConcurrentHashMap<String, ServiceRecord> servicesTable;
    private DatagramSocket heartBeatSocket;
    private int heartBeatTimeout = 3000;
    private int failureDetectorInterval = 1000;

    public APIGateway(int port) throws Exception {
        heartBeatSocket = new DatagramSocket(port);
        servicesTable = new ConcurrentHashMap<>();
    }

    public String getServiceKey(InetAddress address, int port){
        return address.toString() + ":" + port;
    }

    public void addServiceRecord(String key, ServiceRecord service) {
        servicesTable.put(key, service);
    }

    public void logServicesStatus() {
        System.out.println("---Serviços registrados---");
        for (HashMap.Entry<String, ServiceRecord> entry : servicesTable.entrySet()) {
            String key = entry.getKey();
            ServiceRecord service = entry.getValue();

            System.out.println("Port: " + service.getPort() + ", Status: " + service.getStatus());
        }
        System.out.println("--------------------------");
    }

    public void updateService(String key){
        StringTokenizer tokenizer = new StringTokenizer(key, ":");
        ServiceRecord service = servicesTable.get(key);

        if (service == null){
            while (tokenizer.hasMoreElements()) {
                try {
                    addServiceRecord(key, new ServiceRecord(InetAddress.getByName(tokenizer.nextToken()), Integer.parseInt(tokenizer.nextToken())));
                } catch (NumberFormatException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            return;
        }

        service.refreshHeartBeat();
    }

    public void listen() {
        try {
			while (true) {
				byte[] serverMessage = new byte[1024];
				DatagramPacket serverPacket = new DatagramPacket(serverMessage, serverMessage.length);
				this.heartBeatSocket.receive(serverPacket);

				//converte mensagem do servidor em bytes para texto
                                            // dados,              posição inicial, quantidade de bytes
                String message = new String(serverPacket.getData(), 0,       serverPacket.getLength());
                System.out.println("Gateway received this message: " + message);

                updateService(message);

			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException nfe) {
			System.out.println("Erro ao converter numero: " + nfe.getMessage());

		} catch (Exception e) {
			System.out.println("Erro inesperado: " + e.getMessage());
		}
    }

    public void failureDetector() {
        while(true) {
            logServicesStatus();

            for (HashMap.Entry<String, ServiceRecord> entry : servicesTable.entrySet()) {
                String key = entry.getKey();
                ServiceRecord service = entry.getValue();

                if (System.currentTimeMillis() - service.getLastHeartbeat() > heartBeatTimeout) {
                    service.setStatus(false);
                }
            }

            try {
                Thread.sleep(failureDetectorInterval);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            APIGateway gateway = new APIGateway(9000);
            new Thread(() -> gateway.listen()).start();
            new Thread(() -> gateway.failureDetector()).start();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
