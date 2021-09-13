package io.jaegertracing.dependencies;

import com.uber.jaeger.Process;
import com.uber.jaeger.Tag;
import io.jaegertracing.analytics.es.model.JsonProcess;
import io.jaegertracing.analytics.es.model.JsonReference;
import io.jaegertracing.analytics.es.model.JsonSpan;
import io.jaegertracing.analytics.es.model.JsonTag;
import io.jaegertracing.analytics.kafka.deserializer.JsonSpanDeserializer;
import io.jaegertracing.dependencies.cassandra.Dependencies;
import io.jaegertracing.dependencies.cassandra.Dependency;
import io.jaegertracing.dependencies.es.DependencyLink;
import io.jaegertracing.dependencies.es.TimeDependencies;
import io.jaegertracing.dependencies.model.DependencyItem;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;

import static io.jaegertracing.dependencies.Base64Util.toBase64;

public class DependenciesProcessorTest {

    private static final String KIND_SERVER = "server";
    private static final String KIND_CLIENT = "client";

    private final String[] services = {"Thorin", "Balin", "Bifur", "Bofur", "Bombur", "Dwalin", "Fili"};

    private StreamExecutionEnvironment env;

    @Before
    public void setup() {
        env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
    }

    private JsonSpan createSpan(String traceId, String spanId, long parentSpanId, int serviceIdx, String spanKind) {
        JsonSpan span = new JsonSpan();
        span.setTraceId(traceId);
        span.setSpanId(spanId);

        span.setOperationName("operation");

        span.setReferences(Collections.singletonList(new JsonReference(traceId, toBase64(parentSpanId), null)));

        JsonTag tag = new JsonTag();
        tag.setKey("span.kind");
        tag.setValue(spanKind);
        span.setTags(Collections.singletonList(tag));

        JsonProcess process = new JsonProcess();
        process.setServiceName(services[serviceIdx]);
        span.setProcess(process);

        return span;
    }

    @Test
    public void testEndToEnd() throws Exception {

        JsonSpan[] spans = {
                createSpan(toBase64(12, true), toBase64(5), 0, 1, KIND_SERVER),
                createSpan(toBase64(12, true), toBase64(6), 5, 2, KIND_CLIENT),
                createSpan(toBase64(12, true), toBase64(7), 5, 2, KIND_CLIENT),
                createSpan(toBase64(12, true), toBase64(8), 5, 3, KIND_CLIENT),

                // Local span - should be ignored
                createSpan(toBase64(12, true), toBase64(8), 5, 3, "local"),

                // Zipkin style spans with repeated spanids
                createSpan(toBase64(50, true), toBase64(5), 0, 4, KIND_CLIENT),
                createSpan(toBase64(50, true), toBase64(5), 0, 5, KIND_SERVER),
        };

        DataStreamSource<JsonSpan> source = env.fromElements(spans);

        DependenciesProcessor.setupJob(source.map(new SpanDeserializer()), new CallAggregator(), new DependenciesSink());

        env.execute();

        Assertions.assertThat(DependenciesSink.values.get(0).getDependencies())
                .containsExactlyInAnyOrder(
                        new DependencyLink(services[1], services[2], 2L),
                        new DependencyLink(services[1], services[3], 1L),
                        new DependencyLink(services[4], services[5], 1L));

    }



    private static class DependenciesSink implements SinkFunction<TimeDependencies> {
        // This is static because flink serializes sinks
        public static final List<TimeDependencies> values = new ArrayList<>();

        @Override
        public synchronized void invoke(TimeDependencies value, Context context) {
            values.add(value);
        }
    }

    private static class CallAggregator implements AggregateFunction<DependencyItem, Map<DependencyItem, Long>, TimeDependencies> {

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
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'");
            TimeDependencies result = new TimeDependencies(
                    dateFormat.format(new Date()), // To date
                    new ArrayList<>()
            );

            accumulator.forEach((k, v) -> result.getDependencies()
                    .add(new DependencyLink(k.getParent(), k.getChild(), v)));

            return result;
        }

        @Override
        public Map<DependencyItem, Long> merge(Map<DependencyItem, Long> a, Map<DependencyItem, Long> b) {
            a.forEach((k, v) -> b.merge(k, v, Long::sum));
            return b;
        }
    }
}


