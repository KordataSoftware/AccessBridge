package com.kordata.odbcbridge;

import java.io.IOException;
import java.util.Arrays;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

public class BridgeAPI {
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;

    private HttpUrl remoteUrl;

    private String host;
    private final ObjectMapper mapper = new ObjectMapper();

    public String getHost() {
        return host;
    }

    private int port;

    public int getPort() {
        return port;
    }

    private String username;

    public String getUsername() {
        return username;
    }

    private String password;

    public String getPassword() {
        return password;
    }

    private boolean useSsl;

    public boolean getUseSsl() {
        return useSsl;
    }

    private String url;

    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;
        remoteUrl = null;
        host = null;
        port = 0;
        database = null;
        username = null;
        password = null;
        useSsl = true;

        if (url == null) {
            return;
        }

        String[] components = url.split(";");
        Arrays.asList(components).stream().skip(1).map(ConnectionProperty::Create)
                .forEach(this::applyConnectionProperty);

        String[] baseComponents = components[0].split("//");
        if (baseComponents.length > 1) {
            String scheme = useSsl == true ? "https" : "http";
            remoteUrl = HttpUrl.parse(scheme + "://" + baseComponents[1]);

            host = remoteUrl.host();
            port = remoteUrl.port();
        }
    }

    private String database;

    public String getDatabase() {
        return database;
    }

    public String getHealthCheckEndpoint() {
        return "/v1/" + database + "/health_check";
    }

    public String getQueryEndpoint() {
        return "/v1/" + database + "/query";
    }

    public String getMutateEndpoint() {
        return "/v1/" + database + "/mutate";
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    private void applyConnectionProperty(ConnectionProperty property) {
        switch (property.getKey()) {
            case "databaseName":
                database = property.getValue();
                break;
            case "user":
                username = property.getValue();
                break;
            case "password":
                password = property.getValue();
                break;
            case "useSsl":
                useSsl = property.getValue() == "true";
                break;
            default:
                break;
        }
    }

    public BridgeAPI() {
        httpClient = new OkHttpClient();
    }

    public boolean remoteIsReachable() {
        Request request = createGet(getHealthCheckEndpoint());

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            return false;
        }
    }

    public ObjectNode query(String sql, ArrayNode parameters) throws IOException {
        ObjectNode bodyNode = mapper.createObjectNode();
        bodyNode.put("command", sql);

        if (parameters != null) {
            bodyNode.set("parameters", parameters);
        }

        Request request = createPost(getQueryEndpoint(), bodyNode);
        Response response = httpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException(response.message());
        }

        try {
            return (ObjectNode) mapper.readTree(response.body().byteStream());
        } catch (JsonParseException e) {
            // Shouldn't happen because we manually constructed the object.
            throw new IllegalStateException(e.getMessage());
        }
    }

    public int mutate(String sql, ArrayNode parameters) throws IOException {
        ObjectNode bodyNode = mapper.createObjectNode();
        bodyNode.put("command", sql);

        if (parameters != null) {
            bodyNode.set("parameters", parameters);
        }

        Request request = createPost(getQueryEndpoint(), bodyNode);
        Response response = httpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException(response.message());
        }

        try {
            ObjectNode responseNode = (ObjectNode) mapper.readTree(response.body().byteStream());
            return responseNode.get("rowsAffected").asInt();
        } catch (JsonParseException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private Request.Builder createBaseRequest(String endpoint) {
        return new Request.Builder().url(remoteUrl.resolve(endpoint).toString());
    }

    private Request createGet(String endpoint) {
        return createBaseRequest(endpoint).build();
    }

    private Request createPost(String endpoint, JsonNode jsonNode) {
        try {
            String bodyJson = mapper.writeValueAsString(jsonNode);
            RequestBody requestBody = RequestBody.create(JSON, bodyJson);
            return createBaseRequest(endpoint).post(requestBody).build();
        } catch (JsonProcessingException e) {
            // This shouldn't happen because we manually built the objects.
            throw new IllegalStateException(e.getMessage());
        }
    }
}
