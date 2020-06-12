package io.jaegertracing.dependencies;

import com.codahale.metrics.SlidingTimeWindowReservoir;
import io.jaegertracing.analytics.adjuster.Dedupable;
import io.jaegertracing.analytics.adjuster.SpanIdDeduplicator;
import io.jaegertracing.dependencies.model.DependencyItem;
import io.jaegertracing.dependencies.model.Span;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.dropwizard.metrics.DropwizardHistogramWrapper;
import org.apache.flink.metrics.Histogram;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.windowing.RichWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DependenciesProcessor {
    /**
     * These constants are used to provide user friendly names for Flink operators. Flink also uses them in
     * metric names.
     */
    private static final String SPANS_TO_TRACES = "SpansToTraces";
    private static final String DEDUPE_SPAN_IDS = "DedupeSpanIds";
    private static final String TRACE_TO_DEPENDENCIES = "TraceToDependencies";
    private static final String AGGREGATE_DEPENDENCIES = "AggregateDependencies";
    private static final String PREAGGREGATE_DEPENDENCIES = "PreAggregateDependencies";
    private static final String STORAGE_SINK = "StorageSink";
    private static final String COUNT_SPANS = "CountSpans";
    private static final String FILTER_LOCAL_SPANS = "FilterLocalSpans";

    private static final Time sessionWindow = Time.seconds(10);
    private static final Time aggregationWindow = Time.seconds(30);

    public static <S> void setupJob(SingleOutputStreamOperator<Span> spans,
                             AggregateFunction<DependencyItem, Map<DependencyItem, Long>, S> aggregateFunction,
                             SinkFunction<S> sink) {
        DataStream<Iterable<Span>> traces = aggregateSpansToTraces(spans);
        DataStream<DependencyItem> dependencies = computeDependencies(traces);
        aggregateAndWrite(dependencies, aggregateFunction, sink);
    }


    private static DataStream<Iterable<Span>> aggregateSpansToTraces(DataStream<Span> spans) {
        // Use session windows to aggregate spans into traces
        return spans.filter((FilterFunction<Span>) span -> span.isClient() || span.isServer()).name(FILTER_LOCAL_SPANS)
                .keyBy((KeySelector<Span, Long>) Span::getTraceId)
                .window(EventTimeSessionWindows.withGap(sessionWindow))
                .apply(new SpanToTraceWindowFunction()).name(SPANS_TO_TRACES)
                .map(new AdjusterFunction<>()).name(DEDUPE_SPAN_IDS)
                .map(new CountSpansAndLogLargeTraceIdFunction()).name(COUNT_SPANS);
    }

    private static DataStream<DependencyItem> computeDependencies(DataStream<Iterable<Span>> traces) {
        return traces.flatMap(new TraceToDependencies()).name(TRACE_TO_DEPENDENCIES)
                .keyBy(key -> key.getParent() + key.getChild())
                .timeWindow(aggregationWindow)
                .sum("callCount").name(PREAGGREGATE_DEPENDENCIES);
    }

    private static <S> void aggregateAndWrite(DataStream<DependencyItem> dependencies,
                                              AggregateFunction<DependencyItem, Map<DependencyItem, Long>, S> aggregateFunction,
                                              SinkFunction<S> sink) {
        dependencies.timeWindowAll(aggregationWindow)
                .aggregate(aggregateFunction).name(AGGREGATE_DEPENDENCIES)
                .addSink(sink).name(STORAGE_SINK).setParallelism(1);
    }

    private static class SpanToTraceWindowFunction extends RichWindowFunction<Span, Iterable<Span>, Long, TimeWindow> {
        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            log.info("OPENING WINDOW {}", parameters);
        }

        @Override
        public void apply(Long s, TimeWindow timeWindow, Iterable<Span> spans, Collector<Iterable<Span>> collector) {
            log.info("COLLECTING {}", s);
            collector.collect(spans);
        }
    }

    private static class AdjusterFunction<T extends Dedupable> extends RichMapFunction<Iterable<T>, Iterable<T>> implements ResultTypeQueryable<Iterable<T>> {
        private final SpanIdDeduplicator<T> deduplicator = new SpanIdDeduplicator<>();

        @Override
        public Iterable<T> map(Iterable<T> spans) {
            return deduplicator.adjust(spans);
        }

        @Override
        public TypeInformation<Iterable<T>> getProducedType() {
            return TypeInformation.of(new TypeHint<Iterable<T>>() {
            });
        }
    }

    private static class CountSpansAndLogLargeTraceIdFunction extends RichMapFunction<Iterable<Span>, Iterable<Span>> {
        private transient Histogram traceToSpans;

        @Override
        public void open(Configuration parameters) {
            com.codahale.metrics.Histogram histogram =
                    new com.codahale.metrics.Histogram(new SlidingTimeWindowReservoir(10, TimeUnit.SECONDS));
            this.traceToSpans = getRuntimeContext()
                    .getMetricGroup()
                    .histogram("traceToSpans", new DropwizardHistogramWrapper(histogram));
        }

        @Override
        public Iterable<Span> map(Iterable<Span> spans) {
            int size = 0;
            if (spans instanceof Collection) {
                size = ((Collection) spans).size();
            } else {
                for (Span ignored : spans) {
                    size++;
                }
            }

            if (size > 80000) {
                Span span = spans.iterator().next();
                log.info("Large trace traceId:{} spansPerTrace:{}", span.getTraceId(), size);
            }
            traceToSpans.update(size);
            return spans;
        }
    }
}
