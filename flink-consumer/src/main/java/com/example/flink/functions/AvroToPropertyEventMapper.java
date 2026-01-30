package com.example.flink.functions;

import com.example.flink.model.PropertyEvent;
import com.example.flink.model.PropertyPayload;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.functions.MapFunction;

/**
 * Converts Avro GenericRecord into domain POJO.
 * This is the ONLY place where GenericRecord exists.
 */
public class AvroToPropertyEventMapper
        implements MapFunction<GenericRecord, PropertyEvent> {

    @Override
    public PropertyEvent map(GenericRecord record) {

        GenericRecord payloadRecord = (GenericRecord) record.get("payload");

        PropertyPayload payload = null;

        if (payloadRecord != null) {
            payload = new PropertyPayload(
                    payloadRecord.get("property_id").toString(),
                    (Double) payloadRecord.get("price"),
                    payloadRecord.get("status") != null
                            ? payloadRecord.get("status").toString()
                            : null);
        }

        return new PropertyEvent(
                record.get("event_id").toString(),
                record.get("event_type").toString(),
                record.get("source_system").toString(),
                (Long) record.get("event_time"),
                payload);
    }
}
