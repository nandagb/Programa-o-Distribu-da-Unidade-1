package ufrn.imd.br.gateway;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import ufrn.imd.br.gateway.strategy.GatewayStrategy;
import ufrn.imd.br.model.ServiceRecord;
import ufrn.imd.br.service.Service;

public class UDPGateway implements GatewayStrategy {
    private ConcurrentHashMap<String, ServiceRecord> messageServicesTable;
    private ConcurrentHashMap<String, ServiceRecord> userServicesTable;
    AtomicInteger messagesIndex = new AtomicInteger(0);
    AtomicInteger usersIndex = new AtomicInteger(0);

    public UDPGateway() throws Exception{

        messageServicesTable = new ConcurrentHashMap<>();
        userServicesTable = new ConcurrentHashMap<>();
    }

    private ServiceRecord getNextService(ConcurrentHashMap<String, ServiceRecord> table, AtomicInteger index) {
        List<ServiceRecord> services = table.values()
        .stream()
        .filter(ServiceRecord::getStatus)
        .collect(Collectors.toList());

        if (services.isEmpty()) return null;

        int i = index.getAndUpdate(v -> (v + 1) % services.size());

        // old value of index
        return services.get(i);
    }

    private void updateService(String key){
        StringTokenizer tokenizer = new StringTokenizer(key, ":");
        ServiceRecord service;

        while (tokenizer.hasMoreElements()) {
            try {
                String serviceType = tokenizer.nextToken();
                switch(serviceType){
                    case "users":
                        service = userServicesTable.get(key);
                        if (service == null){
                            userServicesTable.put(key, service);
                            return;
                        }
                        break;
                    default:
                        service = messageServicesTable.get(key);
                        if (service == null){
                            messageServicesTable.put(key, new ServiceRecord(InetAddress.getByName(tokenizer.nextToken()), Integer.parseInt(tokenizer.nextToken())));
                            return;
                        }
                }

                service.refreshHeartBeat();
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void logServicesStatus() {
        System.out.println("---Serviços de Mensagens registrados---");
        for (HashMap.Entry<String, ServiceRecord> entry : messageServicesTable.entrySet()) {
            String key = entry.getKey();
            ServiceRecord service = entry.getValue();

            System.out.println("Port: " + service.getPort() + ", Status: " + service.getStatus());
        }
        System.out.println("---------------------------------------");

        System.out.println("---Serviços de Usuários registrados---");
        for (HashMap.Entry<String, ServiceRecord> entry : userServicesTable.entrySet()) {
            String key = entry.getKey();
            ServiceRecord service = entry.getValue();

            System.out.println("Port: " + service.getPort() + ", Status: " + service.getStatus());
        }
        System.out.println("---------------------------------------");
    }


    @Override
    public void server() {
        DatagramSocket socket;

		try {
            socket = new DatagramSocket(serverPort);
			while (true) {
                //receives messages from client
				byte[] clientMessage = new byte[1024];
				DatagramPacket clientPacket = new DatagramPacket(clientMessage, clientMessage.length);
				socket.receive(clientPacket);

				//converte mensagem do cliente em bytes para texto
                                            // dados,              posição inicial, quantidade de bytes
                String message = new String(clientPacket.getData(), 0,       clientPacket.getLength());
                //decodes message
                System.out.println("UDP Gateway received this message from the client: " + message);
                clientPacket.setAddress(InetAddress.getByName("localhost"));

                StringTokenizer tokenizer = new StringTokenizer(message, ";");
                String service = null;
                int messageSender = 0;
                int messageReceiver = 0;
                String messageContent = null;

                while (tokenizer.hasMoreElements()) {
                    service = tokenizer.nextToken();
                    messageSender = Integer.parseInt(tokenizer.nextToken());
                    messageReceiver = Integer.parseInt(tokenizer.nextToken());
                    messageContent = tokenizer.nextToken();
                }
				
                switch(service) {
                    case "messages":
                        if (messageServicesTable.isEmpty()) {
                            System.out.println("No available server!");
                            continue;
                        }

                        ServiceRecord nextService = getNextService(messageServicesTable, messagesIndex);

                        System.out.println("Sending request to messages server with port: " + nextService.getPort());
                        clientPacket.setPort(nextService.getPort());
                        break;
                    default:
                        System.out.println("No service specified");
                }
                System.out.println("after switch");

                //sends message to correct server
                socket.send(clientPacket);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException nfe) {
			System.out.println("Erro ao converter numero: " + nfe.getMessage());

		} catch (Exception e) {
			System.out.println("Erro inesperado: " + e.getMessage());
		}
    }

    @Override
    public void listenHeartBeat() {
        DatagramSocket socket;

        try {
            socket = new DatagramSocket(heartBeatPort);
			while (true) {
				byte[] serverMessage = new byte[1024];
				DatagramPacket serverPacket = new DatagramPacket(serverMessage, serverMessage.length);
				socket.receive(serverPacket);

				//converte mensagem do servidor em bytes para texto
                                            // dados,              posição inicial, quantidade de bytes
                String message = new String(serverPacket.getData(), 0,       serverPacket.getLength());
                // System.out.println("Gateway received this message from the heartbeat: " + message);

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

    @Override
    public void failureDetector() {
        while(true) {
            logServicesStatus();
        }
    }
    
}
