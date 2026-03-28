package ufrn.imd.br.server.strategy;

import ufrn.imd.br.service.Service;

public class ServerContext {
    private ServerStrategy strategy;

    public void setStrategy(ServerStrategy strategy){
        this.strategy = strategy;
    }

    public void executeStrategy(Service service){
        strategy.interfaceMethod(service);
    }
}
