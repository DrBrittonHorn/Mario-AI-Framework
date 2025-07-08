package levelGenerators.ttWFC;
import java.util.Random;
import java.lang.Math;

abstract class Model
{
    protected boolean[][] wave;
    protected boolean[] groundAllowed;
    protected boolean[] topAllowed;
    protected boolean[] leftAllowed;
    protected boolean[] rightAllowed;
    protected int mPatternIndex, fPatternIndex, fPreobserveIndex, mPreobserveIndex;
    protected int[][][] propagator;
    int[][][] compatible;
    protected int[] observed;

    int[][] stack;
    int stacksize, observedSoFar;

    protected int MX, MY, T;
    protected int M, N;
    protected boolean periodic, ground;

    protected double[] weights;
    double[] weightLogWeights, distribution;

    protected int[] sumsOfOnes;
    double sumOfWeights, sumOfWeightLogWeights, startingEntropy;
    protected double[] sumsOfWeights, sumsOfWeightLogWeights, entropies;

    public enum Heuristic { Entropy, MRV, Scanline };
    Heuristic heuristic;

    protected Model(int width, int height, int M, int N, boolean periodic, Heuristic heuristic)
    {
        MX = width;
        MY = height;
        this.M = M;
        this.N = N;
        this.periodic = periodic;
        this.heuristic = heuristic;
    }

    void Init()
    {
        if (DEBUG) System.out.println("Initializing WFC model with " + MX + "x" + MY + 
            " grid, M=" + M + ", N=" + N + ", periodic=" + 
            periodic + ", heuristic=" + heuristic);
        wave = new boolean[MX * MY][];
        compatible = new int[wave.length][][];
        for (int i = 0; i < wave.length; i++)
        {
            wave[i] = new boolean[T];
            compatible[i] = new int[T][];
            for (int t = 0; t < T; t++) compatible[i][t] = new int[4];
        }
        distribution = new double[T];
        observed = new int[MX * MY];

        weightLogWeights = new double[T];
        sumOfWeights = 0;
        sumOfWeightLogWeights = 0;

        for (int t = 0; t < T; t++)
        {
            weightLogWeights[t] = weights[t] * Math.log(weights[t]);
            sumOfWeights += weights[t];
            sumOfWeightLogWeights += weightLogWeights[t];
        }

        startingEntropy = Math.log(sumOfWeights) - sumOfWeightLogWeights / sumOfWeights;

        sumsOfOnes = new int[MX * MY];
        sumsOfWeights = new double[MX * MY];
        sumsOfWeightLogWeights = new double[MX * MY];
        entropies = new double[MX * MY];

        stack = new int[wave.length * T][2];
        stacksize = 0;
    }

    public boolean Run(int seed, int limit)
    {
        if (wave == null) Init();

        Clear();
        // if (DEBUG) return false;

        if (!Propagate()) return false;

        Random random = new Random(seed);

        for (int l = 0; l < limit || limit < 0; l++)
        {
            int node = NextUnobservedNode(random);
            if (node >= 0)
            {
                Observe(node, random);
                boolean success = Propagate();
                if (DEBUG) dumpWave("after Observe/Propagate()");
                if (!success) return false;
            }
            else
            {
                for (int i = 0; i < wave.length; i++) for (int t = 0; t < T; t++) if (wave[i][t]) { observed[i] = t; break; }
                return true;
            }
        }

        return true;
    }

    int NextUnobservedNode(Random random)
    {
        if (heuristic == Heuristic.Scanline)
        {
            for (int i = observedSoFar; i < wave.length; i++)
            {
                if (!periodic && (i % MX >= MX || i / MX >= MY)) continue;
                if (sumsOfOnes[i] > 1)
                {
                    observedSoFar = i + 1;
                    return i;
                }
            }
            return -1;
        }

        double min = 1E+4;
        int argmin = -1;
        for (int i = 0; i < wave.length; i++)
        {
            if (!periodic && (i % MX >= MX || i / MX >= MY)) continue;
            int remainingValues = sumsOfOnes[i];
            double entropy = heuristic == Heuristic.Entropy ? entropies[i] : remainingValues;
            if (remainingValues > 1 && entropy <= min)
            {
                double noise = 1E-6 * random.nextDouble();
                if (entropy + noise < min)
                {
                    min = entropy + noise;
                    argmin = i;
                }
            }
        }
        return argmin;
    }

