package io.jaegertracing.analytics.kafka.deserializer;

import io.jaegertracing.analytics.es.model.JsonSpan;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.serialization.AbstractDeserializationSchema;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

@Slf4j
public class JsonSpanDeserializer extends AbstractDeserializationSchema<JsonSpan> {
    @Override
    public JsonSpan deserialize(byte[] message) throws IOException {
        try {
            // Instantiate new object mapper to avoid serialization issues
            return new ObjectMapper().readValue(message, JsonSpan.class);
        } catch (Exception e) {
            log.warn("Unable to deserialize message.", e);
            return null;
        }
    }
}
