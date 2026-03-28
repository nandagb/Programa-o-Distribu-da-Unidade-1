package ufrn.imd.br.gateway.strategy;

public interface GatewayStrategy {
    public void server();
    public void listenHeartBeat();
    public void failureDetector();
    
}