    void Observe(int node, Random random)
    {
        boolean[] w = wave[node];
        for (int t = 0; t < T; t++) distribution[t] = w[t] ? weights[t] : 0.0;
        double r1 = random.nextDouble();
        double r = Helper.Random(distribution, r1);
        if (DEBUG) System.out.println("Observe() → observing cell " + node + "(" + (node % MX) + "," + (node / MX) + ") with pattern " + r);
        for (int t = 0; t < T; t++) if (w[t] != (t == r)) Ban(node, t);
    }

    boolean Propagate()
    {
        while (stacksize > 0)
        {
            int[] entry = stack[stacksize - 1];
            int i1 = entry[0];
            int t1 = entry[1];
            stacksize--;

            int x1 = i1 % MX;
            int y1 = i1 / MX;

            for (int d = 0; d < 4; d++)
            {
                int x2 = x1 + dx[d];
                int y2 = y1 + dy[d];
                if (!periodic && (x2 < 0 || y2 < 0 || x2 >= MX || y2 >= MY)) continue;

                if (x2 < 0) x2 += MX;
                else if (x2 >= MX) x2 -= MX;
                if (y2 < 0) y2 += MY;
                else if (y2 >= MY) y2 -= MY;

                int i2 = x2 + y2 * MX;
                int[] p = propagator[d][t1];
                int[][] compat = compatible[i2];

                for (int l = 0; l < p.length; l++)
                {
                    int t2 = p[l];
                    int[] comp = compat[t2];

                    comp[d]--;
                    if (comp[d] == 0) Ban(i2, t2);
                    //if (DEBUG) System.out.println("after propagate ban call cell (" + (x2) + "," + (y2) + ") with pattern " + t2 + " because of (" + (x1) + "," + (y1) + ") with pattern " + t1);
                }
            }
            for (int i = 0; i < sumsOfOnes.length; i++) {
                if (sumsOfOnes[i] == 0) {
                    contradiction(i);
                    return false;
                }
            }
        }

        for (int i = 0; i < sumsOfOnes.length; i++) {
            if (sumsOfOnes[i] == 0) {
                contradiction(i);
                return false;
            }
        }
        return true;
    }

    void Ban(int i, int t)
    {
        if (!wave[i][t]) return;  
        // if (DEBUG) {
        //     System.out.printf("Ban() → removing pattern %d at cell (%d,%d) [index %d],  was %d possibilities%n", t, i % MX, i / MX, i, sumsOfOnes[i] );
        // }
    
        wave[i][t] = false;

        int[] comp = compatible[i][t];
        for (int d = 0; d < 4; d++) comp[d] = 0;
        stack[stacksize] = new int[]{ i, t };
        stacksize++;

        sumsOfOnes[i] -= 1;
        sumsOfWeights[i] -= weights[t];
        sumsOfWeightLogWeights[i] -= weightLogWeights[t];

        double sum = sumsOfWeights[i];
        entropies[i] = Math.log(sum) - sumsOfWeightLogWeights[i] / sum;
        
    }

