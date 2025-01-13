package com.project.lambda;

import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SummarizeWorkerApp {
    private static final S3Client s3Client = S3Client.builder().build();
    private static final SummarizeWorker summarizeWorker = new SummarizeWorker();
    private static final String BUCKET_NAME = "newbucket37920"; // Updated S3 bucket name

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java SummarizeWorkerApp <key>");
            System.exit(1);
        }

        String key = args[0];

        try {
            // Get the file from S3
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build();
            
            // Get and process the S3 object
            String processedData = s3Client.getObject(getObjectRequest, 
                ResponseTransformer.toBytes()).asUtf8String();

            // Process the data using SummarizeWorker
            String summarizedData = summarizeWorker.processCSVFile(
                new ByteArrayInputStream(processedData.getBytes(StandardCharsets.UTF_8)));

            // Create output key
            String outputKey = "processed/" + key.replace(".csv", "-summary.csv");

            // Upload the summarized data back to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(outputKey)
                .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromString(summarizedData));

            System.out.println("Successfully uploaded summarized data to: " + outputKey);

        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


