package ufrn.imd.br.server.strategy;

import ufrn.imd.br.service.Service;

public interface ServerStrategy {
    int heartBeatInterval = 1000;
    // port for hearbeat
	int gatewayPort = 9000;
    //declare method here
    public void interfaceMethod(Service service);
}