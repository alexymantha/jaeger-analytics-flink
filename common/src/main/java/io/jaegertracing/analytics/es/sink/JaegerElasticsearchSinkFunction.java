package io.jaegertracing.analytics.es.sink;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@AllArgsConstructor
@Slf4j
public class JaegerElasticsearchSinkFunction<T> implements ElasticsearchSinkFunction<T> {
    private final String indexPrefix;
    private final String type;

    public IndexRequest createIndexRequest(T element) throws IOException {
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        return Requests.indexRequest()
                .index(String.format("%s-jaeger-dependencies-%s", indexPrefix, date.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .type(type)
                .source(new ObjectMapper().writeValueAsBytes(element), XContentType.JSON);
    }

    @Override
    public void process(T element, RuntimeContext ctx, RequestIndexer indexer) {
        System.out.println("Processing element");
        try {
            indexer.add(createIndexRequest(element));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
