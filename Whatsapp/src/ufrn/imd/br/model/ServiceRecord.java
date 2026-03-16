package ufrn.imd.br.model;

import java.net.InetAddress;

public class ServiceRecord {
   InetAddress address;
   int port;
   Boolean status;

   public ServiceRecord(InetAddress address, int port) {
      this.address = address;
      this.port = port;
      this.status = true;
   }

   public void setStatus(Boolean status) {
      this.status = status;
   }

   public Boolean getStatus() {
      return this.status;
   }
}
