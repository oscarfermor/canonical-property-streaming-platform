package com.example.flink.iceberg;

import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.Schema;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.aws.glue.GlueCatalog;
import org.apache.iceberg.catalog.SupportsNamespaces;
import org.apache.iceberg.types.Types;

import java.util.HashMap;
import java.util.Map;

public class CreateIcebergTable {

    public static void main(String[] args) {

        // 1) Configure GlueCatalog properties
        Map<String, String> glueProps = new HashMap<>();
        // S3 location for Iceberg metadata + data
        glueProps.put(CatalogProperties.WAREHOUSE_LOCATION,
                "s3://canonical-property-streaming-platform/iceberg-warehouse");
        // Optional: specify region if not via environment
        glueProps.put("glue.region", System.getenv("AWS_REGION"));

        // 2) Initialize GlueCatalog
        GlueCatalog glueCatalog = new GlueCatalog();
        glueCatalog.initialize("glue_catalog", glueProps);

        Namespace namespace = Namespace.of("db");
        TableIdentifier tableId = TableIdentifier.of(namespace, "property_events_valid");

        // 3) Create namespace if supported and not present
        if (glueCatalog instanceof SupportsNamespaces) {
            SupportsNamespaces nsCatalog = (SupportsNamespaces) glueCatalog;
            try {
                // listNamespaces returns namespace children, and namespaceExists is a default method
                boolean namespaceExists = nsCatalog.listNamespaces().contains(namespace);
                if (!namespaceExists) {
                    nsCatalog.createNamespace(namespace, new HashMap<>());
                    System.out.println("Created namespace: " + namespace);
                }
            } catch (Exception e) {
                // optionally handle any exceptions
                System.out.println("Issue checking/creating namespace: " + e.getMessage());
            }
        }

        // 4) Create Iceberg table if needed
        if (!glueCatalog.tableExists(tableId)) {
            Schema schema = new Schema(
                    Types.NestedField.required(1, "event_id", Types.StringType.get()),
                    Types.NestedField.required(2, "event_type", Types.StringType.get()),
                    Types.NestedField.required(3, "source_system", Types.StringType.get()),
                    Types.NestedField.required(4, "event_time", Types.TimestampType.withZone()),
                    Types.NestedField.optional(5, "property_id", Types.StringType.get()),
                    Types.NestedField.optional(6, "price", Types.DoubleType.get()),
                    Types.NestedField.optional(7, "status", Types.StringType.get())
            );

            glueCatalog.createTable(tableId, schema);
            System.out.println("Created Iceberg table in Glue: " + tableId);
        } else {
            System.out.println("Iceberg table already exists: " + tableId);
        }

        // 5) Clean up
        System.out.println("Done.");
    }
}
