package metrics;

import agents.MFFAgentAdapter;
import agents.robinBaumgarten.Agent;
import engine.core.MarioGame;
import engine.core.MarioLevelModel;
import engine.core.MarioResult;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.awt.geom.Point2D;

public class metricFiller {

    private static final Path WFC_DIR = Paths.get("levels", "WaveFunctionCollapse");
    private static final Path ORIGINAL_DIR = Paths.get("levels", "original");
    private static final Path METRICS_DIR = Paths.get("src", "metrics", "completedMetrics");
    private static final Path INPUT_CSV = METRICS_DIR.resolve("completedMetricsNormWithAll.csv");
    private static final Path OUTPUT_CSV = METRICS_DIR.resolve("completedMetricsNormWithAllDistanceMetric.csv");

    private static final Set<Character> PLATFORM_TILES = Set.of('X','#','S','C','L','U','@','!','2','1','D','t','T','%');
    private static final Set<Character> ENEMY_TILES = Set.of('g','G','r','R','k','K','y','Y', '|', 'o', '*');

    private static boolean isEmpty(char ch) {
        return ch == '-' || ch=='M' || ch=='F' || ENEMY_TILES.contains(ch);
    }

    private static boolean isPlatformTile(char ch) {
        return PLATFORM_TILES.contains(ch);
    }

