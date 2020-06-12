package io.jaegertracing.analytics.es.model;

import lombok.Data;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonProcess implements Serializable {
    private String serviceName;
}
