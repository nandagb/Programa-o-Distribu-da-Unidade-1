package ufrn.imd.br.service;

public class ServiceResponse {
    private int statusCode;
    private String body;

    public void setStatusCode(int code) {
        this.statusCode = code;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBody() {
        return this.body;
    }
}
