package ufrn.imd.br;

import java.net.InetAddress;
import java.util.HashMap;

import ufrn.imd.br.model.ServiceRecord;

public class APIGateway {
    private HashMap<String, ServiceRecord> servicesTable;

    public APIGateway(){
        servicesTable = new HashMap<>();
    }

    public String getServiceKey(InetAddress address, int port){
        return address.toString() + port;
    }

    public void addServiceRecord(String key, ServiceRecord service) {
        servicesTable.put(key, service);
    }

    public void updateService(InetAddress address, int port){
        String key = getServiceKey(address, port);

        ServiceRecord service = servicesTable.get(key);

        if (service == null){
            addServiceRecord(key, new ServiceRecord(address, port));
            return;
        }

        service.setStatus(true);
    }

}
