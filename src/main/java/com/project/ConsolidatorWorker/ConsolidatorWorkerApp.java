package com.project.ConsolidatorWorker;

import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ConsolidatorWorkerApp {

    private final S3Client s3Client;
    private final ConsolidatorWorker consolidatorWorker;

    public ConsolidatorWorkerApp(String region) {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
        this.consolidatorWorker = new ConsolidatorWorker();
    }

    public void processFile(String bucketName, String key) throws IOException {
        // Get the file from S3
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

                // Get and process the S3 object using the correct ResponseTransformer
                String processedData = s3Client.getObject(getObjectRequest,
                        ResponseTransformer.toBytes()).asUtf8String();
        
                // Process the data using ConsolidatorWorker
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                consolidatorWorker.processConsolidation(
                        new ByteArrayInputStream(processedData.getBytes(StandardCharsets.UTF_8)),
                        outputStream);
        
                // Create output key
                String outputKey = key.replace(".csv", "-consolidated.csv");
        
                // Upload the processed data back to S3
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key("processed/" + outputKey)
                        .build();
        
                s3Client.putObject(putObjectRequest,
                        RequestBody.fromBytes(outputStream.toByteArray()));
        
                System.out.println("Successfully processed " + key);
            }

            public static void main(String[] args) {
                if (args.length != 3) {
                    System.err.println("Usage: java ConsolidatorWorkerApp <region> <bucketName> <key>");
                    System.exit(1);
                }
        
                String region = args[0];
                String bucketName = args[1];
                String key = args[2];
        
                try {
                    ConsolidatorWorkerApp app = new ConsolidatorWorkerApp(region);
                    app.processFile(bucketName, key);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }