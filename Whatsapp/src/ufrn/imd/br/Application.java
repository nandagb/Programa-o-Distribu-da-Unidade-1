package ufrn.imd.br;

import ufrn.imd.br.Context;
import ufrn.imd.br.service.*;
import ufrn.imd.br.UDP.server.UDPServer;
import ufrn.imd.br.TCP.TCPServer;

public class Application {
    public static void main(String args[]){
        Context context = new Context();

        if (args.length == 0) {
            System.out.println("Erro! Nenhum argumento fornecido");
            return;
        }

        String protocol = args[0];

        switch(protocol) {
            case "udp":
                System.out.println("opção udp selecionada");
                context.setStrategy(new UDPServer("9004"));
                break;
            case "tcp":
                System.out.println("opção tcp selecionada");
                context.setStrategy(new TCPServer());
                break;
            // case "http":
            //     break;
            // case "grpc":
            //     break;
            default:
                System.out.println("Opção inválida!");                
        }
        System.out.println("DEPOIS DO SWITCH DO SERVER");

        String service = args[1];
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
