package com.example.flink.iceberg;

import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.Schema;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.hadoop.HadoopCatalog;
import org.apache.iceberg.types.Types;

public class CreateIcebergTable {

    public static void main(String[] args) {

        String warehouse = "s3a://canonical-property-streaming-platform/iceberg-warehouse";
        TableIdentifier tableId = TableIdentifier.of("db", "property_events_valid");

        try (HadoopCatalog catalog = new HadoopCatalog(
                new Configuration(),
                warehouse)) {

            if (!catalog.tableExists(tableId)) {

                Schema schema = new Schema(
                        Types.NestedField.required(1, "event_id", Types.StringType.get()),
                        Types.NestedField.required(2, "event_type", Types.StringType.get()),
                        Types.NestedField.required(3, "source_system", Types.StringType.get()),
                        Types.NestedField.required(4, "event_time", Types.TimestampType.withZone()),
                        Types.NestedField.optional(5, "property_id", Types.StringType.get()),
                        Types.NestedField.optional(6, "price", Types.DoubleType.get()),
                        Types.NestedField.optional(7, "status", Types.StringType.get()));

                catalog.createTable(tableId, schema);
                System.out.println("Iceberg table created");
            } else {
                System.out.println("Iceberg table already exists");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to create Iceberg table", e);
        }
    }
}
