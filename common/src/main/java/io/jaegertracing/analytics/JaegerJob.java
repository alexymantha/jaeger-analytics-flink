package io.jaegertracing.analytics;

import io.jaegertracing.analytics.cassandra.ClusterBuilder;
import io.jaegertracing.analytics.config.Utils;
import io.jaegertracing.analytics.kafka.deserializer.JsonSpanDeserializer;
import io.jaegertracing.analytics.es.model.JsonSpan;
import io.jaegertracing.analytics.es.sink.JaegerElasticsearchSinkFunction;
import io.jaegertracing.analytics.es.sink.SecuredRestClientFactory;
import io.jaegertracing.analytics.kafka.SpanTimeStampExtractor;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.cassandra.CassandraPojoSink;
import org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Properties;

public interface JaegerJob<T> {
    /**
     * These constants are used to provide user friendly names for Flink operators. Flink also uses them in
     * metric names.
     */
    String KAFKA_SOURCE = "KafkaSource";

    Logger logger = LoggerFactory.getLogger(JaegerJob.class);

    void setupJob(ParameterTool parameterTool, DataStream<JsonSpan> spans, SinkFunction<T> sinkFunction) throws Exception;

    default void executeJob(String jobName, ParameterTool parameterTool, SinkStorage sinkStorage, TypeInformation<T> sinkType) throws Exception {
        logger.info("Beginning job execution");
        System.out.println("Starting JOB " + jobName);

        FlinkKafkaConsumer<JsonSpan> consumer = configureKafkaConsumer(parameterTool);
        SinkFunction<T> sink = null;
        switch (sinkStorage) {
            case CASSANDRA:
                sink = configureCassandraSink(parameterTool, sinkType);
                break;
            case ELASTICSEARCH:
                sink = configureElasticsearchSink(parameterTool);
                break;
        }

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        configureEnvironmentForJob(parameterTool, env);

        SingleOutputStreamOperator<JsonSpan> spanSource = env.addSource(consumer)
                .name(KAFKA_SOURCE);
        setupJob(parameterTool, spanSource, sink);

        env.execute(jobName);
    }

    default FlinkKafkaConsumer<JsonSpan> configureKafkaConsumer(ParameterTool parameterTool) {
        logger.info("Setting up Kafka");
        String topic = parameterTool.get("kafka.topic", "");
        Properties kafkaProperties = Utils.filterPrefix(parameterTool.getProperties(), "kafka");
        FlinkKafkaConsumer<JsonSpan> kafkaConsumer = new FlinkKafkaConsumer<>(topic, new JsonSpanDeserializer(), kafkaProperties);

        //kafkaConsumer.assignTimestampsAndWatermarks(new SpanTimeStampExtractor());
        kafkaConsumer.setStartFromLatest();
        return kafkaConsumer;
    }

    default SinkFunction<T> configureCassandraSink(ParameterTool parameterTool, TypeInformation<T> typeInformation) {
        logger.info("Setting up Cassandra");
        String contactPoint = parameterTool.get("cassandra.contactpoint", "localhost");
        String keySpace = parameterTool.get("cassandra.keyspace", "jaeger_v1_local");
        short port = parameterTool.getShort("cassandra.port", (short) 9042);
        String username = parameterTool.get("cassandra.username");
        String password = parameterTool.get("cassandra.password");

        ClusterBuilder clusterBuilder = ClusterBuilder.builder()
                .contactPoints(contactPoint)
                .port(port)
                .username(username)
                .password(password)
                .build();

        return new CassandraPojoSink<T>(typeInformation.getTypeClass(), clusterBuilder, keySpace);
    }

    default SinkFunction<T> configureElasticsearchSink(ParameterTool parameterTool) {
        logger.info("Setting up Elasticsearch");

        String elasticUrl = parameterTool.get("elasticsearch.url");
        String type = parameterTool.get("elasticsearch.type", "jaegerDependency");
        String indexPrefix = parameterTool.get("elasticsearch.index.prefix");

        ElasticsearchSink.Builder<T> builder = new ElasticsearchSink.Builder<T>(
                Arrays.asList(HttpHost.create(elasticUrl)),
                new JaegerElasticsearchSinkFunction<>(indexPrefix, type));

        String username = parameterTool.get("elasticsearch.username", "");
        String password = parameterTool.get("elasticsearch.password", "");

        builder.setRestClientFactory(new SecuredRestClientFactory(username, password));
        return builder.build();
    }

    default void configureEnvironmentForJob(ParameterTool parameterTool, StreamExecutionEnvironment env) {
        env.setStreamTimeCharacteristic(TimeCharacteristic.IngestionTime);

        ExecutionConfig executionConfig = env.getConfig();
        executionConfig.setMaxParallelism(Runtime.getRuntime().availableProcessors());
        executionConfig.enableObjectReuse();
        executionConfig.setGlobalJobParameters(parameterTool);

        // TODO: Read this from parameterTool
        CheckpointConfig checkpointConfig = env.getCheckpointConfig();
        checkpointConfig.enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.DELETE_ON_CANCELLATION);
        checkpointConfig.setCheckpointInterval(Time.minutes(10).toMilliseconds());
        checkpointConfig.setMaxConcurrentCheckpoints(1);
        checkpointConfig.setMinPauseBetweenCheckpoints(Time.minutes(5).toMilliseconds());
    }

    enum SinkStorage {
        CASSANDRA,
        ELASTICSEARCH
    }
}
