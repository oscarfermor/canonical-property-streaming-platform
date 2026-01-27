package com.example.flink;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.InputStream;

public class PropertyEventConsumer {

    public static void main(String[] args) throws Exception {

        // Flink environment
        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();

        // Load Avro schema from resources
        InputStream schemaStream =
                PropertyEventConsumer.class
                        .getClassLoader()
                        .getResourceAsStream("schemas/property_event_v1.avsc");

        if (schemaStream == null) {
            throw new RuntimeException(
                    "Avro schema not found at src/main/resources/schemas/property_event_v1.avsc"
            );
        }

        Schema schema = new Schema.Parser().parse(schemaStream);

        // Kafka source with Confluent Schema Registry
        KafkaSource<GenericRecord> source =
                KafkaSource.<GenericRecord>builder()
                        .setBootstrapServers("kafka:29092")
                        .setTopics("property_events")
                        .setGroupId("flink-property-consumer")
                        .setStartingOffsets(OffsetsInitializer.earliest())
                        .setValueOnlyDeserializer(
                                ConfluentRegistryAvroDeserializationSchema.forGeneric(
                                        schema,
                                        "http://schema-registry:8081"
                                )
                        )
                        .build();

        // Consume & print
        env.fromSource(
                        source,
                        WatermarkStrategy.noWatermarks(),
                        "property-events-source"
                )
                .print();

        // Execute job
        env.execute("Property Events Consumer");
    }
}
