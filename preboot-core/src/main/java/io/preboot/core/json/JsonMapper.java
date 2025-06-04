package io.preboot.core.json;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface JsonMapper {
    String toJson(Object object);

    <T> T fromJson(String json, Class<T> clazz);

    ObjectMapper getObjectMapper();
}
