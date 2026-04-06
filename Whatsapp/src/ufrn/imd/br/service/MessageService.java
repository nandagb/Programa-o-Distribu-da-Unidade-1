package ufrn.imd.br.service;

import ufrn.imd.br.model.Message;

import java.util.ArrayList;
import java.util.Map;

public class MessageService implements Service {

   public MessageService(){

   }

   @Override
   public String getType() {
      return "messages";
   }

   @Override
   public ServiceResponse processMessage(String method, String message, Map<String, String> queryParams) {
      String[] tokens = message.split(";");
      System.out.println("PATH: " + method);
      System.out.println("MESSAGE: " + message);

      switch(method) {
         case "POST":
            return sendMessage(tokens);
         // case "GET":
         //    break;
         // case "PUT":
         //    break;
         // case "DELETE":
         //    break;
         default:
            System.out.println("\"Method No Allowed\"");
      }
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'processMessage'");
   }

   public ServiceResponse sendMessage(String[] params) {
      ServiceResponse response = new ServiceResponse();

      int sender = Integer.parseInt(params[0]);
      int receiver = Integer.parseInt(params[1]);
      String message = params[2];

      // salva no banco
      response.setStatusCode(200);
      response.setBody("Messagem enviada com sucesso!");

      return response;
   }

}
