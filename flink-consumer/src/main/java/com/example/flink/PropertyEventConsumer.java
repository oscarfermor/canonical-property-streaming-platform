package com.example.flink;

import com.example.flink.functions.AvroToPropertyEventMapper;
import com.example.flink.functions.PropertyEventValidationFunction;
import com.example.flink.model.PropertyEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.InputStream;

public class PropertyEventConsumer {

        public static void main(String[] args) throws Exception {

                StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

                InputStream schemaStream = PropertyEventConsumer.class
                                .getClassLoader()
                                .getResourceAsStream(
                                                "schemas/property_event_v1.avsc");

                Schema schema = new Schema.Parser().parse(schemaStream);

                KafkaSource<GenericRecord> source = KafkaSource.<GenericRecord>builder()
                                .setBootstrapServers("kafka:29092")
                                .setTopics("property_events")
                                .setGroupId("flink-property-consumer")
                                .setStartingOffsets(OffsetsInitializer.earliest())
                                .setValueOnlyDeserializer(
                                                ConfluentRegistryAvroDeserializationSchema
                                                                .forGeneric(
                                                                                schema,
                                                                                "http://schema-registry:8081"))
                                .build();

                // 1️⃣ Read from Kafka
                DataStream<GenericRecord> rawStream = env.fromSource(
                                source,
                                WatermarkStrategy.noWatermarks(),
                                "kafka-source");

                // 2️⃣ Convert Avro → POJO
                DataStream<PropertyEvent> eventStream = rawStream.map(new AvroToPropertyEventMapper());

                // 3️⃣ Validate
                PropertyEventValidationFunction validator = new PropertyEventValidationFunction();

                SingleOutputStreamOperator<PropertyEvent> validEvents = eventStream.process(validator);

                DataStream<String> invalidEvents = validEvents.getSideOutput(
                                PropertyEventValidationFunction.INVALID_EVENTS);

                validEvents.print("VALID");
                invalidEvents.print("INVALID");

                env.execute("Property Events Consumer");
        }
}
