package metrics;

import java.io.*;
import java.nio.file.*;
import java.util.*;

//NORMALIZES ALL METRICS WITHIN "mergedMetrics.csv" BASED ON "toNorm" ARRAY
public class metricNormalizer {
    public static void main(String[] args) throws IOException {
        Path base      = Paths.get("src", "metrics");
        Path mergedDir = base.resolve("mergedMetrics");
        Path inputFile = mergedDir.resolve("completedMetricsCOMPFixWOAll.csv");
        Path outputFile= mergedDir.resolve("completedMetricsNormWOAll.csv");

        //METRICS TO NORMALIZE
        List<String> toNorm = Arrays.asList("compressionDistance");

        List<String[]> rows = new ArrayList<>();
        String[] headerCols;
        try (BufferedReader br = Files.newBufferedReader(inputFile)) {
            String header = br.readLine();
            if (header == null) {
                System.err.println("Empty file: " + inputFile);
                return;
            }
            headerCols = header.split(",", -1);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                rows.add(line.split(",", -1));
            }
        }


        Map<String,Integer> idx = new HashMap<>();
        for (int i = 0; i < headerCols.length; i++) {
            idx.put(headerCols[i], i);
        }


        Map<String,Double> minMap = new HashMap<>();
        Map<String,Double> maxMap = new HashMap<>();
        for (String m : toNorm) {
            minMap.put(m, Double.POSITIVE_INFINITY);
            maxMap.put(m, Double.NEGATIVE_INFINITY);
        }
        for (String[] row : rows) {
            for (String m : toNorm) {
                int i = idx.get(m);
                String s = row[i];
                try {
                    double v = Double.parseDouble(s);
                    minMap.put(m, Math.min(minMap.get(m), v));
                    maxMap.put(m, Math.max(maxMap.get(m), v));
                } catch (NumberFormatException e) {

                }
            }
        }

        try (BufferedWriter bw = Files.newBufferedWriter(outputFile)) {
            bw.write(String.join(",", headerCols));
            bw.newLine();

            for (String[] row : rows) {
                String[] out = Arrays.copyOf(row, row.length);

                for (String m : toNorm) {
                    int i = idx.get(m);
                    double norm = 0.0;
                    try {
                        double v  = Double.parseDouble(row[i]);
                        double mn = minMap.get(m), mx = maxMap.get(m);
                        if (mx > mn) {
                            norm = (v - mn) / (mx - mn);
                        }
                    } catch (Exception e) {
                    }
                    out[i] = String.format("%.6f", norm);
                }

                bw.write(String.join(",", out));
                bw.newLine();
            }
        }

        System.out.println("Normalized CSV written to " + outputFile.toAbsolutePath());
    }
}
