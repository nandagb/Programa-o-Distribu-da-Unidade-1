package ufrn.imd.br.service;

import java.util.Map;

public interface Service {
    String getType();
    ServiceResponse processMessage(String path, String message, Map<String, String> queryParams);
}
