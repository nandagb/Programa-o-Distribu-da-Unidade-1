package ufrn.imd.br.server;

import ufrn.imd.br.server.strategy.ServerStrategy;
import ufrn.imd.br.service.Service;

public class TCPServer implements ServerStrategy {
    public TCPServer(){

    }

    public void interfaceMethod(Service service){
        System.out.println("This is the TCP Server Strategy!");

        service.processMessage("random message tcp");
    }
}
