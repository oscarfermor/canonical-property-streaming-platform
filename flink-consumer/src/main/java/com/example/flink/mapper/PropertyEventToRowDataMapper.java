package com.example.flink.mapper;

import com.example.flink.model.PropertyEvent;
import com.example.flink.model.PropertyPayload;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.api.common.functions.MapFunction;

public class PropertyEventToRowDataMapper
        implements MapFunction<PropertyEvent, RowData> {

    @Override
    public RowData map(PropertyEvent event) {

        GenericRowData row = new GenericRowData(7);

        row.setField(0, event.getEventId());
        row.setField(1, event.getEventType());
        row.setField(2, event.getSourceSystem());
        row.setField(3, event.getEventTime());

        PropertyPayload payload = event.getPayload();
        if (payload != null) {
            row.setField(4, payload.getPropertyId());
            row.setField(5, payload.getPrice());
            row.setField(6, payload.getStatus());
        } else {
            row.setField(4, null);
            row.setField(5, null);
            row.setField(6, null);
        }

        return row;
    }
}
