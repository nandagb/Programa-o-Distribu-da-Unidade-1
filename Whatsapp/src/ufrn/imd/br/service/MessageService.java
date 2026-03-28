package ufrn.imd.br.service;

public class MessageService implements Service {

   public MessageService(){

   }

   public String getType() {
      return "messages";
   }

   public void processMessage(String message){
      System.out.println("processing message in Message Service: " + message);
   }

}
