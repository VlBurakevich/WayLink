package com.solution.waylink.signaling.json;

import com.google.gson.Gson;

public class GsonMapper implements JsonMapper {
    private final Gson gson = new Gson();

    @Override
    public String toJson(Object src) {
        return gson.toJson(src);
    }

    @Override
    public <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }
}
