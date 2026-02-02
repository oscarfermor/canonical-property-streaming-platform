package com.example.flink.mapper;

import com.example.flink.model.PropertyEvent;
import com.example.flink.model.PropertyPayload;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;

public class PropertyEventToRowDataMapper
        implements MapFunction<PropertyEvent, RowData> {

    @Override
    public RowData map(PropertyEvent event) {

        PropertyPayload payload = event.getPayload();

        GenericRowData row = new GenericRowData(4);

        row.setField(0, StringData.fromString(payload.getPropertyId()));   // STRING
        row.setField(1, payload.getPrice());                               // DOUBLE
        row.setField(2, StringData.fromString("USD"));                     // STRING
        row.setField(3, TimestampData.fromEpochMillis(event.getEventTime())); // TIMESTAMP

        return row;
    }
}