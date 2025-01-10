package com.project.lambda;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummarizeWorker {

    // Logger for tracking errors and information
    private static final Logger logger = LoggerFactory.getLogger(SummarizeWorker.class);

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

    // Method to process the CSV file
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

    // Parse the header of the CSV file and return a map of column names to indices
    private Map<String, Integer> parseHeader(String header) {
        String[] columns = header.split(",");
        Map<String, Integer> indices = new HashMap<>();

        for (int i = 0; i < columns.length; i++) {
            String column = columns[i].trim();
            switch (column) {
                case "Src IP":  // Updated to match the column name in the CSV
                    indices.put("sourceIP", i);
                    break;
                case "Dst IP":  // Updated to match the column name in the CSV
                    indices.put("destIP", i);
                    break;
                case "Flow Duration":  // Updated to match the column name in the CSV
                    indices.put("flowDuration", i);
                    break;
                case "Tot Fwd Pkts":  // Updated to match the column name in the CSV
                    indices.put("forwardPackets", i);
                    break;
                case "Timestamp":  // Updated to match the column name in the CSV
                    indices.put("timestamp", i);
                    break;
            }
        }

        return indices;
    }

    // Process a single line of data
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
            logger.error("Error processing line: {}", line, e);
        }
    }

    // Parse the timestamp and return as a LocalDate
    private LocalDate parseDate(String timestamp) {
        // Updated format based on the CSV's timestamp format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
        return LocalDateTime.parse(timestamp, formatter).toLocalDate();
    }
}
