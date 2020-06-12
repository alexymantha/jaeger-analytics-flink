package io.jaegertracing.dependencies.es;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class DependencyLink implements Serializable {
    private String parent;
    private String child;
    private long callCount;
}
