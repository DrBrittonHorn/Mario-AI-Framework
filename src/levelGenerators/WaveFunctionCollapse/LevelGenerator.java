package levelGenerators.WaveFunctionCollapse;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;

import engine.core.MarioLevelGenerator;
import engine.core.MarioLevelModel;
import engine.core.MarioTimer;

public class LevelGenerator implements MarioLevelGenerator {
    private int sampleWidth = 0;
    private int sampleHeight = 0;
    private int windowHeight = 0;
    private int windowWidth = 0;
    private int outputWidth = 5;
    private int outputHeight = 7;
    private Map<Integer, int[][]> tileIdToMatrix = new HashMap<>();
    private Map<String, Integer> matrixToTileId = new HashMap<>();
    private int nextTileId = 0;
    private String fileName = "";
    private String folderName = "levels/original/";
    private int[][] origData;
    private int[][] tiles;
    private int[][] outputMatrix;
    private Map<Integer, Set<Integer>> wave = new HashMap<>();
    private Map<Integer, Map<Integer, Set<Integer>>> adjacencyMap = new HashMap<>();
    private Map<Integer, Integer> origTileCounts = new HashMap<>();
    private int up=0, down=2, left=3, right=1;

    private Random rnd;

    public LevelGenerator() {
        this("levels/original/", "lvl-1.txt");
    }

    public LevelGenerator(String sampleFolder) {
        this(sampleFolder, 10);
    }

    public LevelGenerator(String sampleFolder, int sampleWidth) {
        this.sampleWidth = sampleWidth;
        this.folderName = sampleFolder;
    }
    public LevelGenerator(String sampleFolder, String sampleFile) {
        this(sampleFolder, sampleFile, 1,1);
    }

    public LevelGenerator(String sampleFolder, String sampleFile, int windowHeight, int windowWidth) {
        this.fileName = sampleFile;
        this.folderName = sampleFolder;
        this.windowHeight = windowHeight;
        this.windowWidth = windowWidth;
    }

