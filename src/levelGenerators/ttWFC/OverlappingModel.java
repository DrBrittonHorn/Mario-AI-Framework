package levelGenerators.ttWFC;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OverlappingModel extends Model
{
    
    List<char[]> tilePatterns;
    List<char[]> tiles;
    private int[] patternToSample;
    private final int sampleWidth;
    private final int sampleHeight;
    private final int[] tileSample; 
    private final List<List<Integer>> patternOccurrences = new ArrayList<>();
    private final int padCols;
    private final int padRows;
    public OverlappingModel(String name, int M, int N, int width, int height, boolean periodicInput, boolean periodic, int symmetry, boolean ground, Heuristic heuristic)
    throws IOException{
        
        super(width, height, M, N, periodic, heuristic);
        List<String> lines = Files.readAllLines(Paths.get("src/levelGenerators/ttWFC/samples/" + name + ".txt"));
        int origSX = lines.get(0).length();
        int origSY = lines.size();

        
        // ADD PADDING TO MAKE aINPUT FIT N
        int padCols = (M - (origSX % M)) % M;  
        int padRows = (N - (origSY % N)) % N; 
        // System.out.println("Original size: " + origSX + "x" + origSY);
        // System.out.println("Target size: " + width + "x" + height);
        // System.out.println("Padding to fit MxN: " + M + "x" + N);
        // System.out.println("Padding cols: " + padCols + ", padding rows: " + padRows);
        this.padCols = padCols;
        this.padRows = padRows;
        String emptyCharacter = "-";
        for (int i = 0; i < origSY; i++) {
            String row = lines.get(i);
            lines.set(i, row + emptyCharacter.repeat(padCols));
        }

        String fullPad = emptyCharacter.repeat(origSX + padCols);
        for (int i = 0; i < padRows; i++) {
            lines.add(fullPad);
        }

        int SX = origSX + padCols;
        int SY = origSY + padRows;

        this.sampleWidth = SX;
        this.sampleHeight = SY;
        char[] charBitmap = new char[SX * SY];
        int tileCols = SX/M;
        int tileRows = SY/N;

        for (int y = 0; y < SY; y++) {
            String row = lines.get(SY - 1 - y);
            for (int x = 0; x < SX; x++) {
                charBitmap[x + y * SX] = row.charAt(x);
            }
        }
        
        int [] tileSample = new int[tileCols * tileRows];
        tiles = new ArrayList<>();

        for (int y = 0; y < tileRows; y++) {
            for (int x = 0; x < tileCols; x++) {
                char[] tile = tPatternBF(charBitmap, x*M, y*N, SX, SY, M, N);
                int k = 0;
                for(; k<tiles.size();k++) if (Arrays.equals(tiles.get(k), tile)) break;
                if (k==tiles.size()) tiles.add(tile);
                int flatInd = x + y * tileCols;
                tileSample[flatInd] = k;
                // System.out.printf("%2d ",k ); // print original input as tiles
            }
            // System.out.println();
        }
        
        this.tileSample = tileSample;
        tilePatterns = new ArrayList<>();

        Map<String, Integer> tpIndices = new HashMap<>();
        List<Double> weightList = new ArrayList<>();
        List<Integer> patternToSampleList = new ArrayList<>();
        int xmax = periodicInput ? SX : SX - M + 1;
        int ymax = periodicInput ? SY : SY - N + 1;

        for (int y = 0; y < ymax; y+=N) for (int x = 0; x < xmax; x+=M)
            {
                char[] rawTile = tPatternBF(charBitmap, x, y, SX, SY, M, N);

                    char[] p0 = rawTile;
                    char[] p1 = tReflect(p0, M, N);
                    char[] p2 = tRotate(p0, M, N);
                    char[] p3 = tReflect(p2, M, N);
                    char[] p4 = tRotate(p2, M, N);
                    char[] p5 = tReflect(p4, M, N);
                    char[] p6 = tRotate(p4, M, N);
                    char[] p7 = tReflect(p6, M, N);
                    char[][] variants = {p0,p1,p2, p3, p4, p5, p6, p7};


                for (int k = 0; k < symmetry; k++)
                {
                    String key = new String(variants[k]);
                    if (tpIndices.containsKey(key)) {
                        int idx = tpIndices.get(key);
                        weightList.set(idx, weightList.get(idx) + 1.0);
                        patternOccurrences.get(idx).add( x + y * tileCols );
                    } else {
                        int newIndex = weightList.size();
                        tpIndices.put(key, newIndex);
                        weightList.add(1.0);
                        tilePatterns.add(variants[k]); 
                        patternOccurrences.add(new ArrayList<>());
                        patternOccurrences.get(newIndex).add(x + y*tileCols);
                        patternToSampleList.add(tileSample[(x/M) + (y/N) * tileCols]);
                        
                    }
                }
            }
          
        // System.out.println("Printing tile patterns:");
        // System.out.println("Total patterns found: " + tilePatterns.size());
        // System.out.println("Total patterns in sample: " + tiles.size());
        for (int i = 0; i < tilePatterns.size(); i++) {
            char[] p = tilePatterns.get(i);
            // System.out.print("Pattern " + i + ":");
            for (int j = 0; j < p.length; j++) {
                System.out.print(p[j]);
            }
            System.out.println();
        }

        this.patternToSample = patternToSampleList.stream().mapToInt(i->i).toArray();
        weights = new double[weightList.size()];
        for (int i = 0; i < weightList.size(); i++) {
            weights[i] = weightList.get(i);
        }
        T = weights.length;
        this.ground = ground; 
        int mCell = -1, fCell = -1;
        for (int cell = 0; cell < tileSample.length; cell++) {
            int cx = cell % tileCols, cy = cell / tileCols;
            char[] block = tPatternBF(charBitmap, cx*M, cy*N, SX, SY, M, N);
            for (char c : block) {
                if (c == 'M') mCell = cell;
                if (c == 'F') fCell = cell;
            }
        }
        if (mCell < 0 || fCell < 0) {
            throw new IllegalStateException("Could not find both M and F in the sample!");
        }

        int sampleCols = sampleWidth  / M, outputCols = MX;
        int sampleRows = sampleHeight / N, outputRows = MY;

        int mCx = mCell % sampleCols, mCy = mCell / sampleCols;
        int fCx = fCell % sampleCols, fCy = fCell / sampleCols;


        int yOffset = outputRows - sampleRows;
        if (yOffset < 0) yOffset = 0;
        int mOutX = mCx, mOutY = yOffset + mCy;

        int fOutX = (outputCols - sampleCols) + fCx, fOutY = yOffset + fCy;

        this.mPreobserveIndex = mOutX + mOutY * MX;
        this.fPreobserveIndex = fOutX + fOutY * MX;

        int rawM = tileSample[mCell];
        int rawF = tileSample[fCell];

        mPatternIndex = -1;
        for (int t = 0; t < T; t++) {
            if (patternToSample[t] == rawM) { 
                mPatternIndex = t;
                break;
            }
        }
        if (mPatternIndex < 0) throw new IllegalStateException("Could not find the M-pattern!");

        fPatternIndex = -1;
        for (int t = 0; t < T; t++) {
            if (patternToSample[t] == rawF) { 
                fPatternIndex = t;
                break;
            }
        }
        if (fPatternIndex < 0) throw new IllegalStateException("Could not find the F-pattern!");
       
        groundAllowed = new boolean[T];
        topAllowed   = new boolean[T];
        leftAllowed  = new boolean[T];
        rightAllowed = new boolean[T];
        //boolean [][] allowed = new boolean[4][T];

        for (int idx = 0; idx < tileSample.length; idx++) {
            int x = idx % tileCols, y = idx / tileCols;
            int rawID = tileSample[idx];
            for (int p = 0; p < T; p++) if (patternToSample[p] == rawID) {
                for (int d = 0; d < 4; d++) {
                    int nx = x + dx[d], ny = y + dy[d];
                    if (nx < 0 || nx >= tileCols || ny < 0 || ny >= tileRows) {
                        //allowed[d][p] = true;
                        switch (d) {
                            case 0:
                                leftAllowed[p] = true;
                                break;
                            case 1:
                                topAllowed[p] = true;
                                break;
                            case 2:
                                rightAllowed[p] = true;
                                break;
                            case 3:
                                groundAllowed[p] = true;
                                break;
                        }
                    }
                }
            }
        }
        // printAllowed("ground", groundAllowed);
        // printAllowed("top", topAllowed);
        // printAllowed("left", leftAllowed);
        // printAllowed("right", rightAllowed);
        propagator = new int[4][][];
        for (int d = 0; d < 4; d++)
        {
            propagator[d] = new int[T][];
            for (int t = 0; t < T; t++)
            {
                List<Integer> list = new ArrayList<>();
                char[] p1 = tilePatterns.get(t);
                for (int t2 = 0; t2 < T; t2++) {
                    char[] p2 = tilePatterns.get(t2);

                    if (tAgrees( p1, p2, dx[d], dy[d],N ) ) 
                    {
                        list.add(t2);
                    }
                }
                int[] arr = new int[list.size()];
                for (int c= 0; c<arr.length; c++){
                    arr[c] = list.get(c);
                }
                propagator[d][t] = arr;
            }
        }

        // printPropagator();

    }
    @Override
    public void Save(String filename)
    {
        int tileCols = MX;   
        int tileRows = MY;   
        int outW = MX * M;
        int outH = MY * N;
        char[] charBitmap = new char[outW * outH];
        
        boolean fullyCollapsed = true;
        for (int i = 0; i < observed.length; i++) {
        if (observed[i] < 0) {
            fullyCollapsed = false;
            // break;
        }
        }
        if (fullyCollapsed)
        {
            for (int ty = 0; ty < tileRows; ty++)
            {
                for (int tx = 0; tx < tileCols; tx++)
                {
                    int tInd = tx + ty * tileCols;
                    int obs = observed[tInd];
                    if(obs < 0 || obs >=T) {
                        for(int tdy = 0; tdy < N; tdy++){
                            for (int tdx=0; tdx<M; tdx++){
                                int outX = tx * M + tdx;
                                int outY = ty * N + tdy;
                                charBitmap[outX + outY * outW] = ' ';
                            }
                        }
                        
                    } else {
                        char[]tile = tilePatterns.get(obs);
                        for(int tdy = 0; tdy < N; tdy++){
                            for (int tdx=0; tdx<M; tdx++){
                                int outX = tx * M + tdx;
                                int outY = ty * N + tdy;
                                charBitmap[outX + outY * outW] = tile[tdx + tdy * M];
                            }
                        }
                    }

                }
            }
        }
        else
        {
            for (int ty = 0; ty < tileRows; ty++) 
            {
                for (int tx = 0; tx < tileCols; tx++) 
                {
                    double[] freq = new double[T];

                    int cellInd = tx + ty * tileCols;
                    for(int t=0; t<T; t++){
                        if(wave[cellInd][t]){
                            freq[t] = weights[t];
                        }
                    }

                    int chosenTile = 0;
                    double totalWeight = 0.0;
                    for (int t = 0; t < T; t++) {
                        totalWeight += freq[t];
                    }

                    if (totalWeight > 0.0) {
                        chosenTile = Helper.Random(freq, Math.random());
                    }

                    char[] tile = tilePatterns.get(chosenTile);
                    for(int tdy = 0; tdy < N; tdy++){
                        for(int tdx = 0; tdx < M; tdx++){
                            int outX = tx * M + tdx;
                            int outY = ty * N + tdy;
                            charBitmap[outX + outY * outW] = tile[tdx + tdy * M];
                        }

                    }
                }
            }
        }

        int trimCols = padCols; 
        int trimRows = padRows; 

        int croppedW = outW - trimCols;
        int croppedH = outH - trimRows;
        char[] cropped = new char[croppedW * croppedH];

        for (int y = 0; y < croppedH; y++) {
        for (int x = 0; x < croppedW; x++) {
            int srcIndex = (y + trimRows) * outW + x;
            int dstIndex = y * croppedW + x;
            cropped[dstIndex] = charBitmap[srcIndex];
        }
        }

        charBitmap = cropped;
        outW    = croppedW;
        outH    = croppedH;
        char[] flipped = new char[outW * outH];
            for (int y = 0; y < outH; y++) {
                int srcRowIndex = y * outW;
                int dstRowIndex = (outH - 1 - y) * outW;
                System.arraycopy(charBitmap, srcRowIndex, flipped, dstRowIndex, outW);
            }

        try {
            Helper.saveBitmap(flipped, outW, outH, filename);
        }   catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean tAgrees(char[] p1, char[] p2, int dx, int dy, int N) {
        int idx1 = -1, idx2 = -1;
        for (int i = 0; i < tilePatterns.size(); i++) {
            if (Arrays.equals(tilePatterns.get(i), p1)) idx1 = i;
            if (Arrays.equals(tilePatterns.get(i), p2)) idx2 = i;
            if (idx1 >= 0 && idx2 >= 0) break;
        }
        if (idx1 < 0 || idx2 < 0) return false;

        int raw1 = patternToSample[idx1];
        int raw2 = patternToSample[idx2];
        int tileCols = sampleWidth / M;
        int tileRows = sampleHeight / N;

        for (int y = 0; y < tileRows; y++) {
            for (int x = 0; x < tileCols; x++) {
                if (tileSample[x + y*tileCols] != raw1) continue;
                int nx = x + dx, ny = y + dy;
                if (nx < 0 || nx >= tileCols || ny < 0 || ny >= tileRows) continue;
                if (tileSample[nx + ny*tileCols] == raw2) {
                    return true;
                }
            }
        }
        return false;
    }

    private static char[] tPatternBF(char[] sample, int x, int y, int SX, int SY, int M, int N) {
        char[] result = new char[M * N];
        for (int dy = 0; dy < N; dy++) {
            int sourceRowInSample = y + dy;  
            for (int dx = 0; dx < M; dx++) {
                int sx = x + dx;
                int sy = sourceRowInSample;
                result[ dx + dy * M] = sample[ sx + sy * SX ];
            }
        }
        return result;
    }

    private static char[] tRotate(char[] p, int M, int N) {
        if(M == N){
            char[] result = new char[N * N];
            for (int y = 0; y < N; y++) {
                for (int x = 0; x < N; x++) {
                    int xPrime = (N - 1 - y);
                    result[xPrime + x * N] = p[ x + y * N ];
                }   
            }
            return result;
        }else{
            return p;
        }
    }
    private static char[] tReflect(char[] p, int M, int N) {
        char[] result = new char[M * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < M; x++) {
                int xPrime = (M - 1 - x);
                result[xPrime + y * M] = p[x + y * M];
            }
        }
        return result;
    }
    // private static void printTile(char[] tile, int N) {
    //     for (int y = 0; y < N; y++) {
    //         for (int x = 0; x < N; x++) {
    //             System.out.print(tile[x + y * N]);
    //         }
    //         System.out.println();
    //     }
    // }
    private void printAllowed(String name, boolean[] allowed) {
        System.out.print(name + "Allowed patterns: ");
        for (int t = 0; t < allowed.length; t++) {
            if (allowed[t]) System.out.print(t + " ");
        }
        System.out.println();
    }

    private void printPropagator() {
        System.out.println("\n── Propagator ──");
        for (int t = 0; t < T; t++) {
            System.out.print("Pattern " + t + ": ");
            for (int d = 0; d < 4; d++) {
            String dirName;
            switch (d) {
                case 0:
                    dirName = "left";
                    break;
                case 1:
                    dirName = "top";
                    break;
                case 2:
                    dirName = "right";
                    break;
                case 3:
                    dirName = "ground";
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + d);
            }
                System.out.print(dirName + "[");
                if (propagator[d][t].length == 0) {
                    System.out.print(" ");
                } else {
                    for (int p : propagator[d][t]) {
                        System.out.print(p + " ");
                    }
                }
                System.out.print("] ");
            }
            System.out.println();
        }
    }

}
