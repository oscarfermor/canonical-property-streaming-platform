package com.example.flink.model;

import org.apache.avro.generic.GenericRecord;

/**
 * ValidationResult
 *
 * This class wraps a record and tells us:
 * - Is it valid?
 * - If not, why?
 *
 * This avoids throwing exceptions in streaming jobs.
 */
public class ValidationResult {

    private final boolean valid;
    private final String errorMessage;
    private final GenericRecord record;

    // Private constructor (forces use of static methods)
    private ValidationResult(
            boolean valid,
            String errorMessage,
            GenericRecord record) {

        this.valid = valid;
        this.errorMessage = errorMessage;
        this.record = record;
    }

    /**
     * Create a VALID result.
     */
    public static ValidationResult ok(
            GenericRecord record) {

        return new ValidationResult(
                true,
                null,
                record
        );
    }

    /**
     * Create an INVALID result.
     */
    public static ValidationResult error(
            String errorMessage,
            GenericRecord record) {

        return new ValidationResult(
                false,
                errorMessage,
                record
        );
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public GenericRecord getRecord() {
        return record;
    }
}