    private String getRandomLevel() {
        File[] listOfFiles = new File(folderName).listFiles();
        //List<String> lines = Files.readAllLines(listOfFiles[rnd.nextInt(listOfFiles.length)].toPath());
        List<String> lines;
        try {
            lines = Files.readAllLines(listOfFiles[0].toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        String result = "";
        for (int i = 0; i < lines.size(); i++) {
            result += lines.get(i) + "\n";
        }
        return result;
    }

    private String getLevel() {
        String content = "";
        try {
            content = new String(Files.readAllBytes(new File(folderName + fileName).toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    private void readSampleLevel() {
        String levelString;
        if (fileName.isBlank()) {
            levelString = getRandomLevel();
        } else {
            levelString = getLevel();
        }
        String[] lines = levelString.split("\n");
        sampleWidth = lines[0].length();
        sampleHeight = lines.length;
        origData = new int[sampleHeight][sampleWidth];
        tiles = new int[sampleHeight-windowHeight+1][sampleWidth-windowWidth+1];
        System.out.println("Sample level size: " + sampleHeight + "x" + sampleWidth);
        System.out.println("tile size: " + (sampleHeight-windowHeight+1) + "x" + (sampleWidth-windowWidth+1));
        for (int i = 0; i < sampleHeight; i++) {
            for (int j = 0; j < sampleWidth; j++) {
                origData[i][j] = lines[i].charAt(j);
                //if (origData[i][j] != 45) System.out.println("origData[" + i + "][" + j + "] = " + origData[i][j]);
            }
        }
    }

    private void calculateAdjacencies() {
        // Convert original data to tiles
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[0].length; j++) {
                int[][] matrix = getMatrix(i, j);
                String maxtrixString = matrixToString(matrix);
                if (!matrixToTileId.containsKey(maxtrixString)) {
                    matrixToTileId.put(maxtrixString, nextTileId);
                    tileIdToMatrix.put(nextTileId, matrix);
                    System.out.println("Tile ID: " + nextTileId + " at " + j + "," + i + " Matrix: " + maxtrixString);
                    nextTileId++;
                }
                tiles[i][j] = matrixToTileId.get(maxtrixString);
                origTileCounts.merge(tiles[i][j], 1, Integer::sum);
            }
        }

        // Calculate adjacencies
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[0].length; j++) {
                adjacencyMap.putIfAbsent(tiles[i][j], new HashMap<>());
                adjacencyMap.get(tiles[i][j]).putIfAbsent(up, new HashSet<>());
                adjacencyMap.get(tiles[i][j]).putIfAbsent(down, new HashSet<>());
                adjacencyMap.get(tiles[i][j]).putIfAbsent(left, new HashSet<>());
                adjacencyMap.get(tiles[i][j]).putIfAbsent(right, new HashSet<>());

                // up adjacency
                if (i > 0) {
                    adjacencyMap.get(tiles[i][j]).get(up).add(tiles[i - 1][j]);
                } else {
                    adjacencyMap.get(tiles[i][j]).get(up).add(-1); // Top edge: adjacency to "nothing"
                }
                if (i < tiles.length - 1) {
                    adjacencyMap.get(tiles[i][j]).get(down).add(tiles[i + 1][j]);
                } else {
                    adjacencyMap.get(tiles[i][j]).get(down).add(-1); // Bottom edge: adjacency to "nothing"
                }
                if (j > 0) {
                    adjacencyMap.get(tiles[i][j]).get(left).add(tiles[i][j - 1]);
                } else {
                    adjacencyMap.get(tiles[i][j]).get(left).add(-1); // Left edge: adjacency to "nothing"
                }
                if (j < tiles[0].length - 1) {
                    adjacencyMap.get(tiles[i][j]).get(right).add(tiles[i][j + 1]);
                } else {
                    adjacencyMap.get(tiles[i][j]).get(right).add(-1); // Right edge: adjacency to "nothing"
                }
            }
        }
    }

    // Get matrix for a given range
    private int[][] getMatrix(int y, int x) {
        int[][] matrix = new int[windowHeight][windowWidth];
        for (int i = 0; i < windowHeight; i++) {
            for (int j = 0; j < windowWidth; j++) {
                matrix[i][j] = origData[y + i][x + j];
            }
        }
        return matrix;
    }

    // Add a mapping between a matrix and a tile ID
    private String matrixToString(int[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (int[] row : matrix) {
            for (int cell : row) {
                sb.append(cell).append(",");
            }
            sb.append(";");
        }
        return sb.toString();
    }

    // Retrieve a matrix by tile ID
    private int[][] getMatrixByTileId(int tileId) {
        return tileIdToMatrix.get(tileId);
    }

    // Retrieve a tile ID by matrix
    private int getTileIdByMatrix(int[][] matrix) {
        String matrixKey = matrixToString(matrix);
        return matrixToTileId.getOrDefault(matrixKey, -1); // Return -1 if not found
    }

    private void printAllAdjacencies(){
        for (Map.Entry<Integer, Map<Integer, Set<Integer>>> entry : adjacencyMap.entrySet()) {
            System.out.print(entry.getKey() + ": ");
            for (Map.Entry<Integer, Set<Integer>> innerEntry : entry.getValue().entrySet()) {
                System.out.print(innerEntry.getKey() + " -> ");
                for (Integer value : innerEntry.getValue()) {
                    System.out.print(value + " ");
                }
            }
            System.out.println();
        }
    }

    private void printMatrixByTileId(int tileId) {
        System.out.println("Matrix for tile ID " + tileId + ":");
        int[][] matrix = getMatrixByTileId(tileId);
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println();
        }
    }

    @Override
    public String getGeneratedLevel(MarioLevelModel model, MarioTimer timer) {
        rnd = new Random();
        model.clearMap();
        outputMatrix = new int[outputHeight][outputWidth];
        // Initialize the output matrix with -1
        for (int i = 0; i < outputHeight; i++) {
            for (int j = 0; j < outputWidth; j++) {
                outputMatrix[i][j] = -1;
            }
        }
        readSampleLevel();
        calculateAdjacencies();
        for (Integer tileId : tileIdToMatrix.keySet()) {
            System.out.println("Tile ID: " + tileId);
            printMatrixByTileId(tileId);   
        }
        printAllAdjacencies();
        initializeWave();
        while (true) {
            // Observe
            if (!observe()) {
                break;
            }
            // Propagate
            propagate();
            // Ban
            ban();
            // Print the wave
            System.out.println("Wave after observation and propagation:");
            printWave();
        }
        /*for (int i = 0; i < model.getWidth() / sampleWidth; i++) {
            try {
                model.copyFromString(i * sampleWidth, 0, i * sampleWidth, 0, sampleWidth, model.getHeight(), this.getRandomLevel());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
        //runTiledModel(model);
        return model.getMap();
    }

    private void initializeWave() {
        for (int i = 0; i < outputHeight * outputWidth; i++) {
            wave.put(i, new HashSet<>());

            int x = i % outputWidth; // Column index
            int y = i / outputWidth; // Row index
            // System.out.println("i: " + i + " x: " + x + " y: " + y);

            // Check adjacency for each tile ID
            for (int tileId = 0; tileId < nextTileId; tileId++) {
                boolean valid = true;

                // Check adjacency to "nothing" for edge positions
                Map<Integer, Set<Integer>> currentTileData = adjacencyMap.get(tileId);
                // System.out.println("Tile ID: " + tileId + " Adjacency: " + currentTileData);
                // Correct adjacency checks for edge positions
                if (y == 0 && !adjacencyMap.get(tileId).get(up).contains(-1)) {
                    // System.out.println("Tile ID " + tileId + " not valid up (top row)");
                    valid = false; // Top row: must have a top adjacency to "nothing"
                }
                if (y == outputHeight - 1 && !adjacencyMap.get(tileId).get(down).contains(-1)) {
                    // System.out.println("Tile ID " + tileId + " not valid down (bottom row)");
                    valid = false; // Bottom row: must have a bottom adjacency to "nothing"
                }
                if (x == 0 && !adjacencyMap.get(tileId).get(left).contains(-1)) {
                    // System.out.println("Tile ID " + tileId + " not valid left (left column)");
                    valid = false; // Left column: must have a left adjacency to "nothing"
                }
                if (x == outputWidth - 1 && !adjacencyMap.get(tileId).get(right).contains(-1)) {
                    // System.out.println("Tile ID " + tileId + " not valid right (right column)");
                    valid = false; // Right column: must have a right adjacency to "nothing"
                }

                // Add tile to wave if valid
                if (valid) {
                    wave.get(i).add(tileId);
                }
            }
        }
        // Print the initial wave
        System.out.println("Initial wave:");
        printWave();
    }

    private boolean observe() {
        int nextIndex = chooseWaveIndexWithSmallestPossibilities();
        if (nextIndex == -1) {
            return false; // No more observations possible
        }
        for (int i = 0; i < outputHeight * outputWidth; i++) {
            if (outputMatrix[i / outputWidth][i % outputWidth] != -1) {
                continue; // Skip already observed positions
            }
            if (wave.get(i).size() == 1) {
                int x = i % outputWidth;
                int y = i / outputWidth;

                // Get the observed tile
                int observedTile = wave.get(i).iterator().next();

                outputMatrix[y][x] = observedTile;

                // Mark as observed
                return true;
            }
        }

        return false; // No more observations possible
    }

    private void propagate() {
        for (int i = 0; i < outputHeight * outputWidth; i++) {
            if (wave.get(i).size() == 1) {
                int x = i % outputWidth;
                int y = i / outputWidth;

                // Get the observed tile
                int observedTile = wave.get(i).iterator().next();

                // Update neighbors
                for (int direction : adjacencyMap.get(observedTile).keySet()) {
                    int nx = x, ny = y;

                    // Determine the neighboring position based on the direction
                    if (direction == up) ny--;
                    if (direction == down) ny++;
                    if (direction == left) nx--;
                    if (direction == right) nx++;

                    // Check if the neighboring position is within bounds
                    if (nx >= 0 && nx < outputWidth && ny >= 0 && ny < outputHeight) {
                        int neighborIndex = ny * outputWidth + nx;

                        // Get the valid tiles for the neighboring position
                        Set<Integer> validNeighbors = adjacencyMap.get(observedTile).get(direction);

                        // Remove invalid tiles from the wave at the neighboring position
                        wave.get(neighborIndex).removeIf(tile -> !validNeighbors.contains(tile));
                    }
                }
            }
        }
    }

    private void ban() {

    }

    private void printWave() {
        // Print the initial wave
        for (Integer prod : wave.keySet()) {
            if (prod % outputWidth == 0) {
                System.out.print("\n");
            }
            System.out.print((prod % outputWidth) + "," + (prod / outputWidth) + " = " + wave.get(prod));
        }
        System.out.println();
    }

    private int chooseWaveIndexWithSmallestPossibilities() {
        int minPossibilities = Integer.MAX_VALUE;
        int chosenIndex = -1;

        for (int i = 0; i < outputHeight * outputWidth; i++) {
            int possibilities = wave.get(i).size();

            // Skip already observed indices (size 0 or 1)
            if (possibilities == 0) {
                return -1; // Return 0 if any wave index has no possibilities
            }

        }

        return chosenIndex;
    }

    @Override
    public String getGeneratorName() {
        return "WFCLevelGenerator";
    }
}
