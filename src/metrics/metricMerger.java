package metrics;

import java.io.*;
import java.nio.file.*;
import java.util.*;


//MERGES ALL CSV FILES WITHIN "metricDataCSVs" to "mergedMetrics"
public class metricMerger {
    public static void main(String[] args) throws IOException {
        Path base    = Paths.get("src", "metrics");
        Path dataDir = base.resolve("metricDataCSVs");
        Path outDir  = base.resolve("mergedMetrics");

        Files.createDirectories(outDir);

        String[] metrics = {
            "completionPct",
            "compressionDistance",
            "editDistance",
            "densityMetric",
            "leniencyMetric",
            "linearityRSquared"
        };

        Map<String, Map<String,String>> records = new HashMap<>();

        for (String metric : metrics) {
            Path folder = dataDir.resolve(metric);
            if (!Files.isDirectory(folder)) {
                System.err.println("Missing folder: " + folder);
                continue;
            }
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder, "*.csv")) {
                for (Path csv : ds) {
                    try (BufferedReader br = Files.newBufferedReader(csv)) {
                        br.readLine(); 
                        String line;
                        while ((line = br.readLine()) != null && !line.isEmpty()) {
                            String[] p = line.split(",", -1);
                            if (p.length < 5) continue;
                            String key = p[0] + "_" + p[1] + "_" + p[2] + "_" + p[3];
                            Map<String,String> row = records.computeIfAbsent(key, k -> {
                                Map<String,String> m = new HashMap<>();
                                m.put("level", p[0]);
                                m.put("M",     p[1]);
                                m.put("N",     p[2]);
                                m.put("seed",  p[3]);
                                return m;
                            });
                            row.put(metric, p[4]);
                        }
                    }
                }
            }
        }

        Path outFile = outDir.resolve("mergedMetrics.csv");
        try (BufferedWriter bw = Files.newBufferedWriter(outFile)) {
            bw.write("level,M,N,seed");
            for (String m : metrics) bw.write("," + m);
            bw.newLine();

            List<Map<String,String>> rows = new ArrayList<>(records.values());
            rows.sort(
                Comparator.<Map<String,String>,String>comparing(r -> r.get("level"))
                          .thenComparingInt(r -> Integer.parseInt(r.get("M")))
                          .thenComparingInt(r -> Integer.parseInt(r.get("N")))
                          .thenComparing(r -> r.get("seed"))
            );

            for (Map<String,String> r : rows) {
                bw.write(String.join(",", r.get("level"), r.get("M"), r.get("N"), r.get("seed")));
                for (String m : metrics) {
                    bw.write(",");
                    bw.write(r.getOrDefault(m, ""));
                }
                bw.newLine();
            }
        }

        System.out.println("Merged CSV written to " + outFile.toAbsolutePath());
    }
}
