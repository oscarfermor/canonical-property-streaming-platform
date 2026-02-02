package com.example.flink.sink;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.data.RowData;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.flink.CatalogLoader;
import org.apache.iceberg.flink.TableLoader;
import org.apache.iceberg.flink.sink.FlinkSink;

import java.util.HashMap;
import java.util.Map;

public class IcebergSinks {

        public static void sinkValidEvents(
                        DataStream<RowData> stream,
                        String warehouse,
                        String namespace,
                        String tableName) {

                // Glue catalog properties
                Map<String, String> catalogProps = new HashMap<>();
                catalogProps.put("warehouse", warehouse);
                catalogProps.put("catalog-impl", "org.apache.iceberg.aws.glue.GlueCatalog");
                catalogProps.put("io-impl", "org.apache.iceberg.aws.s3.S3FileIO");

                // Hadoop conf (lightweight now)
                Configuration hadoopConf = new Configuration();
                hadoopConf.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");

                // Let AWS SDK resolve credentials:
                // - IAM Role (ECS / EKS / EMR)
                // - ~/.aws/credentials (local dev)
                hadoopConf.set(
                                "fs.s3a.aws.credentials.provider",
                                "com.amazonaws.auth.DefaultAWSCredentialsProviderChain");

                CatalogLoader catalogLoader = CatalogLoader.custom(
                                "glue",
                                catalogProps,
                                hadoopConf,
                                "org.apache.iceberg.aws.glue.GlueCatalog");

                TableIdentifier tableId = TableIdentifier.of(namespace, tableName);

                TableLoader tableLoader = TableLoader.fromCatalog(catalogLoader, tableId);

                FlinkSink.forRowData(stream)
                                .tableLoader(tableLoader)
                                .append();
        }
}
