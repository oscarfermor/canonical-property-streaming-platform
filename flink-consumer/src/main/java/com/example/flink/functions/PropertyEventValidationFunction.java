package com.example.flink.functions;

import com.example.flink.model.ValidationResult;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

/**
 * PropertyEventValidationFunction
 *
 * This function:
 * - Validates required fields
 * - Applies simple business rules
 * - Normalizes values (trim, uppercase, defaults)
 *
 * Input  : GenericRecord (raw Avro from Kafka)
 * Output : ValidationResult (valid or invalid)
 */
public class PropertyEventValidationFunction
        extends ProcessFunction<GenericRecord, ValidationResult> {

    @Override
    public void processElement(
            GenericRecord record,
            Context ctx,
            Collector<ValidationResult> out) {

        /*
         * === VALIDATION ===
         * Check that required fields exist and are usable.
         */

        String eventId = getString(record, "event_id");
        String eventType = getString(record, "event_type");
        Long eventTime = getLong(record, "event_time");

        // event_id must exist
        if (eventId == null || eventId.isBlank()) {
            out.collect(
                    ValidationResult.error(
                            "event_id is missing",
                            record
                    )
            );
            return;
        }

        // event_type must exist
        if (eventType == null || eventType.isBlank()) {
            out.collect(
                    ValidationResult.error(
                            "event_type is missing",
                            record
                    )
            );
            return;
        }

        // event_time must be a valid timestamp
        if (eventTime == null || eventTime <= 0) {
            out.collect(
                    ValidationResult.error(
                            "event_time is invalid",
                            record
                    )
            );
            return;
        }

        /*
         * === NORMALIZATION ===
         * Modify fields in-place to standardize data.
         */

        // Normalize event_type (trim + uppercase)
        record.put(
                "event_type",
                eventType.trim().toUpperCase()
        );

        // Default source_system if missing
        record.put(
                "source_system",
                normalizeString(
                        record.get("source_system"),
                        "UNKNOWN"
                )
        );

        /*
         * Normalize nested payload (if present).
         * payload is nullable in the Avro schema.
         */
        GenericRecord payload =
                (GenericRecord) record.get("payload");

        if (payload != null) {
            normalizePayload(payload);
        }

        // If everything is OK, emit a VALID result
        out.collect(
                ValidationResult.ok(record)
        );
    }

    /**
     * Normalize fields inside the payload record.
     */
    private void normalizePayload(GenericRecord payload) {

        // Trim property_id
        String propertyId =
                getString(payload, "property_id");

        if (propertyId != null) {
            payload.put(
                    "property_id",
                    propertyId.trim()
            );
        }

        /*
         * Business rule:
         * If price is negative, drop it (set to null).
         */
        Double price = (Double) payload.get("price");

        if (price != null && price < 0) {
            payload.put("price", null);
        }
    }

    /*
     * === Helper methods ===
     * These avoid repetitive null checks everywhere.
     */

    private String normalizeString(
            Object value,
            String defaultValue) {

        if (value == null) {
            return defaultValue;
        }

        String s = value.toString().trim();
        return s.isEmpty() ? defaultValue : s;
    }

    private String getString(
            GenericRecord record,
            String fieldName) {

        Object value = record.get(fieldName);
        return value == null ? null : value.toString();
    }

    private Long getLong(
            GenericRecord record,
            String fieldName) {

        Object value = record.get(fieldName);
        return value instanceof Long
                ? (Long) value
                : null;
    }
}