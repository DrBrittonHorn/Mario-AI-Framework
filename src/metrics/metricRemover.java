package metrics;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class metricRemover {

    private static final String[] LEVELS_TO_CLEAR = {
        /* "1","2","3","4","5","6","7","8","9","10","11","12","13","13modified","14","15",*/"all"
    };

    private static final String[] METRICS_TO_CLEAR = {"compressionDistance"};

    private static final Path INPUT_CSV  = Paths.get("src", "metrics", "mergedMetrics", "completedMetricsNormWO.csv");
    private static final Path OUTPUT_CSV = Paths.get("src", "metrics", "completedMetrics", "completedMetricsNormWOAll.csv");

    private static final Path WFC_DIR = Paths.get("levels", "WaveFunctionCollapse");

    public static void main(String[] args) throws IOException {
        if (!Files.exists(INPUT_CSV)) {
            System.err.println("Input CSV not found: " + INPUT_CSV);
            return;
        }

        Set<String> levelsSet = new HashSet<>();
        for (String s : LEVELS_TO_CLEAR) levelsSet.add(s.trim());

        List<String[]> rows = new ArrayList<>();
        String[] header;

        try (BufferedReader br = Files.newBufferedReader(INPUT_CSV)) {
            String headerLine = br.readLine();
            if (headerLine == null) { System.err.println("Empty CSV file"); return; }
            header = headerLine.split(",", -1);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                rows.add(line.split(",", -1));
            }
        }

        System.out.println("Loaded " + rows.size() + " rows from CSV");
        System.out.println("Levels to clear: " + Arrays.toString(LEVELS_TO_CLEAR));
        System.out.println("Metrics to clear: " + Arrays.toString(METRICS_TO_CLEAR));

        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < header.length; i++) colIndex.put(header[i], i);

        // sanity
        for (String c : new String[]{"level","M","N","seed"}) {
            if (!colIndex.containsKey(c)) { System.err.println("CSV missing: " + c); return; }
        }
        for (String m : METRICS_TO_CLEAR) {
            if (!colIndex.containsKey(m)) { System.err.println("CSV missing metric: " + m); return; }
        }

        // Cache: map each sizeFolder -> set of filenames in that folder
        Map<String, Set<String>> folderCache = new HashMap<>();

        int clearedCount = 0;

        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
            String[] row = rows.get(rowIdx);

            String level = safeCell(row, colIndex.get("level"));
            String M     = safeCell(row, colIndex.get("M"));
            String N     = safeCell(row, colIndex.get("N"));
            String seed  = safeCell(row, colIndex.get("seed"));

            if (!levelsSet.contains(level)) continue;

            // EARLY-OUT: if target metric already empty, skip I/O entirely
            int metricIdx = colIndex.get("compressionDistance");
            String currVal = row[metricIdx] == null ? "" : row[metricIdx].trim();
            if (currVal.isEmpty()) continue;

            // Build candidates (exactly your 4 patterns)
            String sizeFolder = M + "x" + N;
            String[] candidates = {
                String.format("tmp-lvl-%s-M%s-N%s-s-%s.txt", level, M, N, seed),
                String.format("tmp-lvl-%s-M%s-N%s-s%s.txt",   level, M, N, seed),
                String.format("tmp-%s-M%s-N%s-s-%s.txt",      level, M, N, seed),
                String.format("tmp-%s-M%s-N%s-s%s.txt",       level, M, N, seed)
            };

            // Look up or list folder once
            Set<String> names = folderCache.computeIfAbsent(sizeFolder, sf -> listFilenames(WFC_DIR.resolve(sf)));

            boolean anyExists = false;
            for (String name : candidates) {
                if (names.contains(name)) { anyExists = true; break; }
            }
            if (!anyExists) continue;

            // Clear
            row[metricIdx] = "";
            clearedCount++;
        }

        try (BufferedWriter bw = Files.newBufferedWriter(OUTPUT_CSV)) {
            bw.write(String.join(",", header));
            bw.newLine();
            for (String[] row : rows) {
                bw.write(String.join(",", row));
                bw.newLine();
            }
        }

        System.out.println("\n=== Summary ===");
        System.out.println("Total rows processed: " + rows.size());
        System.out.println("Folders scanned once: " + folderCache.size());
        System.out.println("Values cleared: " + clearedCount);
        System.out.println("Output written to: " + OUTPUT_CSV.toAbsolutePath());
    }

    private static Set<String> listFilenames(Path folder) {
        Set<String> names = new HashSet<>();
        if (!Files.isDirectory(folder)) return names;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder)) {
            for (Path p : ds) {
                if (Files.isRegularFile(p)) names.add(p.getFileName().toString());
            }
        } catch (IOException ignored) {}
        return names;
    }

    private static String safeCell(String[] row, int idx) {
        if (idx < 0 || idx >= row.length) return "";
        String v = row[idx];
        return v == null ? "" : v.trim();
    }
}
