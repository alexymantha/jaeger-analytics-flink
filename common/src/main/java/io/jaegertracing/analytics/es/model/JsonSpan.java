package io.jaegertracing.analytics.es.model;

import lombok.Data;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonSpan implements Serializable {
    @JsonProperty("traceID")
    private String traceId;
    @JsonProperty("spanID")
    private String spanId;
    private Integer flags;
    private String operationName;
    private List<JsonReference> references;
    private Long startTime;
    private Long startTimeMillis;
    private Long duration;
    private String type;
    private JsonProcess process;
    @JsonProperty("JaegerTag")
    private Map<String, String> tag;
}