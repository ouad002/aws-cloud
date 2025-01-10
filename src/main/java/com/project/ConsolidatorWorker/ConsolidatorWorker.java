package com.project.worker;

import java.io.*;
import java.util.*;

public class ConsolidatorWorker {

    // Class to manage traffic statistics for a source-destination pair
    private static class TrafficStats {
        // Lists to store the flow durations and forward packets
        private final List<Double> flowDurations = new ArrayList<>();
        private final List<Long> forwardPackets = new ArrayList<>();

        // Method to add flow duration and forward packets to the lists
        public void add(double flowDuration, long forwardPacket) {
            flowDurations.add(flowDuration);  // Add flow duration
            forwardPackets.add(forwardPacket);  // Add forward packets count
        }

        // Method to calculate the average of a list of numeric values
        public double getAverage(List<? extends Number> values) {
            return values.stream().mapToDouble(Number::doubleValue).average().orElse(0.0);
        }

        // Method to calculate the standard deviation of a list of numeric values
        public double getStdDev(List<? extends Number> values, double average) {
            return Math.sqrt(values.stream()
                .mapToDouble(v -> Math.pow(v.doubleValue() - average, 2))
                .average().orElse(0.0));
        }

        // Method to get the average flow duration
        public double getAverageFlowDuration() {
            return getAverage(flowDurations);
        }

        // Method to get the standard deviation of the flow duration
        public double getStdDevFlowDuration() {
            return getStdDev(flowDurations, getAverageFlowDuration());
        }

        // Method to get the average forward packets
        public double getAverageForwardPackets() {
            return getAverage(forwardPackets);
        }

        // Method to get the standard deviation of the forward packets
        public double getStdDevForwardPackets() {
            return getStdDev(forwardPackets, getAverageForwardPackets());
        }
    }

    // Method to process the consolidation of traffic data and calculate statistics
    public void processConsolidation(InputStream inputStream, OutputStream outputStream) throws IOException {
        // Map to store traffic stats for each source-destination pair (key: sourceIP,destIP)
        Map<String, TrafficStats> trafficStatsMap = new HashMap<>();

        // Reading the input file and writing the output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {

            // Read the header line
            String header = reader.readLine();
            if (header == null || !header.contains("Source IP")) {
                throw new IOException("Invalid input file format");  // Validate the file format
            }

            // Process each line in the input file
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line, trafficStatsMap);  // Process and update the stats map
            }

            // Write the header for the output file
            writer.write("Source IP,Destination IP,Average Flow Duration,StdDev Flow Duration,Average Forward Packets,StdDev Forward Packets\n");

            // Write the results for each source-destination pair
            for (Map.Entry<String, TrafficStats> entry : trafficStatsMap.entrySet()) {
                String key = entry.getKey();  // Get the key (source IP, destination IP)
                TrafficStats stats = entry.getValue();  // Get the stats for this pair

                // Write the consolidated statistics for this source-destination pair
                writer.write(String.format("%s,%.2f,%.2f,%.2f,%.2f\n",
                    key,  // Source IP and Destination IP
                    stats.getAverageFlowDuration(), stats.getStdDevFlowDuration(),  // Flow Duration statistics
                    stats.getAverageForwardPackets(), stats.getStdDevForwardPackets()));  // Forward Packets statistics
            }
        }
    }

    // Method to process a single line of data from the input file
    private void processLine(String line, Map<String, TrafficStats> trafficStatsMap) {
        // Split the line by commas
        String[] values = line.split(",");

        // Extract the necessary columns (source IP, destination IP, flow duration, forward packets)
        String sourceIP = values[0].trim();  // Source IP
        String destIP = values[1].trim();    // Destination IP
        double flowDuration = Double.parseDouble(values[3].trim());  // Total Flow Duration
        long forwardPackets = Long.parseLong(values[4].trim());      // Total Forward Packets

        // Create a unique key for each source-destination pair (source IP, destination IP)
        String key = sourceIP + "," + destIP;

        // If the key doesn't exist in the map, create a new instance of TrafficStats
        TrafficStats stats = trafficStatsMap.computeIfAbsent(key, k -> new TrafficStats());

        // Add the data to the corresponding TrafficStats
        stats.add(flowDuration, forwardPackets);
    }
}

