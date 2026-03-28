package ufrn.imd.br.gateway.strategy;

public interface GatewayStrategy {
    int heartBeatTimeout = 3000;
    int failureDetectorInterval = 1000;
    int heartBeatGatewayPort = 9000;
    int ServerGatewayPort = 9001;

    public void server();
    public void listenHeartBeat();
    public void failureDetector();
    
}
