package ufrn.imd.br.gateway;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import ufrn.imd.br.gateway.strategy.GatewayContext;
import ufrn.imd.br.model.ServiceRecord;

public class APIGateway {
    private ConcurrentHashMap<String, ServiceRecord> messageServicesTable;
    private ConcurrentHashMap<String, ServiceRecord> userServicesTable;
    AtomicInteger messagesIndex = new AtomicInteger(0);
    AtomicInteger usersIndex = new AtomicInteger(0);
    private int heartBeatTimeout = 3000;
    private int failureDetectorInterval = 1000;
    private DatagramSocket heartBeatSocket;
    public int heartBeatGatewayPort = 9007;
    public int ServerGatewayPort = 9001;

    public APIGateway() throws Exception {
        heartBeatSocket = new DatagramSocket(heartBeatGatewayPort);
        messageServicesTable = new ConcurrentHashMap<>();
        userServicesTable = new ConcurrentHashMap<>();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Erro! Nenhum argumento fornecido");
            return;
        }

        String protocol = args[0];

        try {
            GatewayContext context = new GatewayContext();

            APIGateway gateway = new APIGateway();

            switch(protocol) {
            case "udp":
                System.out.println("opção udp selecionada");
                // chama o context sem passar serviço
                context.setStrategy(new UDPGateway());
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
            new Thread(() -> context.server()).start();
            new Thread(() -> context.listenHeartBeat()).start();
            new Thread(() -> context.failureDetector()).start();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
