package ufrn.imd.br.gateway;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import ufrn.imd.br.gateway.strategy.GatewayStrategy;
import ufrn.imd.br.model.ServiceRecord;
import ufrn.imd.br.service.Service;

public class UDPGateway implements GatewayStrategy {
    private int serverPort;

    private ConcurrentHashMap<String, ServiceRecord> messageServicesTable;
    private ConcurrentHashMap<String, ServiceRecord> userServicesTable;
    AtomicInteger messagesIndex = new AtomicInteger(0);
    AtomicInteger usersIndex = new AtomicInteger(0);

    public UDPGateway(int port){
        this.serverPort = port;

        messageServicesTable = new ConcurrentHashMap<>();
        userServicesTable = new ConcurrentHashMap<>();
    }

    public ServiceRecord getNextService(ConcurrentHashMap<String, ServiceRecord> table, AtomicInteger index) {
        List<ServiceRecord> services = table.values()
        .stream()
        .filter(ServiceRecord::getStatus)
        .collect(Collectors.toList());

        if (services.isEmpty()) return null;

        int i = index.getAndUpdate(v -> (v + 1) % services.size());

        // old value of index
        return services.get(i);
    }

    @Override
    public void server() {DatagramSocket socket;

		try {
            socket = new DatagramSocket(this.serverPort);
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'server'");
    }

    @Override
    public void listenHeartBeat() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listenHeartBeat'");
    }

    @Override
    public void failureDetector() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'failureDetector'");
    }
    
}
