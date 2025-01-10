package com.project.lambda;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SummarizeWorker {
    // Class to hold summarized data
    private static class TrafficSummary {
        String sourceIP;
        String destIP;
        LocalDate date;
        double totalFlowDuration;
        long totalForwardPackets;

        public TrafficSummary(String sourceIP, String destIP, LocalDate date) {
            this.sourceIP = sourceIP;
            this.destIP = destIP;
            this.date = date;
            this.totalFlowDuration = 0.0;
            this.totalForwardPackets = 0;
        }

        @Override
        public String toString() {
            return String.format("%s,%s,%s,%.2f,%d",
                sourceIP, destIP, date, totalFlowDuration, totalForwardPackets);
        }
    }

    // Key class for grouping traffic data
    private static class TrafficKey {
        String sourceIP;
        String destIP;
        LocalDate date;

        public TrafficKey(String sourceIP, String destIP, LocalDate date) {
            this.sourceIP = sourceIP;
            this.destIP = destIP;
            this.date = date;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TrafficKey that = (TrafficKey) o;
            return Objects.equals(sourceIP, that.sourceIP) &&
                   Objects.equals(destIP, that.destIP) &&
                   Objects.equals(date, that.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceIP, destIP, date);
        }
    }

    public String processCSVFile(InputStream inputStream) throws IOException {
        Map<TrafficKey, TrafficSummary> summaries = new HashMap<>();
        StringBuilder output = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            // Read header
            String header = reader.readLine();
            if (header == null) {
                throw new IOException("Empty CSV file");
            }

            // Find required column indices
            Map<String, Integer> columnIndices = parseHeader(header);
            
            // Process each line
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line, columnIndices, summaries);
            }
        }

        // Write results
        output.append("Source IP,Destination IP,Date,Total Flow Duration,Total Forward Packets\n");
        for (TrafficSummary summary : summaries.values()) {
            output.append(summary.toString()).append("\n");
        }

        return output.toString();
    }

    private Map<String, Integer> parseHeader(String header) {
        String[] columns = header.split(",");
        Map<String, Integer> indices = new HashMap<>();
        
        for (int i = 0; i < columns.length; i++) {
            String column = columns[i].trim();
            switch (column) {
                case "Src IP":
                    indices.put("sourceIP", i);
                    break;
                case "Dst IP":
                    indices.put("destIP", i);
                    break;
                case "Flow Duration":
                    indices.put("flowDuration", i);
                    break;
                case "Tot Fwd Pkts":
                    indices.put("forwardPackets", i);
                    break;
                case "Timestamp":
                    indices.put("timestamp", i);
                    break;
            }
        }

        validateColumns(indices);
        return indices;
    }

    private void validateColumns(Map<String, Integer> indices) {
        List<String> requiredColumns = Arrays.asList("sourceIP", "destIP", "flowDuration", 
                                                   "forwardPackets", "timestamp");
        for (String column : requiredColumns) {
            if (!indices.containsKey(column)) {
                throw new IllegalArgumentException("Required column missing: " + column);
            }
        }
    }

    private void processLine(String line, Map<String, Integer> columnIndices, 
                           Map<TrafficKey, TrafficSummary> summaries) {
        String[] values = line.split(",");
        
        try {
            // Extract values
            String sourceIP = values[columnIndices.get("sourceIP")].trim();
            String destIP = values[columnIndices.get("destIP")].trim();
            double flowDuration = Double.parseDouble(values[columnIndices.get("flowDuration")].trim());
            long forwardPackets = Long.parseLong(values[columnIndices.get("forwardPackets")].trim());
            LocalDate date = parseDate(values[columnIndices.get("timestamp")].trim());

            // Create or update summary
            TrafficKey key = new TrafficKey(sourceIP, destIP, date);
            TrafficSummary summary = summaries.computeIfAbsent(key,
                k -> new TrafficSummary(sourceIP, destIP, date));
            
            summary.totalFlowDuration += flowDuration;
            summary.totalForwardPackets += forwardPackets;

        } catch (Exception e) {
            System.err.println("Error processing line: " + line);
            System.err.println("Error details: " + e.getMessage());
        }
    }

    private LocalDate parseDate(String timestamp) {
        // Assuming timestamp format is "MM/dd/yyyy hh:mm:ss a"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
        return LocalDateTime.parse(timestamp, formatter).toLocalDate();
    }
}
