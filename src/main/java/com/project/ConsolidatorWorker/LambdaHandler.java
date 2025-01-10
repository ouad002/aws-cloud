package com.project.ConsolidatorWorker;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LambdaHandler implements RequestHandler<S3Event, String> {
    private final S3Client s3Client = S3Client.builder().build();
    private final ConsolidatorWorker consolidatorWorker = new ConsolidatorWorker();

    @Override
    public String handleRequest(S3Event s3Event, Context context) {
        try {
            // Get the S3 bucket and key from the event
            String sourceBucket = s3Event.getRecords().get(0).getS3().getBucket().getName();
            String sourceKey = s3Event.getRecords().get(0).getS3().getObject().getKey();
            
            context.getLogger().log("Processing file: " + sourceKey + " from bucket: " + sourceBucket);

            // Get the file from S3
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(sourceBucket)
                .key(sourceKey)
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
            String outputKey = sourceKey.replace(".csv", "-consolidated.csv");

            // Upload the processed data back to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(sourceBucket)
                .key("processed/" + outputKey)
                .build();

            s3Client.putObject(putObjectRequest, 
                RequestBody.fromBytes(outputStream.toByteArray()));

            return "Successfully processed " + sourceKey;

        } catch (IOException e) {
            context.getLogger().log("Error processing file: " + e.getMessage());
            throw new RuntimeException("Error processing S3 event", e);
        }
    }
}

