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

    private String refType;
    @JsonProperty("traceID")
    private String traceId;
    @JsonProperty("spanID")
    private String spanId;
}
