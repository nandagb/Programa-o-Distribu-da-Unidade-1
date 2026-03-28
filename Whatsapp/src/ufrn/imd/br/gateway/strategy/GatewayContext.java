package ufrn.imd.br.gateway.strategy;

import ufrn.imd.br.server.strategy.ServerStrategy;

public class GatewayContext {
    private GatewayStrategy strategy;

    public void setStrategy(GatewayStrategy strategy){
        this.strategy = strategy;
    }

    public void server() {
        strategy.server();
    }

    public void listenHeartBeat() {
        strategy.listenHeartBeat();
    }

    public void failureDetector() {
        strategy.failureDetector();
    }
}
