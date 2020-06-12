package io.jaegertracing.analytics.es.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonTag implements Serializable {
}
