package com.example.flink.model;

import java.io.Serializable;

/**
 * Canonical domain event used inside Flink.
 * This replaces GenericRecord everywhere after ingestion.
 */
public class PropertyEvent implements Serializable {

    private String eventId;
    private String eventType;
    private String sourceSystem;
    private long eventTime;
    private PropertyPayload payload;

    // Required empty constructor
    public PropertyEvent() {
    }

    public PropertyEvent(
            String eventId,
            String eventType,
            String sourceSystem,
            long eventTime,
            PropertyPayload payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.sourceSystem = sourceSystem;
        this.eventTime = eventTime;
        this.payload = payload;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public long getEventTime() {
        return eventTime;
    }

    public PropertyPayload getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "PropertyEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", sourceSystem='" + sourceSystem + '\'' +
                ", eventTime=" + eventTime +
                ", payload=" + payload +
                '}';
    }
}
