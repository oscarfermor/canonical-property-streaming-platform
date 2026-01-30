package com.example.flink.functions;

import com.example.flink.model.PropertyEvent;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

/**
 * Validates PropertyEvent and routes invalid ones to a side output.
 */
public class PropertyEventValidationFunction
        extends ProcessFunction<PropertyEvent, PropertyEvent> {

    public static final OutputTag<String> INVALID_EVENTS = new OutputTag<String>("invalid-events") {
    };

    @Override
    public void processElement(
            PropertyEvent event,
            Context ctx,
            Collector<PropertyEvent> out) {

        if (event.getEventId() == null ||
                event.getEventType() == null ||
                event.getPayload() == null ||
                event.getPayload().getPropertyId() == null) {

            ctx.output(
                    INVALID_EVENTS,
                    "Invalid event: " + event);
            return;
        }

        // Event is valid → forward unchanged
        out.collect(event);
    }
}
