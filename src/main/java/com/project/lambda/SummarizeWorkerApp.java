package com.project.lambda;

import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SummarizeWorkerApp {
    private static final S3Client s3Client = S3Client.builder().region(Region.of("us-east-1")).build();
    private static final SqsClient sqsClient = SqsClient.builder().region(Region.of("us-east-1")).build();
    private static final SummarizeWorker summarizeWorker = new SummarizeWorker();
    private static final String BUCKET_NAME = "newbucket37920"; // Updated S3 bucket name
    private static final String SQS_URL = "https://sqs.us-east-1.amazonaws.com/082171137258/IoTTrafficQueue"; // Your specified SQS URL

    public static void main(String[] args) {
        while (true) {
            System.out.println("Polling for messages...");
            ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(SQS_URL)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(20)
                .build();

            List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
            System.out.println("Received " + messages.size() + " messages.");
            for (Message message : messages) {
                processMessage(message);
            }
        }
    }

    private static void processMessage(Message message) {
        String messageBody = message.body();
        System.out.println("Processing message: " + messageBody);

        try {
            // Parse the SNS notification message
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(messageBody);
            String key = jsonNode.get("Message").asText().replace("File uploaded: ", "");
            System.out.println("Extracted key: " + key);

            // Get the file from S3
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build();

            // Get and process the S3 object
            String processedData = s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes()).asUtf8String();

            // Process the data using SummarizeWorker
            String summarizedData = summarizeWorker.processCSVFile(new ByteArrayInputStream(processedData.getBytes(StandardCharsets.UTF_8)));

            // Create output key
            String outputKey = "processed/" + key.replace(".csv", "-summary-javaapp.csv");

            // Upload the summarized data back to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(outputKey)
                .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromString(summarizedData));
            System.out.println("Successfully processed and uploaded summarized data for: " + key);

        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