    // Number of parallel threads - adjust based on your CPU cores
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    public static void main(String[] args) throws IOException, InterruptedException {
        if (!Files.exists(INPUT_CSV)) {
            System.err.println("Input CSV not found: " + INPUT_CSV);
            return;
        }

        List<String[]> rows = new ArrayList<>();
        String[] header;

        try (BufferedReader br = Files.newBufferedReader(INPUT_CSV)) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                System.err.println("Empty CSV file");
                return;
            }
            header = headerLine.split(",", -1);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] rowData = line.split(",", -1);

                if (rowData.length < header.length) {
                    String[] paddedRow = new String[header.length];
                    System.arraycopy(rowData, 0, paddedRow, 0, rowData.length);
                    for (int i = rowData.length; i < header.length; i++) {
                        paddedRow[i] = "";
                    }
                    rows.add(paddedRow);
                } else {
                    rows.add(rowData);
                }
            }
        }

        System.out.println("Loaded " + rows.size() + " rows from CSV");
        System.out.println("Using " + NUM_THREADS + " parallel threads");

        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            colIndex.put(header[i], i);
        }

        String[] metrics = {"completionPct", "compressionDistance", "editDistance",
                           "densityMetric", "leniencyMetric", "linearityRSquared", "completionPctAstarDistanceMetric"};

        // Prepare tasks for rows that need processing
        List<RowTask> tasks = new ArrayList<>();
        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
            String[] row = rows.get(rowIdx);

            String level = row[colIndex.get("level")];
            String M = row[colIndex.get("M")];
            String N = row[colIndex.get("N")];
            String seed = row[colIndex.get("seed")];

            List<String> missingMetrics = new ArrayList<>();
            for (String metric : metrics) {
                int idx = colIndex.get(metric);
                String value = row[idx];
                if (value == null || value.trim().isEmpty() || value.equals("NaN") || value.equals("null")) {
                    missingMetrics.add(metric);
                }
            }

            if (missingMetrics.isEmpty()) {
                
                continue;
            }

            String sizeFolder = M + "x" + N;
            Path levelPath = null;

            String[] patterns = {
                String.format("tmp-lvl-%s-M%s-N%s-s-%s.txt", level, M, N, seed),
                String.format("tmp-lvl-%s-M%s-N%s-s%s.txt", level, M, N, seed),
                String.format("tmp-%s-M%s-N%s-s-%s.txt", level, M, N, seed),
                String.format("tmp-%s-M%s-N%s-s%s.txt", level, M, N, seed)
            };

            for (String pattern : patterns) {
                Path testPath = WFC_DIR.resolve(sizeFolder).resolve(pattern);
                if (Files.exists(testPath)) {
                    levelPath = testPath;
                    break;
                }
            }

            if (levelPath == null) {
                System.err.println("WARNING: Level file not found for row " + (rowIdx + 1));
                continue;
            }

            Path originalPath;
            if ("all".equals(level)) {
                originalPath = ORIGINAL_DIR.resolve("lvl-1.txt");
            } else {
                originalPath = ORIGINAL_DIR.resolve("lvl-" + level + ".txt");
            }

            if (!Files.exists(originalPath)) {
                System.err.println("WARNING: Original level not found: " + originalPath);
                continue;
            }

            tasks.add(new RowTask(rowIdx, row, levelPath, originalPath, missingMetrics, colIndex));
        }

        System.out.println("Processing " + tasks.size() + " rows with missing metrics...\n");

        // Process rows in parallel
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        AtomicInteger filledCount = new AtomicInteger(0);
        AtomicInteger completedRows = new AtomicInteger(0);
        int totalTasks = tasks.size();

        List<Future<?>> futures = new ArrayList<>();
        for (RowTask task : tasks) {
            futures.add(executor.submit(() -> {
                for (String metric : task.missingMetrics) {
                    try {
                        String value = computeMetric(metric, task.levelPath, task.originalPath);
                        synchronized (task.row) {
                            task.row[task.colIndex.get(metric)] = value;
                        }
                        filledCount.incrementAndGet();
                    } catch (Exception e) {
                        System.err.printf("ERROR row %d, %s: %s%n", task.rowIdx + 1, metric, e.getMessage());
                    }
                }
                int done = completedRows.incrementAndGet();
                System.out.printf("Completed row %d (%d/%d)%n", task.rowIdx + 1, done, totalTasks);
            }));
        }

        // Wait for all tasks to complete
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (ExecutionException e) {
                System.err.println("Task failed: " + e.getCause().getMessage());
            }
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);

        // Write output
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
        System.out.println("Metrics filled: " + filledCount.get());
        System.out.println("Output written to: " + OUTPUT_CSV.toAbsolutePath());
    }

    // Helper class to hold row processing data
    private static class RowTask {
        final int rowIdx;
        final String[] row;
        final Path levelPath;
        final Path originalPath;
        final List<String> missingMetrics;
        final Map<String, Integer> colIndex;

        RowTask(int rowIdx, String[] row, Path levelPath, Path originalPath,
                List<String> missingMetrics, Map<String, Integer> colIndex) {
            this.rowIdx = rowIdx;
            this.row = row;
            this.levelPath = levelPath;
            this.originalPath = originalPath;
            this.missingMetrics = missingMetrics;
            this.colIndex = colIndex;
        }
    }

    private static String computeMetric(String metric, Path levelPath, Path originalPath) throws IOException {
        switch (metric) {
            case "completionPct":
                return String.format("%.2f", runAgentOnLevel(levelPath));
            case "completionPctAstarDistanceMetric":
                return String.format("%.2f", runAgentOnLevelAstar(levelPath));
            case "compressionDistance":
                return String.format("%.6f", runCompression(levelPath, originalPath));
            case "editDistance":
                return Integer.toString(runEditDistance(levelPath, originalPath));
            case "densityMetric":
                return String.format("%.4f", runDensityMetric(levelPath));
            case "leniencyMetric":
                return String.format("%.4f", runLeniency(levelPath));
            case "linearityRSquared":
                double rSquared = runLinearity(levelPath);
                return Double.isNaN(rSquared) ? "NaN" : String.format("%.4f", rSquared);
            default:
                throw new IllegalArgumentException("Unknown metric: " + metric);
        }
    }

    private static double runCompression(Path genPath, Path origPath) throws IOException {
        byte[] x = canonicalizeToUtf8Bytes(Files.readAllLines(genPath));
        byte[] y = canonicalizeToUtf8Bytes(Files.readAllLines(origPath));

        int Cx = compress(x).length;
        int Cy = compress(y).length;

        byte[] sep = "\n§§SEP§§\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        byte[] xy = new byte[x.length + sep.length + y.length];
        System.arraycopy(x, 0, xy, 0, x.length);
        System.arraycopy(sep, 0, xy, x.length, sep.length);
        System.arraycopy(y, 0, xy, x.length + sep.length, y.length);

        byte[] yx = new byte[y.length + sep.length + x.length];
        System.arraycopy(y, 0, yx, 0, y.length);
        System.arraycopy(sep, 0, yx, y.length, sep.length);
        System.arraycopy(x, 0, yx, y.length + sep.length, x.length);

        int Cxy = compress(xy).length;
        int Cyx = compress(yx).length;
        int Cconcat = Math.min(Cxy, Cyx);

        int min = Math.min(Cx, Cy);
        int max = Math.max(Cx, Cy);
        if (max == 0) return Double.NaN;

        double ncd = (Cconcat - min) / (double) max;
        if (ncd < 0) ncd = 0.0;
        return ncd;
    }

    private static byte[] canonicalizeToUtf8Bytes(List<String> lines) {
        String joined = String.join("\n", lines);
        return joined.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }



    public static int runEditDistance(Path genPath, Path origPath) throws IOException {
        List<String> genLines  = Files.readAllLines(genPath);
        List<String> origLines = Files.readAllLines(origPath);
        String gen  = String.join("\n", genLines);
        String orig = String.join("\n", origLines);
        return editDistance(gen, orig);
    }

    public static double runAgentOnLevel(Path genPath) throws IOException {
        List<String> lines = Files.readAllLines(genPath);
        int height = lines.size();
        int width  = lines.get(0).length();

        MarioLevelModel model = new MarioLevelModel(width, height);
        model.copyFromString(String.join("\n", lines));

        MarioGame game = new MarioGame();
        MarioResult res = game.runGame(new Agent(), model.getMap(),20,0,false);

        return res.getCompletionPercentage();
    }
    public static double runAgentOnLevelAstar(Path genPath) throws IOException {
        List<String> lines = Files.readAllLines(genPath);
        int height = lines.size();
        int width  = lines.get(0).length();

        MarioLevelModel model = new MarioLevelModel(width, height);
        model.copyFromString(String.join("\n", lines));

        MarioGame game = new MarioGame();
        MarioResult res = game.runGame(new MFFAgentAdapter(new mff.agents.astarDistanceMetric.Agent()), model.getMap(),20,0,false);

        return res.getCompletionPercentage();
    }

    private static int editDistance(String s1, String s2) {
        int n = s1.length(), m = s2.length();
        int[][] dp = new int[n+1][m+1];

        for (int i = 0; i <= n; i++) dp[i][0] = i;
        for (int j = 0; j <= m; j++) dp[0][j] = j;

        for (int i = 1; i <= n; i++) {
            char a = s1.charAt(i-1);
            for (int j = 1; j <= m; j++) {
                char b = s2.charAt(j-1);
                int cost = (a == b) ? 0 : 1;
                dp[i][j] = Math.min( Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1), dp[i-1][j-1] + cost );
            }
        }
        return dp[n][m];
    }

    private static byte[] compress(byte[] data) throws IOException {
        Deflater compressor = new Deflater(Deflater.BEST_COMPRESSION);
        compressor.setInput(data);
        compressor.finish();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length)) {
            byte[] buffer = new byte[1024];
            while (!compressor.finished()) {
                int count = compressor.deflate(buffer);
                bos.write(buffer, 0, count);
            }
            return bos.toByteArray();
        } finally {
            compressor.end();
        }
    }

    public static double runLinearity(Path lvlPath) throws IOException {
        List<String> lines = Files.readAllLines(lvlPath);
        List<Point2D.Double> pts = collectPlatformCenters(lines);

        int n = pts.size();
        if (n < 2) return Double.NaN;

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (Point2D.Double p : pts) {
            sumX  += p.x;
            sumY  += p.y;
            sumXY += p.x * p.y;
            sumX2 += p.x * p.x;
        }
        double denom = n * sumX2 - sumX * sumX;
        if (denom == 0) return Double.NaN;

        double m = (n * sumXY - sumX * sumY) / denom;
        double b = (sumY - m * sumX) / n;

        double yBar  = sumY / n;
        double ssRes = 0, ssTot = 0;
        for (Point2D.Double p : pts) {
            double yFit = m * p.x + b;
            ssRes += (p.y - yFit) * (p.y - yFit);
            ssTot += (p.y - yBar) * (p.y - yBar);
        }
        if (ssTot == 0) return Double.NaN;

        return 1.0 - ssRes / ssTot;
    }

    private static List<Point2D.Double> collectPlatformCenters(List<String> lines) {
        int rows = lines.size();
        int cols = lines.get(0).length();
        List<Point2D.Double> pts = new ArrayList<>();

        for (int fileRow = 0; fileRow < rows; fileRow++) {
            String row      = lines.get(fileRow);
            String rowAbove = (fileRow == 0) ? null : lines.get(fileRow - 1);
            if(rowAbove == null) continue;

            int col = 0;
            while (col < cols) {
                boolean qualifies = isPlatformTile(row.charAt(col)) && (isEmpty(rowAbove.charAt(col)));
                if (!qualifies) { col++; continue; }

                int start = col;
                while (col < cols) {
                    char c = row.charAt(col);
                    boolean ok = isPlatformTile(c) && (isEmpty(rowAbove.charAt(col)));
                    if (!ok) break;
                    col++;
                }
                int end = col - 1;
                double xMid = (start + end) / 2.0;
                double yVal = (rows - 1 - fileRow);
                pts.add(new Point2D.Double(xMid, yVal));
            }
        }
        return pts;
    }

    public static double runDensityMetric(Path lvlPath) throws IOException {
        List<String> lines = Files.readAllLines(lvlPath);
        if (lines.isEmpty()) return 0.0;

        int rows = lines.size();
        int cols = lines.get(0).length();
        int[] platformCounts = new int[cols];

        for (int y = 1; y < rows; y++) {
            String row = lines.get(y);
            String rowAbove = lines.get(y - 1);

            for (int x = 0; x < cols; x++) {
                char tile = row.charAt(x);
                boolean isStandable = isPlatformTile(tile) && isEmpty(rowAbove.charAt(x));
                if (isStandable) {
                    platformCounts[x]++;
                }
            }
        }

        double sum = 0;
        for (int count : platformCounts) {
            sum += count;
        }
        return sum / cols;
    }

    public static double runLeniency(Path lvlPath) throws IOException {
        List<String> lines = Files.readAllLines(lvlPath);

        int    gaps    = countLevelGaps(lines);
        double avgGapW = averageLevelGapWidth(lines);
        int    enemies = countLevelEnemies(lines);
        int    pipes   = countLevelPipes(lines);

        return gaps   * -0.5
             + avgGapW * -1.0
             + enemies * -1.0
             + pipes   * -0.5;
    }

    private static List<Integer> collectAllGapWidths(List<String> lines) {
        List<Integer> gapWidths = new ArrayList<>();
        int rows = lines.size();
        if (rows == 0) return gapWidths;
        int cols = lines.get(0).length();

        for (int y = 0; y < rows; y++) {
            String row      = lines.get(y);
            String rowAbove = (y == 0) ? null : lines.get(y - 1);

            List<int[]> segments = new ArrayList<>();
            boolean inPlat = false;
            int start = 0;
            for (int x = 0; x < cols; x++) {
                boolean qualifies = isPlatformTile(row.charAt(x)) && (isEmpty(rowAbove.charAt(x)));
                if (qualifies) {
                    if (!inPlat) {
                        inPlat = true;
                        start = x;
                    }
                } else if (inPlat) {
                    segments.add(new int[]{ start, x - 1 });
                    inPlat = false;
                }
            }
            if (inPlat) {
                segments.add(new int[]{ start, cols - 1 });
            }

            for (int i = 0; i + 1 < segments.size(); i++) {
                int endOfFirst  = segments.get(i)[1];
                int startOfNext = segments.get(i + 1)[0];
                int gapWidth    = startOfNext - (endOfFirst + 1);
                if (gapWidth > 0) {
                    gapWidths.add(gapWidth);
                }
            }
        }
        return gapWidths;
    }

    private static int countLevelGaps(List<String> lines) {
        return collectAllGapWidths(lines).size();
    }

    private static double averageLevelGapWidth(List<String> lines) {
        List<Integer> widths = collectAllGapWidths(lines);
        if (widths.isEmpty()) return 0.0;
        int sum = widths.stream().mapToInt(Integer::intValue).sum();
        return sum / (double) widths.size();
    }

    private static int countLevelEnemies(List<String> lines) {
        Set<Character> enemies = Set.of('g','G','r','R','k','K','y','Y');
        int cnt = 0;
        for (String row : lines) {
            for (char c : row.toCharArray()) {
                if (enemies.contains(c)) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

    private static int countLevelPipes(List<String> lines) {
        Set<Character> pipes = Set.of('T', '*');
        int cnt = 0;
        for (String row : lines) {
            for (char c : row.toCharArray()) {
                if (pipes.contains(c)) {
                    cnt++;
                }
            }
        }
        return cnt;
    }
}
