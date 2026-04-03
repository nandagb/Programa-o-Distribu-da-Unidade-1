package ufrn.imd.br.server;

import ufrn.imd.br.server.strategy.ServerContext;
import ufrn.imd.br.service.*;

public class Application {
    public static void main(String args[]){
        ServerContext context = new ServerContext();

        if (args.length == 0) {
            System.out.println("Erro! Nenhum argumento fornecido");
            return;
        }

        String protocol = args[0];
        String service = args[1];
        int instance = Integer.parseInt(args[2]);
        int port;

        if (service.equals("messages")) {
            if (instance == 1) {
                port = 9004;
            }
            else {
                port = 9004;
            }
        }
        else {
            if (instance == 1) {
                port = 9006;
            }
            else {
                port = 9007;
            }

        }

        switch(protocol) {
            case "udp":
                System.out.println("opção udp selecionada");
                context.setStrategy(new UDPServer(port));
                break;
            case "tcp":
                System.out.println("opção tcp selecionada");
                context.setStrategy(new TCPServer(port));
                break;
            // case "http":
            //     break;
            // case "grpc":
            //     break;
            default:
                System.out.println("Opção inválida!");                
        }
        System.out.println("DEPOIS DO SWITCH DO SERVER");

        System.out.println("SERVICE OPTION: " + service);
        // Service service;

        switch(service) {
            // case "users":
            //     System.out.println("opção users selecionada");
            //     context.setStrategy(new UDPServer("8080"));
            //     break;
            case "messages":
                System.out.println("opção messages selecionada");
                // service = new MessageService();
                context.executeStrategy(new MessageService());
                break;
            default:
                System.out.println("Opção inválida!");                
        }
    }
}
