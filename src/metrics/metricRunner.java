package metrics;
import agents.robinBaumgarten.Agent;
import engine.core.MarioGame;
import engine.core.MarioLevelModel;
import engine.core.MarioResult;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.*;
import java.util.zip.Deflater;

public class metricRunner {

    private static final Path WFC_DIR      = Paths.get("levels", "WaveFunctionCollapse");
    private static final Path ORIGINAL_DIR = Paths.get("levels", "original");
    // file pattern: tmp-lvl-<baseId>-M<M>-N<N>-s<seed>.txt
    private static final Pattern FILENAME = Pattern.compile(
        "tmp-lvl-(\\d+)-M(\\d+)-N(\\d+)-s-?(\\d+)\\.txt"
    );

    private static final String[] TARGET_SIZES  = { "1x1", /*"2x2", "3x3", */ "1x16", "6x6", "7x5", "14x6", "15x2" };
    private static final String[] TARGET_LEVELS = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15" };

    public static void main(String[] args) throws IOException {
        try (DirectoryStream<Path> sizes = Files.newDirectoryStream(WFC_DIR, Files::isDirectory)) {
            for (Path sizeDir : sizes) {
                String sizeName = sizeDir.getFileName().toString();
                if (!Arrays.asList(TARGET_SIZES).contains(sizeName)) continue;

                String[] mn = sizeName.split("x");
                int M = Integer.parseInt(mn[0]), N = Integer.parseInt(mn[1]);

                try (DirectoryStream<Path> levels = Files.newDirectoryStream(sizeDir, "*.txt")) {
                    for (Path lvlPath : levels) {
                        Matcher m = FILENAME.matcher(lvlPath.getFileName().toString());
                        if (!m.matches()) continue;

                        String baseLevelId = m.group(1);
                        if (!Arrays.asList(TARGET_LEVELS).contains(baseLevelId)) continue;

                        int seed = Integer.parseInt(m.group(4));
                        Path originalPath = ORIGINAL_DIR.resolve("lvl-" + baseLevelId + ".txt");
                        if (!Files.exists(originalPath)) {
                            System.err.println(" Missing original lvl-" + baseLevelId);
                            continue;
                        }

                        runAllMetrics(lvlPath, originalPath, M, N, baseLevelId, seed);
                    }
                }
            }
        }
    }

    private static void runAllMetrics(Path lvlPath, Path originalPath, int M, int N, String baseLevelId, int seed) {
        
        try {
            double dist = runCompression(lvlPath, originalPath);
            System.out.printf("   distance = %.6f%n", dist);
            writeCsvRecord(baseLevelId, M, N, seed, "compressionDistance", Double.toString(dist));
            int ed = runEditDistance(lvlPath, originalPath);
            System.out.printf("   editDistance = %d%n", ed);
            writeCsvRecord(baseLevelId, M, N, seed,"editDistance", Integer.toString(ed));
            double completionPct = runAgentOnLevel(lvlPath);
            System.out.printf("   agent completion = %.2f%%%n", completionPct);
            writeCsvRecord( baseLevelId, M, N, seed, "completionPct", String.format("%.2f", completionPct));
        } catch (IOException e) {
            System.err.println("failed: " + e.getMessage());
        }
    }

    private static double runCompression(Path genPath, Path origPath) throws IOException {
        byte[] genBytes  = Files.readAllBytes(genPath);
        byte[] origBytes = Files.readAllBytes(origPath);

        int compGen  = compress(genBytes).length;
        int compOrig = compress(origBytes).length;

        return Math.abs(compGen - compOrig) / ((compGen + compOrig) / 2.0);
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
        MarioResult res = game.runGame(new Agent(), model.getMap(),40,0,false);

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
    
    private static void writeCsvRecord(
            String level,
            int M,
            int N,
            int seed,
            String metricName,
            String metricValue
    ) throws IOException {

        Path metricDir = Paths.get("src","metrics", "metricOutputs", metricName);
        Files.createDirectories(metricDir);

        String fileName = String.format("lvl-%s_M%dx%d_%s.csv", level, M, N, metricName);
        Path csvPath = metricDir.resolve(fileName);

        boolean isNew = Files.notExists(csvPath);
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {

            if (isNew) {
                writer.write("level,M,N,seed," + metricName);
                writer.newLine();
            }
            writer.write(String.join(",", level, Integer.toString(M), Integer.toString(N), Integer.toString(seed), metricValue)); writer.newLine();
        }
    }
}
