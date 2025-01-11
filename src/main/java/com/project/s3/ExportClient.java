package com.project.s3;

import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportClient {
    private final S3Client s3Client;
    private final String bucketName;

    public ExportClient(String region, String bucketName) {
        this.bucketName = bucketName;
        this.s3Client = S3Client.builder()
            .region(Region.of(region))
            .build();
    }

    public void exportDeviceCombination(List<String> fileNames, String date, String outputFilePath) throws IOException {
        StringBuilder summarizeData = new StringBuilder();
        StringBuilder consolidatorData = new StringBuilder();

        for (String fileName : fileNames) {
            try {
                // Determine the correct path for the file
                String filePath;
                if (fileName.contains("-summary")) {
                    filePath = "processed/branch001/" + date + "/" + fileName;
                } else if (fileName.contains("-consolidated")) {
                    filePath = "processed/processed/branch001/" + date + "/" + fileName;
                } else {
                    System.err.println("Unknown file type: " + fileName);
                    continue;
                }

                // Get the file from S3
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();

                String fileContent = s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes()).asUtf8String();

                // Append the content to the appropriate StringBuilder
                if (fileName.contains("-summary")) {
                    summarizeData.append(fileContent).append("\n");
                } else if (fileName.contains("-consolidated")) {
                    consolidatorData.append(fileContent).append("\n");
                }
            } catch (NoSuchKeyException e) {
                System.err.println("The specified key does not exist: " + fileName);
            }
        }

        // Export merged data to CSV file
        exportMergedData(summarizeData.toString(), consolidatorData.toString(), outputFilePath);

        System.out.println("Successfully exported data to " + outputFilePath);
    }

    // Method to export merged data from Summarizer and Consolidator results into a CSV file
    public void exportMergedData(String summarizeData, String consolidatorData, String outputFilePath) throws IOException {
        // Maps to store the results from Summarizer and Consolidator
        Map<String, TrafficSummary> summarizeMap = new HashMap<>();
        Map<String, ConsolidatedStats> consolidatorMap = new HashMap<>();

        // Parse the Summarizer data (CSV String) and store it in summarizeMap
        parseSummarizeData(summarizeData, summarizeMap);

        // Parse the Consolidator data (CSV String) and store it in consolidatorMap
        parseConsolidatorData(consolidatorData, consolidatorMap);

        // Write the merged data to a CSV file
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(outputFilePath), StandardCharsets.UTF_8)) {
            // Write the header of the CSV file
            writer.write(
                "Source IP,Destination IP,Date,Total Flow Duration,Total Forward Packets,Average Flow Duration,StdDev Flow Duration,Average Forward Packets,StdDev Forward Packets\n");

            // Merge the data from both maps and write to the CSV file
            for (String key : summarizeMap.keySet()) {
                TrafficSummary summary = summarizeMap.get(key);
                ConsolidatedStats stats = consolidatorMap.get(key);

                // If we have data for both summarizer and consolidator
                if (stats != null) {
                    // Write the merged data to the CSV file
                    writer.write(String.format("%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                        key, // Source IP and Destination IP
                        summary.totalFlowDuration, summary.totalForwardPackets, // Total Flow Duration, Total Forward Packets
                        stats.averageFlowDuration, stats.stdDevFlowDuration, // Average Flow Duration, StdDev Flow Duration
                        stats.averageForwardPackets, stats.stdDevForwardPackets)); // Average Forward Packets, StdDev Forward Packets
                }
            }
        }
    }

    // Method to parse the Summarizer data (CSV string) and store it in a map
    private void parseSummarizeData(String data, Map<String, TrafficSummary> summarizeMap) {
        String[] lines = data.split("\n"); // Split the input data into lines

        boolean isFirstLine = true;
        for (String line : lines) {
            if (isFirstLine) {
                isFirstLine = false;
                continue; // Skip the header line
            }

            String[] values = line.split(",");
            if (values.length < 5)
                continue; // Skip invalid lines

            String sourceIP = values[0].trim();
            String destIP = values[1].trim();
            double flowDuration = Double.parseDouble(values[3].trim());
            long forwardPackets = Long.parseLong(values[4].trim());

            String key = sourceIP + "," + destIP;
            summarizeMap.put(key, new TrafficSummary(flowDuration, forwardPackets));
        }
    }

    // Method to parse the Consolidator data (CSV string) and store it in a map
    private void parseConsolidatorData(String data, Map<String, ConsolidatedStats> consolidatorMap) {
        String[] lines = data.split("\n"); // Split the input data into lines

        boolean isFirstLine = true;
        for (String line : lines) {
            if (isFirstLine) {
                isFirstLine = false;
                continue; // Skip the header line
            }

            String[] values = line.split(",");
            if (values.length < 6)
                continue; // Skip invalid lines

            String sourceIP = values[0].trim();
            String destIP = values[1].trim();
            double avgFlowDuration = Double.parseDouble(values[2].trim());
            double stdDevFlowDuration = Double.parseDouble(values[3].trim());
            double avgForwardPackets = Double.parseDouble(values[4].trim());
            double stdDevForwardPackets = Double.parseDouble(values[5].trim());

            String key = sourceIP + "," + destIP;
            consolidatorMap.put(key, new ConsolidatedStats(avgFlowDuration, stdDevFlowDuration, avgForwardPackets, stdDevForwardPackets));
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: java ExportClient <outputFilePath> <date> <fileName1> <fileName2> ... <fileNameN>");
            System.exit(1);
        }

        String outputFilePath = args[0];
        String date = args[1];
        List<String> fileNames = List.of(args).subList(2, args.length);

        // Assuming region and bucketName are configured here
        String region = "us-east-1";  // Your specified region
        String bucketName = "newbucket37920";  // Your specified bucket

        try {
            ExportClient client = new ExportClient(region, bucketName);
            client.exportDeviceCombination(fileNames, date, outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

// TrafficSummary class to store summary data
class TrafficSummary {
    double totalFlowDuration;
    long totalForwardPackets;

    public TrafficSummary(double totalFlowDuration, long totalForwardPackets) {
        this.totalFlowDuration = totalFlowDuration;
        this.totalForwardPackets = totalForwardPackets;
    }
}

// ConsolidatedStats class to store consolidated statistics
class ConsolidatedStats {
    double averageFlowDuration;
    double stdDevFlowDuration;
    double averageForwardPackets;
    double stdDevForwardPackets;

    public ConsolidatedStats(double averageFlowDuration, double stdDevFlowDuration, double averageForwardPackets, double stdDevForwardPackets) {
        this.averageFlowDuration = averageFlowDuration;
        this.stdDevFlowDuration = stdDevFlowDuration;
        this.averageForwardPackets = averageForwardPackets;
        this.stdDevForwardPackets = stdDevForwardPackets;
    }
}
