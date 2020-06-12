package io.jaegertracing.dependencies.es;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class TimeDependencies implements Serializable {
    private long timestamp;
    private List<DependencyLink> dependencies;
}
