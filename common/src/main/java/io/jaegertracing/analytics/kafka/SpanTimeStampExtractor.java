package io.jaegertracing.analytics.kafka;

import io.jaegertracing.analytics.es.model.JsonSpan;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;

@Slf4j
public class SpanTimeStampExtractor extends AscendingTimestampExtractor<JsonSpan> {

    @Override
    public long extractAscendingTimestamp(JsonSpan element) {
        return System.currentTimeMillis();
    }
}
