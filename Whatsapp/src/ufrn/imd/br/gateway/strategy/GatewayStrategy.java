package ufrn.imd.br.gateway.strategy;

public interface GatewayStrategy {
    int heartBeatTimeout = 3000;
    int failureDetectorInterval = 1000;
    int heartBeatPort = 9000;
    int serverPort = 9001;

    public void server();
    public void listenHeartBeat();
    public void failureDetector();
    
}
