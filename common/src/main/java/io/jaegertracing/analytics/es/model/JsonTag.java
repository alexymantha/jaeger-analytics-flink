package io.jaegertracing.analytics.es.model;

import lombok.Data;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonTag implements Serializable {

    private String key;
    @JsonProperty("vStr")
    private String value;
}
