package io.jaegertracing.dependencies;

import io.jaegertracing.analytics.es.model.JsonProcess;
import io.jaegertracing.analytics.es.model.JsonReference;
import io.jaegertracing.analytics.es.model.JsonSpan;
import io.jaegertracing.analytics.es.model.JsonTag;
import io.jaegertracing.dependencies.model.Span;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.shaded.guava18.com.google.common.primitives.Longs;
import org.apache.kafka.common.utils.ByteUtils;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

class SpanDeserializer extends RichMapFunction<JsonSpan, Span> implements ResultTypeQueryable<Span> {

    @Override
    public Span map(JsonSpan jSpan) {
        Span span = new Span();

        byte[] decoded = Base64.getDecoder().decode(jSpan.getTraceId());

        /*
         * If decoded array is longer than 8, it means the trace-id is 128 bits. We can truncate it to 64 bits for this application.
         */
        if(decoded.length > 8)
            decoded = Arrays.copyOfRange(decoded, 8, 16);
        span.setTraceId(Longs.fromByteArray(decoded));
        span.setSpanId(Longs.fromByteArray(Base64.getDecoder().decode(jSpan.getSpanId())));

        List<JsonReference> refs = jSpan.getReferences();
        if (refs != null) {
            for (JsonReference ref : refs) {
                span.setParentSpanId(Longs.fromByteArray(Base64.getDecoder().decode(ref.getSpanId())));
            }
        }

        JsonProcess process = jSpan.getProcess();
        span.setServiceName(process.getServiceName());

        if (jSpan.getTags() != null) {
            for (JsonTag tag : jSpan.getTags()) {
                String str = tag.getValue();
                if ("client".equals(str) && "span.kind".equals(tag.getKey())) {
                    span.setClient(true);
                }
                if ("server".equals(str) && "span.kind".equals(tag.getKey())) {
                    span.setServer(true);
                }
            }
        }

        return span;
    }

    @Override
    public TypeInformation<Span> getProducedType() {
        return TypeInformation.of(Span.class);
    }
}
