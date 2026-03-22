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

import ufrn.imd.br.TCP.TCPServer;
import ufrn.imd.br.UDP.server.UDPServer;
import ufrn.imd.br.model.Message;
import ufrn.imd.br.model.ServiceRecord;

public class APIGateway {
    private ConcurrentHashMap<String, ServiceRecord> servicesTable;
    private int heartBeatTimeout = 3000;
    private int failureDetectorInterval = 1000;
    private DatagramSocket heartBeatSocket;
    private int heartBeatGatewayPort = 9000;
    private int ServerGatewayPort = 9001;

    public APIGateway() throws Exception {
        System.out.println("right before connecting heartbeat on gateway");

        heartBeatSocket = new DatagramSocket(heartBeatGatewayPort);
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
                System.out.println("Gateway received this message from the heartbeat: " + message);

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

    public void UDPServer(){
        DatagramSocket socket;

		try {
            System.out.println("right before connecting udpserver on gateway");
            socket = new DatagramSocket(this.ServerGatewayPort);
			while (true) {
                //receives messages from client
				byte[] clientMessage = new byte[1024];
				DatagramPacket clientPacket = new DatagramPacket(clientMessage, clientMessage.length);
				socket.receive(clientPacket);

				//converte mensagem do cliente em bytes para texto
                                            // dados,              posição inicial, quantidade de bytes
                String message = new String(clientPacket.getData(), 0,       clientPacket.getLength());
                //decodes message
                System.out.println("Gateway received this message from the client: " + message);
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
                        //TODO: round robin pra decidir qual instancia do servidor vai ser usada
                        System.out.println("Sending request to messages server 1");
                        clientPacket.setPort(9004);
                        break;
                    default:
                        System.out.println("No service specified");
                }
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

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Erro! Nenhum argumento fornecido");
            return;
        }

        String protocol = args[0];

        try {

            APIGateway gateway = new APIGateway();

            switch(protocol) {
            case "udp":
                System.out.println("opção udp selecionada");
                new Thread(() -> gateway.UDPServer()).start();
                // context.setStrategy(new UDPServer(port));
                break;
            case "tcp":
                System.out.println("opção tcp selecionada");
                // context.setStrategy(new TCPServer());
                break;
            // case "http":
            //     break;
            // case "grpc":
            //     break;
            default:
                System.out.println("Opção inválida!");
        }

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
