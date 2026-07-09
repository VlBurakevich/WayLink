package com.solution.waylink.signaling.json;

public interface JsonMapper {
    String toJson(Object src);
    <T> T fromJson(String json, Class<T> classOfT);
}
