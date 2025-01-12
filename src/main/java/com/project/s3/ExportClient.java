package com.project.s3;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExportClient {

    private static final String BUCKET_NAME = "newbucket37920"; // Updated S3 bucket name
    private static final String EXPORT_FILE = "exported_combined_summary.csv";
    private static final Logger logger = Logger.getLogger(ExportClient.class.getName()); // Logger instance
    private S3Client s3;

    public ExportClient() {
        // Initialize AWS S3 Client
        s3 = S3Client.builder()
                     .region(Region.US_EAST_1)
                     .credentialsProvider(ProfileCredentialsProvider.create())
                     .build();
    }

    // Method to download summarized and consolidated CSV files, combine them, and export as a single CSV
    public void downloadAndExport(String date) throws Exception {
        logger.info("Starting the export process for date: " + date);
    
        // Download Summarized and Consolidated CSV files for the given date
        Map<String, String[]> summarizedData = downloadSummarizedData(date);
        Map<String, String[]> consolidatedData = downloadConsolidatedData(date);
    
        // Prepare CSV Printer to write combined output to a new file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(EXPORT_FILE));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                     "Source IP", "Destination IP", "Date", "Total Flow Duration", "Total Forward Packets",
                     "Average Flow Duration", "StdDev Flow Duration", "Average Forward Packets", "StdDev Forward Packets"))) {
    
            for (String key : summarizedData.keySet()) {
                String[] summarized = summarizedData.get(key);
                String[] consolidated = consolidatedData.getOrDefault(key, new String[6]);
    
                // Ensure consolidated array has at least 6 elements
                if (consolidated.length < 6) {
                    consolidated = new String[]{"", "", "", "", "", ""};
                }
    
                // Write the merged data (summarized + consolidated) to the export CSV
                csvPrinter.printRecord(summarized[0], summarized[1], summarized[2], summarized[3], summarized[4],
                        consolidated[2], consolidated[3], consolidated[4], consolidated[5]);
            }
    
            logger.info("Data successfully written to: " + EXPORT_FILE);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while writing to the export file", e);
            throw e;
        }
    }

    // Method to download summarized data from S3 for a given date
    private Map<String, String[]> downloadSummarizedData(String date) throws Exception {
        Map<String, String[]> summarizedData = new HashMap<>();

        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(BUCKET_NAME)
                .prefix("processed/branch001/" + date + "/") // Corrected prefix for summarized files
                .build();
        
        ListObjectsV2Response listRes = s3.listObjectsV2(listReq);
        List<S3Object> objects = listRes.contents();

        logger.info("Found " + objects.size() + " summarized files for date: " + date);

        for (S3Object object : objects) {
            String key = object.key();

            logger.info("Downloading summarized file: " + key);

            // Download and process each summarized CSV file
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .build();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(s3.getObject(getObjectRequest)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split(",");
                    String fileSrcIp = fields[0];
                    String fileDstIp = fields[1];

                    // Use Src IP and Dst IP as the key
                    summarizedData.put(fileSrcIp + "-" + fileDstIp, fields);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error while processing summarized file: " + key, e);
                throw e;
            }
        }

        return summarizedData;
    }

    // Method to download consolidated data from S3
    private Map<String, String[]> downloadConsolidatedData(String date) throws Exception {
        Map<String, String[]> consolidatedData = new HashMap<>();

        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(BUCKET_NAME)
                .prefix("processed/processed/branch001/" + date + "/") // Corrected prefix for consolidated files
                .build();
        
        ListObjectsV2Response listRes = s3.listObjectsV2(listReq);
        List<S3Object> objects = listRes.contents();

        logger.info("Found " + objects.size() + " consolidated files for date: " + date);

        for (S3Object object : objects) {
            String key = object.key();

            logger.info("Downloading consolidated file: " + key);

            // Download and process each consolidated CSV file
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .build();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(s3.getObject(getObjectRequest)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split(",");
                    String fileSrcIp = fields[0];
                    String fileDstIp = fields[1];

                    // Use Src IP and Dst IP as the key
                    consolidatedData.put(fileSrcIp + "-" + fileDstIp, fields);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error while processing consolidated file: " + key, e);
                throw e;
            }
        }

        return consolidatedData;
    }

    public static void main(String[] args) {
        try {
            ExportClient client = new ExportClient();
            // Specify the date to filter traffic (for all device combinations)
            client.downloadAndExport("2025-01-11");
            logger.info("Export completed successfully.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error occurred during export", e);
        }
    }
}


