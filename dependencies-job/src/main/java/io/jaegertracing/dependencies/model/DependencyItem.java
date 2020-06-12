package io.jaegertracing.dependencies.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = "callCount")
public class DependencyItem {
    String parent;
    String child;
    Long callCount;
}
