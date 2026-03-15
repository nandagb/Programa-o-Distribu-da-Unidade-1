package ufrn.imd.br.TCP;

import ufrn.imd.br.Strategy;
import ufrn.imd.br.service.Service;

public class TCPServer implements Strategy {
    public TCPServer(){

    }

    public void interfaceMethod(Service service){
        System.out.println("This is the TCP Server Strategy!");

        service.processMessage("random message tcp");
    }
}
