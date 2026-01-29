package com.example.flink.functions;

import com.example.flink.model.ValidationResult;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

/**
 * PropertyEventRoutingFunction
 *
 * This function:
 * - Sends valid events forward
 * - (Later) can route invalid events to a DLQ
 */
public class PropertyEventRoutingFunction
        extends ProcessFunction<ValidationResult, GenericRecord> {

    @Override
    public void processElement(
            ValidationResult value,
            Context ctx,
            Collector<GenericRecord> out) {

        // Only forward valid records
        if (value.isValid()) {
            out.collect(value.getRecord());
        } else {
            // For now we just log invalid records
            System.err.println(
                    "INVALID EVENT: " + value.getErrorMessage()
            );
        }
    }
}
