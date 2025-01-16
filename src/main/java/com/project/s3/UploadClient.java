package com.project.s3;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.nio.file.Path;
import java.time.LocalDate;

public class UploadClient {
    private final S3Client s3Client;
    private final SnsClient snsClient;
    private final String bucketName;
    private final String branchId;
    private final String snsTopicArn;

    public UploadClient(String branchId, String region, String bucketName, String snsTopicArn) {
        this.branchId = branchId;
        this.bucketName = bucketName;
        this.snsTopicArn = snsTopicArn;
        this.s3Client = S3Client.builder()
            .region(Region.of(region))
            .build();
        this.snsClient = SnsClient.builder()
            .region(Region.of(region))
            .build();
    }

    public void uploadFile(Path filePath) {
        try {
            // Create a key in format: branchId/date/filename.csv
            String key = String.format("%s/%s/%s",
                branchId,
                LocalDate.now(),
                filePath.getFileName());

            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            s3Client.putObject(request, RequestBody.fromFile(filePath));
            System.out.println("Successfully uploaded file: " + filePath + " to key: " + key);

            // Publish message to SNS
            String message = String.format("File uploaded: %s", key);
            PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(snsTopicArn)
                .message(message)
                .build();
            PublishResponse publishResponse = snsClient.publish(publishRequest);
            System.out.println("Message published to SNS: " + publishResponse.messageId());

        } catch (S3Exception e) {
            throw new RuntimeException("Failed to upload file: " + filePath, e);
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java UploadClient <filePath>");
            System.exit(1);
        }

        String filePath = args[0];

        // Assuming branchId, region, bucketName, and snsTopicArn are configured here
        String branchId = "branch001"; // You can modify this as needed
        String region = "us-east-1";  // Your specified region
        String bucketName = "newbucket37920";  // Your specified bucket
        String snsTopicArn = "arn:aws:sns:us-east-1:082171137258:NotifIoTTraffic"; // Your specified SNS topic ARN

        UploadClient client = new UploadClient(branchId, region, bucketName, snsTopicArn);
        client.uploadFile(Path.of(filePath));
    }
}