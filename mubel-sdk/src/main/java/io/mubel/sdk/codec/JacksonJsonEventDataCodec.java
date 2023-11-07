package io.mubel.sdk.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JacksonJsonEventDataCodec implements EventDataCodec {

    private final ObjectMapper jsonMapper;

    public JacksonJsonEventDataCodec(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public JacksonJsonEventDataCodec() {
        this(new ObjectMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        );
    }

    @Override
    public byte[] encode(Object data) {
        try {
            return jsonMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T decode(byte[] bytes, Class<T> klass) {
        try {
            return jsonMapper.readValue(bytes, klass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
