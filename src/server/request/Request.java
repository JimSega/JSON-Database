package server.request;

import com.google.gson.JsonElement;

public class Request {

    private final String type;
    private  JsonElement key = null;
    private  JsonElement value = null;





    public Request(String typeRequest, JsonElement key, JsonElement value) {
        this.type = typeRequest;
        this.key = key;
        this.value = value;
    }

    public Request(String typeRequest, JsonElement key) {
        this.type = typeRequest;
        this.key = key;
    }

    public Request(String typeRequest) {
        this.type = typeRequest;
    }
    public String getType () {
        return type;
    }
    public JsonElement getKey () {
        return key;
    }
    public JsonElement getValue () {
        return value;
    }
}