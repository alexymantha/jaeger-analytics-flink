package io.jaegertracing.analytics.es.model;

import lombok.Data;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonSpan implements Serializable {
    private String traceId;
    private String spanId;
    private Integer flags;
    private String operationName;
    private List<JsonReference> references;
    private String startTime;
    private Long startTimeMillis;
    private String duration;
    private String type;
    private JsonProcess process;
    private List<JsonTag> tags;

}