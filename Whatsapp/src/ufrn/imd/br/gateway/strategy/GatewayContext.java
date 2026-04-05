package ufrn.imd.br.gateway.strategy;

import ufrn.imd.br.server.strategy.ServerStrategy;

public class GatewayContext {
    private GatewayStrategy strategy;

    public void setStrategy(GatewayStrategy strategy){
        this.strategy = strategy;
    }

    public void start() {
        new Thread(() -> strategy.server()).start();
        new Thread(() -> strategy.listenHeartBeat()).start();
        new Thread(() -> strategy.failureDetector()).start();
    }
}
