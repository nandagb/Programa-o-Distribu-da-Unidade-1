package ufrn.imd.br.server.strategy;

import ufrn.imd.br.service.Service;

public interface ServerStrategy {
    int heartBeatInterval = 1000;
    // port for hearbeat
	int heartBeatPort = 9000;

    // port for client requests or server responses
    int gatewayPort = 9001;
    //declare method here
    public void interfaceMethod(Service service);
}