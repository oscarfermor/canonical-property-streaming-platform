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
                /*
                 * Kafka message arrives
                 * ↓
                 * Deserialized as GenericRecord
                 * ↓
                 * rawStream emits it
                 * ↓
                 * map() converts it to PropertyEvent
                 * ↓
                 * process():
                 * if valid → goes to validEvents
                 * if invalid → goes to side output
                 * ↓
                 * print() writes to TaskManager logs
                 * ↓
                 * event is gone forever
                 */

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

                // Read from Kafka -> Every message coming from Kafka, one by one
                DataStream<GenericRecord> rawStream = env.fromSource(
                                source,
                                WatermarkStrategy.noWatermarks(),
                                "kafka-source");

                // Convert Avro -> POJO per incoming event
                DataStream<PropertyEvent> eventStream = rawStream.map(new AvroToPropertyEventMapper());

                // Validate
                PropertyEventValidationFunction validator = new PropertyEventValidationFunction();

                // zero, one or many events, has context, side outputs
                // eventStream -> Validator -> validEvents || INVALID_EVENTS
                // From the validator operator, give me everything it emitted to INVALID_EVENTS
                SingleOutputStreamOperator<PropertyEvent> validEvents = eventStream.process(validator);

                DataStream<String> invalidEvents = validEvents.getSideOutput(
                                PropertyEventValidationFunction.INVALID_EVENTS);

                validEvents.print("VALID");
                invalidEvents.print("INVALID");

                env.execute("Property Events Consumer");
        }
}
