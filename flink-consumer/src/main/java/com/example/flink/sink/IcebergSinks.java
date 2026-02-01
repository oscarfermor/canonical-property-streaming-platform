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

                Map<String, String> catalogProps = new HashMap<>();
                catalogProps.put("warehouse", warehouse);

                Configuration hadoopConf = new Configuration();

                // REQUIRED: S3A credentials
                hadoopConf.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
                hadoopConf.set(
                                "fs.s3a.aws.credentials.provider",
                                "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");
                hadoopConf.set(
                                "fs.s3a.access.key",
                                System.getenv("AWS_ACCESS_KEY_ID"));
                hadoopConf.set(
                                "fs.s3a.secret.key",
                                System.getenv("AWS_SECRET_ACCESS_KEY"));
                hadoopConf.set("fs.s3a.endpoint", "s3.amazonaws.com");

                CatalogLoader catalogLoader = CatalogLoader.hadoop(
                                "hadoop",
                                hadoopConf,
                                catalogProps);

                TableIdentifier tableId = TableIdentifier.of(namespace, tableName);

                TableLoader tableLoader = TableLoader.fromCatalog(catalogLoader, tableId);

                FlinkSink.forRowData(stream)
                                .tableLoader(tableLoader)
                                .append();
        }
}
