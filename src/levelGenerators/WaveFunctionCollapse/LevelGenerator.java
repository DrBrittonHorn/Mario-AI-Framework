package levelGenerators.WaveFunctionCollapse;

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

import engine.core.MarioLevelGenerator;
import engine.core.MarioLevelModel;
import engine.core.MarioTimer;

public class LevelGenerator implements MarioLevelGenerator {
    private int sampleWidth = 0;
    private int sampleHeight = 0;
    private int windowHeight = 0;
    private int windowWidth = 0;
    private Map<Integer, int[][]> tileIdToMatrix = new HashMap<>();
    private Map<String, Integer> matrixToTileId = new HashMap<>();
    private int nextTileId = 0;
    private String fileName = "";
    private String folderName = "levels/original/";
    private int[][] origData;
    private int[][] tiles;
    private Map<Integer, Map<Integer, Set<Integer>>> adjacencyMap = new HashMap<>();
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
        for (int i = 0; i <= sampleHeight - windowHeight; i++) {
            for (int j = 0; j <= sampleWidth - windowWidth; j++) {
                origData[i][j] = lines[i].charAt(j);
                //if (origData[i][j] != 45) System.out.println("origData[" + i + "][" + j + "] = " + origData[i][j]);
            }
        }
    }

    private void calculateAdjacencies() {
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[0].length; j++) {
                int[][] matrix = getMatrix(i, j);
                String maxtrixString = matrixToString(matrix);
                if (!matrixToTileId.containsKey(maxtrixString)) {
                    matrixToTileId.put(maxtrixString, nextTileId);
                    tileIdToMatrix.put(nextTileId, matrix);
                    nextTileId++;
                }
                tiles[i][j] = matrixToTileId.get(maxtrixString);
            }
        }
        for (int i = 0; i < sampleHeight; i++) {
            for (int j = 0; j < sampleWidth; j++) {
                adjacencyMap.putIfAbsent(tiles[i][j], new HashMap<Integer,Set<Integer>>());
                adjacencyMap.get(tiles[i][j]).putIfAbsent(up, new HashSet<Integer>());
                adjacencyMap.get(tiles[i][j]).putIfAbsent(down, new HashSet<Integer>());
                adjacencyMap.get(tiles[i][j]).putIfAbsent(left, new HashSet<Integer>());
                adjacencyMap.get(tiles[i][j]).putIfAbsent(right, new HashSet<Integer>());

                // up adjacency
                if (i > 0) {
                    adjacencyMap.get(tiles[i][j]).get(up).add(tiles[i - 1][j]);
                }
                // down adjacency
                if (i < sampleHeight - 1) {
                    adjacencyMap.get(tiles[i][j]).get(down).add(tiles[i + 1][j]);
                }
                // left adjacency
                if (j > 0) {
                    adjacencyMap.get(tiles[i][j]).get(left).add(tiles[i][j - 1]);
                }
                // right adjacency
                if (j < sampleWidth - 1) {
                    adjacencyMap.get(tiles[i][j]).get(right).add(tiles[i][j + 1]);
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

    @Override
    public String getGeneratedLevel(MarioLevelModel model, MarioTimer timer) {
        rnd = new Random();
        model.clearMap();
        readSampleLevel();
        calculateAdjacencies();
        printAllAdjacencies();
        /*for (int i = 0; i < model.getWidth() / sampleWidth; i++) {
            try {
                model.copyFromString(i * sampleWidth, 0, i * sampleWidth, 0, sampleWidth, model.getHeight(), this.getRandomLevel());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
        return model.getMap();
    }

    @Override
    public String getGeneratorName() {
        return "WFCLevelGenerator";
    }
}
