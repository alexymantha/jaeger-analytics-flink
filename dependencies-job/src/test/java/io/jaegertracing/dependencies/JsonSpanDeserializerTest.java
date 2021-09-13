package io.jaegertracing.dependencies;

import com.fasterxml.jackson.annotation.JsonTypeId;
import com.uber.jaeger.Process;
import com.uber.jaeger.SpanRef;
import com.uber.jaeger.Tag;
import io.jaegertracing.analytics.es.model.JsonProcess;
import io.jaegertracing.analytics.es.model.JsonReference;
import io.jaegertracing.analytics.es.model.JsonSpan;
import io.jaegertracing.analytics.es.model.JsonTag;
import io.jaegertracing.analytics.kafka.deserializer.JsonSpanDeserializer;
import io.jaegertracing.dependencies.model.Span;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;

import static io.jaegertracing.dependencies.Base64Util.toBase64;

public class JsonSpanDeserializerTest {
    // Base64 value of 1235
    private final String traceId = Base64Util.toBase64(1235, true);
    // Base64 value of 4444
    private final String spanId = Base64Util.toBase64(4444);
    private final String serviceName = "boop";

    private final SpanDeserializer spanDeserializer = new SpanDeserializer();

    private JsonSpan getSpan() {
        JsonSpan jSpan = new JsonSpan();
        jSpan.setTraceId(traceId);
        jSpan.setSpanId(spanId);

        JsonProcess process = new JsonProcess();
        process.setServiceName(serviceName);
        jSpan.setProcess(process);

        return jSpan;
    }

    @Test
    public void mapRequiredFields() {
        JsonSpan jSpan = getSpan();

        Span span = spanDeserializer.map(jSpan);
        Assertions.assertThat(span.getTraceId()).isEqualTo(1235);
        Assertions.assertThat(span.getSpanId()).isEqualTo(4444);
        Assertions.assertThat(span.getServiceName()).isEqualTo(serviceName);
    }

    @Test
    public void mapServerTag() {
        JsonSpan jSpan = getSpan();

        JsonTag tag = new JsonTag();
        tag.setValue("server");
        tag.setKey("span.kind");
        jSpan.setTags(Collections.singletonList(tag));

        Span span = spanDeserializer.map(jSpan);
        Assertions.assertThat(span.isServer()).isTrue();
        Assertions.assertThat(span.isClient()).isFalse();
    }

    @Test
    public void mapClientTag() {
        JsonSpan jSpan = getSpan();

        JsonTag tag = new JsonTag();
        tag.setValue("client");
        tag.setKey("span.kind");
        jSpan.setTags(Collections.singletonList(tag));

        Span span = spanDeserializer.map(jSpan);
        Assertions.assertThat(span.isServer()).isFalse();
        Assertions.assertThat(span.isClient()).isTrue();
    }

    @Test
    public void mapParentSpan() {
        long parentSpanId = 123;

        JsonSpan jSpan = getSpan();
        jSpan.setReferences(Collections.singletonList(new JsonReference(traceId, toBase64(parentSpanId), null)));

        Assertions.assertThat(spanDeserializer.map(jSpan).getParentSpanId()).isEqualTo(parentSpanId);
    }

    @Test
    public void mapParentSpanFromReference() {
        String parentSpanId = Base64Util.toBase64(123);

        JsonReference spanRef = new JsonReference();
        spanRef.setSpanId(parentSpanId);

        JsonSpan jSpan = getSpan();
        jSpan.setReferences(Collections.singletonList(spanRef));

        Assertions.assertThat(spanDeserializer.map(jSpan).getParentSpanId()).isEqualTo(123);
    }


    @Test
    public void getProducedType() {
        SpanDeserializer spanDeserializer = new SpanDeserializer();
        Assertions.assertThat(spanDeserializer.getProducedType()).isEqualTo(TypeInformation.of(Span.class));
    }
}