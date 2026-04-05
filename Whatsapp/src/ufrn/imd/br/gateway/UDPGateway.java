package ufrn.imd.br.gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.concurrent.*;

import ufrn.imd.br.gateway.strategy.GatewayStrategy;
import ufrn.imd.br.http.HTTPRequest;
import ufrn.imd.br.http.HTTPResponse;
import ufrn.imd.br.model.ServiceRecord;

public class UDPGateway implements GatewayStrategy {
    private ConcurrentHashMap<String, ServiceRecord> messageServicesTable;
    private ConcurrentHashMap<String, ServiceRecord> userServicesTable;
    AtomicInteger messagesIndex = new AtomicInteger(0);
    AtomicInteger usersIndex = new AtomicInteger(0);
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

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

    private HTTPRequest getHTTPRequest(BufferedReader clientRequest) {
        StringBuilder headersBuilder = new StringBuilder();
        String firstHeader;

        try {
            firstHeader = clientRequest.readLine();
            HTTPRequest request = new HTTPRequest(firstHeader);

            String line;
            while ((line = clientRequest.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    request.setContentLength(line);
                }

                headersBuilder.append(line).append("\r\n");
            }

            request.setHeaders(headersBuilder.toString());

            if (request.getContentLength() > 0) {
                int totalRead = 0;
                char[] body = new char[request.getContentLength()];

                while (totalRead < request.getContentLength()) {
                    int read = clientRequest.read(body, totalRead, request.getContentLength() - totalRead);
                    if (read == -1) break;
                    totalRead += read;
                }
                request.setBody(body);
            }

            return request;

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private HTTPResponse getHTTPResponse(BufferedReader serverResponse) {
        StringBuilder headersBuilder = new StringBuilder();
        String firstHeader;

        try {
            firstHeader = serverResponse.readLine();
            HTTPResponse response = new HTTPResponse(firstHeader);

            String line;
            while ((line = serverResponse.readLine()) != null && !line.isEmpty()) {
                System.out.println("READING LINE OF SERVER RESPONSE: " + line);
                if (line.startsWith("Content-Length:")) {
                    response.setContentLength(line);
                }

                headersBuilder.append(line).append("\r\n");
            }

            response.setHeaders(headersBuilder.toString());
            if (response.getContentLength() > 0) {
                char[] body = new char[response.getContentLength()];
                serverResponse.read(body, 0, response.getContentLength());
                response.setBody(body);
            }

            return response;

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private DatagramPacket processRequest(DatagramPacket packet) {
        // converte mensagem do cliente em bytes para texto
                                            // dados,              posição inicial, quantidade de bytes
        String message = new String(packet.getData(), 0,       packet.getLength());
        System.out.println("MESSAGE RECEIVED IN GATEWAY: " + message);
        int clientPort = packet.getPort();
        InetAddress clientAddress = packet.getAddress();
        // decodes message
        // System.out.println("UDP Gateway received this message from the client of port: " + packet.getPort() + ", " + message);

        try {
            packet.setAddress(InetAddress.getByName("localhost"));

            BufferedReader messageReader = new BufferedReader(new StringReader(message));

            if (message.startsWith("HTTP/")) {
                System.out.println("Resposta do Servidor" + message);

                HTTPResponse response = getHTTPResponse(messageReader);
                String newServerResponse = response.toString();
                String ip = response.getHeader("X-Client-IP");

                if (ip.startsWith("/")) {
                    ip = ip.substring(1);
                }

                InetAddress ogClientAddress = InetAddress.getByName(ip);
                int ogclientPort = Integer.parseInt(response.getHeader("X-Client-Port"));

                return new DatagramPacket( newServerResponse.getBytes(), newServerResponse.getBytes().length, ogClientAddress, ogclientPort );
            } else {
                System.out.println("Requisição do Cliente");

                HTTPRequest request = getHTTPRequest(messageReader);

                if (request == null) {
                    System.out.println("Não foi possível processar a requisição!");
                    //retornar erro sla
                    return null;
                }
                else {
                    request.setHeader("X-Client-IP: " + clientAddress);
                    request.setHeader("X-Client-Port: " + clientPort);

                    System.out.println("PATH FROM UDP REQUEST: " + request.getPath());
                    String path = request.getPath();
                    switch (path) {
                        case "/messages":
                            // if (messageServicesTable.isEmpty()) {
                            //     System.out.println("No available server!");
                            //     return null;
                            // }

                            String newClientMsg = request.toString();

                            // ServiceRecord nextService = getNextService(messageServicesTable, messagesIndex);

                            // System.out.println("Sending request to messages server with port: " + nextService.getPort());

                            // return new DatagramPacket( newClientMsg.getBytes(), newClientMsg.getBytes().length, packet.getAddress(), nextService.getPort() );

                            // Enviando apenas para porta 9004 por enquanto
                            return new DatagramPacket( newClientMsg.getBytes(), newClientMsg.getBytes().length, packet.getAddress(), 9004 );
                    }
                }

                // TEMPORÁRIO; REMOVER DEPOIS
                return null;
            }
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void server() {
        DatagramSocket socket;

		try {
            socket = new DatagramSocket(serverPort);
			while (true) {
                // receives messages from client
				byte[] clientMessage = new byte[1024];
				DatagramPacket clientPacket = new DatagramPacket(clientMessage, clientMessage.length);
				socket.receive(clientPacket);

                DatagramPacket packetCopy = new DatagramPacket(
                    clientPacket.getData().clone(),
                    clientPacket.getLength(),
                    clientPacket.getAddress(),
                    clientPacket.getPort()
                );

				// === chamar threads ===
                executor.execute(() -> {
                    DatagramPacket processedPacket = processRequest(packetCopy);

                    if (processedPacket == null) {
                        System.out.println("Não foi possível processar o pacote!");
                    }
                    else {
                        try {
                            socket.send(processedPacket);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                });
                // ======================
			}
		} catch (IOException e) {
			e.printStackTrace();
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
                BufferedReader messageReader = new BufferedReader(new StringReader(message));
                HTTPRequest request = getHTTPRequest(messageReader);

                updateService(request.getBody());
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

            for (HashMap.Entry<String, ServiceRecord> entry : messageServicesTable.entrySet()) {
                String key = entry.getKey();
                ServiceRecord service = entry.getValue();

                if (System.currentTimeMillis() - service.getLastHeartbeat() > heartBeatTimeout) {
                    service.setStatus(false);
                }
            }

            for (HashMap.Entry<String, ServiceRecord> entry : userServicesTable.entrySet()) {
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
    
}
