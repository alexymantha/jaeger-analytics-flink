package io.jaegertracing.analytics.es.model;

import lombok.Data;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonReference implements Serializable {
    public static final String CHILD_OF_REF_TYPE = "CHILD_OF";
    public static final String FOLLOWS_FROM_REF_TYPE = "FOLLOWS_FROM";

    private String traceId;
    private String spanId;
    private String refType;

    public JsonReference() {

    }

    public JsonReference(String traceId, String spanId, String refType) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.refType = refType;
    }
}
