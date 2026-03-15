package ufrn.imd.br;

import ufrn.imd.br.service.Service;

public class Context {
    private Strategy strategy;

    public void setStrategy(Strategy strategy){
        this.strategy = strategy;
    }

    public void executeStrategy(Service service){
        strategy.interfaceMethod(service);
    }
}