    void Clear()
    {
        
        for (int i = 0; i < wave.length; i++)
        {
            for (int t = 0; t < T; t++)
            {
                wave[i][t] = true;
                for (int d = 0; d < 4; d++) compatible[i][t][d] = propagator[opposite[d]][t].length;
            }

            sumsOfOnes[i] = weights.length;
            sumsOfWeights[i] = sumOfWeights;
            sumsOfWeightLogWeights[i] = sumOfWeightLogWeights;
            entropies[i] = startingEntropy;
            observed[i] = -1;
        }

        if (DEBUG) dumpWave("before Clear()");
        observedSoFar = 0;

        // //FORCE MARIO AND FINISH
        // System.out.println("Preobserving Mario and Finish patterns at cell (" + (mPreobserveIndex % MX) + "," + (mPreobserveIndex / MX) + ") and (" + (fPreobserveIndex % MX) + "," + (fPreobserveIndex / MX) + ")");
        // System.out.println();
        for (int t = 0; t < T; t++) if (t != mPatternIndex) Ban(mPreobserveIndex, t);
        for (int t = 0; t < T; t++) if (t != fPatternIndex) Ban(fPreobserveIndex, t);
        // System.out.println("********* POST BAN *********");
        Propagate();
        if (DEBUG) dumpCompleteWave("after m/f propagate");

        //BAN THEM EVERYWHERE ELSE
        for (int i = 0; i < MX*MY; i++) if (i != mPreobserveIndex) Ban(i, mPatternIndex);
        for (int i = 0; i < MX*MY; i++) if (i != fPreobserveIndex) Ban(i, fPatternIndex);
        Propagate();
        if (DEBUG) dumpWave("after else m/f propagate");
        //if (DEBUG) return;
        if (ground) {
            //ground, top, left, right edge bans
            for (int x = 0; x < MX; x++) for (int t = 0; t < T; t++) if (!groundAllowed[t]) Ban(x + 0 * MX, t);
            for (int x = 0; x < MX; x++) for (int t = 0; t < T; t++) if (!topAllowed[t]) Ban(x + (MY - 1) * MX, t);
            for (int y = 0; y < MY; y++) for (int t = 0; t < T; t++) if (!leftAllowed[t]) Ban(0 + y * MX, t);
            for (int y = 0; y < MY; y++) for (int t = 0; t < T; t++) if (!rightAllowed[t]) Ban((MX - 1) + y * MX, t);
            if (DEBUG) dumpWave("before ground propagate");
            Propagate();
            
            /* for each location,
             * for each direction,
             * for each tile,
                * if tile has no neighbor in that direction AND direction is in bounds
                * ban tile from that location
             */
            for (int y = 0; y < MY; y++) {
                for (int x = 0; x < MX; x++) {
                    int idx = x + y * MX;
                    for (int d = 0; d < 4; d++) {
                        int nx = x + dx[d];
                        int ny = y + dy[d];
                        int nidx = nx + ny * MX;
                        if (nidx < 0 || nidx >= MX * MY || nx < 0 || ny < 0 || nx >= MX || ny >= MY) continue;
                        for (int t = 0; t < T; t++) {
                            if (propagator[d][t].length == 0 && wave[idx][t]) {
                                if (DEBUG) System.out.printf("Banning tile %d at (%d,%d) because no neighbor in direction %d%n", t, x, y, d);
                                Ban(idx, t);
                            }
                        }
                    }
                }
            }
        }
        if (DEBUG) dumpWave("after Clear()");
    }

    public abstract void Save(String filename);

    protected static int[] dx = { -1, 0, 1, 0 };
    protected static int[] dy = { 0, 1, 0, -1 };
    static int[] opposite = { 2, 3, 0, 1 };
    private static final boolean DEBUG = false;


    private void dumpWave(String title) {
        if (!DEBUG) return;
        System.out.println("\n── " + title + " ──");
        for (int y = 0; y < MY; y++) {
            for (int x = 0; x < MX; x++) {
                int idx = x + y * MX;
                if (sumsOfOnes[idx] < 5) {
                    System.out.print("[");
                    for (int t = 0; t < T; t++) {
                        if (wave[idx][t]) {
                            System.out.print(" " + t + " ");
                        }
                    }
                    System.out.print("]");
                } else {
                    System.out.printf("%3d", sumsOfOnes[idx]); 
                }
            }
            System.out.println();
        }
    }

private void dumpCompleteWave(String title) {
        if (!DEBUG) return;
        System.out.println("\n── " + title + " ──");
        for (int y = 0; y < MY; y++) {
            for (int x = 0; x < MX; x++) {
                int idx = x + y * MX;
                System.out.print("[");
                for (int t = 0; t < T; t++) {
                    if (wave[idx][t]) {
                        System.out.print(" " + t + " ");
                    }
                }
                System.out.print("]");
            }
            System.out.println();
        }
    }

    private void dumpCell(int idx) {
        if (!DEBUG) return;
        System.out.printf("cell (%d,%d)  remaining=%d  allowed: ",
                idx % MX, idx / MX, sumsOfOnes[idx]);
        for (int t = 0; t < T; t++) if (wave[idx][t]) System.out.print(t + " ");
        System.out.println();
    }

    private void contradiction(int idx) {
        System.out.println("\n‼ CONTRADICTION at (" + (idx % MX) + "," + (idx / MX) + ")");
        dumpCell(idx);
    }

}
