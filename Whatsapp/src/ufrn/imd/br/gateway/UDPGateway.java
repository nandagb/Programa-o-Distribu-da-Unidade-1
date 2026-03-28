package ufrn.imd.br.gateway;

import ufrn.imd.br.gateway.strategy.GatewayStrategy;
import ufrn.imd.br.service.Service;

public class UDPGateway implements GatewayStrategy {
    private int port;

    public UDPGateway(int port){
        this.port = port;
    }

    @Override
    public void server() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'server'");
    }

    @Override
    public void listenHeartBeat() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listenHeartBeat'");
    }

    @Override
    public void failureDetector() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'failureDetector'");
    }
    
}
