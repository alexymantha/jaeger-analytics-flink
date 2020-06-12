package io.jaegertracing.dependencies.es;

import io.jaegertracing.dependencies.model.DependencyItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.AggregateFunction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ElasticsearchCallCountAggregator implements AggregateFunction<DependencyItem, Map<DependencyItem, Long>, TimeDependencies> {
    @Override
    public Map<DependencyItem, Long> createAccumulator() {
        return new HashMap<>();
    }

    @Override
    public Map<DependencyItem, Long> add(DependencyItem value, Map<DependencyItem, Long> accumulator) {
        accumulator.merge(value, value.getCallCount(), Long::sum);
        return accumulator;
    }

    @Override
    public TimeDependencies getResult(Map<DependencyItem, Long> accumulator) {
        TimeDependencies result = new TimeDependencies(
                Instant.now().toEpochMilli() * 1000 * 1000, // To nanoseconds
                new ArrayList<>()
        );

        accumulator.forEach((k, v) -> result.getDependencies()
                .add(new DependencyLink(k.getParent(), k.getChild(), v)));

        log.info("Getting result " + result.getDependencies().size());
        System.out.println("Getting result " + result.getDependencies().size());
        return result;
    }

    @Override
    public Map<DependencyItem, Long> merge(Map<DependencyItem, Long> a, Map<DependencyItem, Long> b) {
        a.forEach((k, v) -> b.merge(k, v, Long::sum));
        return b;
    }
}
