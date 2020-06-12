package io.jaegertracing.dependencies;

import io.jaegertracing.analytics.JaegerJob;
import io.jaegertracing.analytics.es.model.JsonSpan;
import io.jaegertracing.dependencies.es.ElasticsearchCallCountAggregator;
import io.jaegertracing.dependencies.es.TimeDependencies;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

@Slf4j
public class DependenciesJob implements JaegerJob<TimeDependencies> {
    /**
     * These constants are used to provide user friendly names for Flink operators. Flink also uses them in
     * metric names.
     */
    private static final String DESERIALIZE_SPAN = "DeserializeSpan";

    public static void main(String[] args) throws Exception {
        DependenciesJob job = new DependenciesJob();
        ParameterTool parameterTool = ParameterTool.fromArgs(args);

        job.executeJob("Dependencies Job", parameterTool, SinkStorage.ELASTICSEARCH, TypeInformation.of(TimeDependencies.class));
    }

    @Override
    public void setupJob(ParameterTool parameterTool, DataStream<JsonSpan> spans, SinkFunction<TimeDependencies> sinkFunction) {
        SingleOutputStreamOperator<io.jaegertracing.dependencies.model.Span> modelSpans = spans.map(new SpanDeserializer()).name(DESERIALIZE_SPAN);
        DependenciesProcessor.setupJob(modelSpans, new ElasticsearchCallCountAggregator(), sinkFunction);
    }
}
