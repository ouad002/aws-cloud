package com.project.s3;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.file.Path;
import java.time.LocalDate;

public class UploadClient {
    private final S3Client s3Client;
    private final String bucketName;
    private final String branchId;

    public UploadClient(String branchId, String region, String bucketName) {
        this.branchId = branchId;
        this.bucketName = bucketName;
        this.s3Client = S3Client.builder()
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
            
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to upload file: " + filePath, e);
        }
    }

    public static void main(String[] args) {
        // Using your specific values
        String branchId = "branch001"; // You can modify this as needed
        String region = "us-east-1";  // Your specified region
        String bucketName = "newbucket37920";  // Your specified bucket
        String filePath = "/mnt/c/Users/user/Downloads/data-20221207.csv";  // Your specified file path

        UploadClient client = new UploadClient(branchId, region, bucketName);
        client.uploadFile(Path.of(filePath));
    }
}
