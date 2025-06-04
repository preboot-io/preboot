package io.preboot.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class JacksonJsonMapper implements JsonMapper {
    private final ObjectMapper objectMapper;

    public JacksonJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String toJson(final Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonParsingException(e);
        }
    }

    @Override
    public <T> T fromJson(final String json, final Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new JsonParsingException(e);
        }
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
