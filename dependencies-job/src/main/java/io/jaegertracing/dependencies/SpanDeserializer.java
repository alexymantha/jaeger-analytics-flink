package io.jaegertracing.dependencies;

import io.jaegertracing.analytics.es.model.JsonProcess;
import io.jaegertracing.analytics.es.model.JsonReference;
import io.jaegertracing.analytics.es.model.JsonSpan;
import io.jaegertracing.dependencies.model.Span;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;

import java.util.List;
import java.util.Map;

class SpanDeserializer extends RichMapFunction<JsonSpan, Span> implements ResultTypeQueryable<Span> {

    @Override
    public Span map(JsonSpan jSpan) {
        Span span = new Span();
        span.setTraceId(Long.parseLong(jSpan.getTraceId()));
        span.setSpanId(Long.parseLong(jSpan.getSpanId()));

        List<JsonReference> refs = jSpan.getReferences();
        if (refs != null) {
            for (JsonReference ref : refs) {
                if (JsonReference.CHILD_OF_REF_TYPE.equals(ref.getRefType())) {
                    span.setParentSpanId(Long.parseLong(ref.getSpanId()));
                }
            }
        }

        JsonProcess process = jSpan.getProcess();
        span.setServiceName(process.getServiceName());

        if (jSpan.getTag() != null) {
            for (Map.Entry<String, String> tagEntry : jSpan.getTag().entrySet()) {
                String str = tagEntry.getValue();
                if ("client".equals(str) && "span.kind".equals(tagEntry.getKey())) {
                    span.setClient(true);
                }
                if ("server".equals(str) && "span.kind".equals(tagEntry.getKey())) {
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
