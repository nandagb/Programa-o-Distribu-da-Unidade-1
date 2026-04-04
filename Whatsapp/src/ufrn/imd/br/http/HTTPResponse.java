package ufrn.imd.br.http;

import java.util.StringTokenizer;

public class HTTPResponse {
    private String status;
    private int code;
    private String contentType;
    private int contentLength;
    private String protocol;

    private String headers;
    private String body;

    public HTTPResponse() {
        this.contentLength = 0;
    }

    public HTTPResponse(String firstHeader) {
        this.contentLength = 0;

        StringTokenizer tokenizer = new StringTokenizer(firstHeader);
        this.protocol = tokenizer.nextToken();
        this.code = Integer.parseInt(tokenizer.nextToken());
        this.status = tokenizer.nextToken();
    }

    public HTTPResponse(String protocol, int code, String status) {
        this.contentLength = 0;
        this.protocol = protocol;
        this.code = code;
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setContentLength(String line) {
        this.contentLength = Integer.parseInt(line.split(":")[1].trim());
    }

    public void setContentLength(int length) {
        this.contentLength = length;
    }

    public int getContentLength() {
        return this.contentLength;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getHeaders() {
        return this.headers;
    }

    public void setBody(char[] body) {
        this.body = new String(body);
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBody() {
        return this.body;
    }

}
