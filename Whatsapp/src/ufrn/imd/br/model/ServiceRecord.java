package ufrn.imd.br.model;

import java.net.InetAddress;

public class ServiceRecord {
   private InetAddress address;
   private int port;
   private volatile Boolean status;
   private volatile long lastHeartbeat;
   private String type;

   public ServiceRecord(InetAddress address, int port) {
      this.address = address;
      this.port = port;
      this.status = true;
      this.lastHeartbeat = System.currentTimeMillis();
   }

   public void refreshHeartBeat() {
      this.status = true;
      this.lastHeartbeat = System.currentTimeMillis();
   }

   public long getLastHeartbeat() {
      return this.lastHeartbeat;
   }

   public void setStatus(Boolean status) {
      this.status = status;
   }

   public Boolean getStatus() {
      return this.status;
   }

   public int getPort() {
      return this.port;
   }
}
