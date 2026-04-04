package ufrn.imd.br.http;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class HTTPRequest {
    private String method;
    private String path;
    private String queryString;
    private Map<String, String> queryParams = new HashMap<>();

    private int contentLength;
    private String headers;
    private String body;

    public HTTPRequest() {
        this.contentLength = 0;
    }

    public HTTPRequest(String firstHeader) {
        this.contentLength = 0;
        StringTokenizer tokenizer = new StringTokenizer(firstHeader);
        this.method = tokenizer.nextToken();

        String pathQuery = tokenizer.nextToken();

        if (pathQuery.contains("?")) {
            String[] parts = pathQuery.split("\\?", 2);

            this.path = parts[0];
            this.queryString = parts[1];
            parseQueryParams(this.queryString);
        } else {
            this.path = pathQuery;
        }
    }

    private void parseQueryParams(String query) {
        String[] params = query.split("&");

        for (String param : params) {
            String[] keyValue = param.split("=", 2);

            String key = keyValue[0];
            String value = keyValue.length > 1 ? keyValue[1] : "";

            this.queryParams.put(key, value);
        }
    }

    public String getMethod() {
        return this.method;
    }

    public String getPath() {
        return this.path;
    }

    public String getQueryString() {
        return this.queryString;
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
