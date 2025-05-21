package levelGenerators.quentinmorris;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.io.FileNotFoundException;

public class WFC {

    protected char[][] input;
    protected Preset[] presets;
    protected Preset[] bounds;
    protected char[] forcedFs;
    protected int outX, outY;
    protected int snapshotX, snapshotY;
    protected boolean screenWrap;

    public WFC(char[][] input, Preset[] presets, Preset[] bounds, char[] forcedFs,
                    int snapshotX, int snapshotY, int outX, int outY, boolean sw){
        this.input = input;
        this.presets = presets;
        this. bounds = bounds;
        this.forcedFs = forcedFs;

        this.snapshotX = snapshotX;
        this.snapshotY = snapshotY;
        this.outX = outX;
        this.outY = outY;

        this.screenWrap = sw;

    }

    Random random = new Random();

//----------------------------------
// Main Functions
//----------------------------------

    //main function
    public static void main( String[] args ) throws FileNotFoundException{

        // WFC jt = new WFC();
        // jt.startTest();
        
    }

    public Ret startTest(boolean noDuplicateMs) throws FileNotFoundException{

        // System.out.println("printing input image");
        // Utilities.PrintArray(input);

        while(observed == null){
            Run(0, presets, bounds, forcedFs);
        }

        //Utilities.printSnapshots(snapshots, input, snapshotX, snapshotY, false, false, screenWrap);

        int[] outFrequency = new int[noSnapshots];
        char[][] out = new char[outX][outY];
        for (int y = 0; y < outY; y++){
            for (int x = 0; x < outX; x++){ 
                outFrequency[observed[x + y * outX]] += 1;
                char c = (char)snapshots[observed[x + y * outX]].getSn().output();
                out[x][y] =  c;
            }
        }

        //snapshot frequencies vs output frequencies
        //0 = input image, 1 = output image
        int[][] frequencyCompare = new int[2][noSnapshots];
        for(int i = 0; i < noSnapshots; i++){

            frequencyCompare[0][i] = snapshots[i].getInfo().getFreqency();
            frequencyCompare[1][i] = outFrequency[i];
            //System.out.println("Snapshot "+i+": "+outFrequency[i]+","+snapshots[i].getInfo().getFreqency());

        }

        //add Mario if none
        out = Utilities.AddTile('M', out, outX, outY, false);

        //add Flag if none
        out = Utilities.AddTile('F', out, outX, outY, true);

        //get rid of extra Marios - takes the leftmost
        if(noDuplicateMs) out = Utilities.takeLeftmostTile('M', out, outX, outY);

        return new Ret(out, frequencyCompare);
        //return out;

    }

    class Ret {
        private char[][] image;
        private int[][] snapshotFrequs;
        protected Ret(char[][] i, int [][] s){
            this.image = i;
            this.snapshotFrequs = s;
        }
        char[][] getImage() {return image;}
        int[][] getFs() {return snapshotFrequs;}
    }

    Boolean Run(int limit, Preset[] ps, Preset[] bd, char[] ff){
        patternsFromSample(snapshotX,snapshotY);
        if (wave == null) buildPropogator(outX,outY);

        Clear();

        //forces presets
        RunForcedRules(ps);

        //loads occurrence counts of specified blocks
        LoadFrequency(ff);

        //forces specified bounded block placements
        RunBoundedRules(bd);

        //if screen-wrapping is off prepare the wave
        if(!screenWrap) PrepareNonWrapping();

        for(int l = 0; l < limit || limit == 0; l++){
            Boolean result = Observe(outX, outY);
            if(result != null){ 
                
                return result;
            }
            Propagate();
        }

        return true;
    }
    

//----------------------------------
// Canonical WFC Functions
//----------------------------------

    int noSnapshots, noSnapshotsUnsquashed;
    ArrayList<SnapAdjs> snapsToAdjs;
    HashMap<Snapshot, SnapInfo> snapshotInfo;
    SnapAndInfo[] snapshots;
    int[][][] propagator;
    void patternsFromSample(int snapshotX, int snapshotY){

        snapsToAdjs = new ArrayList<SnapAdjs>();

        //double loop through input and create snapshots
        if(screenWrap){ //with screen wrapping
            for(int i = 0; i < input.length; i++) for(int j = 0; j < input[i].length; j++){
                
                //creates snapshot from input image and adds it to 
                char[][] snapshot = new char[snapshotX][snapshotY];
                for(int x = 0; x < snapshotX; x++) for(int y = 0; y < snapshotY; y++){
                    int xVal = (x+i < input.length ? x+i : 0);
                    int yVal = (y+j < input[i].length ? y+j : 0);
                    snapshot[x][y] = input[xVal][yVal];
                }
                Snapshot sn = new Snapshot(snapshotX, snapshotY, snapshot);

                //finds four adjacent tiles to current tile at (i,j) - essentially just says whether or not it's on an edge
                Adjacency adjacents = 
                        new Adjacency((i != 0 ? new Tuple(i-1, j) : new Tuple(input.length-1,j)), //up
                                    (i != input.length-1 ? new Tuple(i+1,j) : new Tuple(0,j)), //down
                                    (j != 0 ? new Tuple(i, j-1) : new Tuple(i,input[i].length-1)),    //left
                                    (j != input[i].length-1 ? new Tuple(i, j+1) : new Tuple(i,0)));    //right

                //adds "adjacents" to arraylist of snap-adj parings
                SnapAdjs curPairing = new SnapAdjs(sn, adjacents);
                snapsToAdjs.add(curPairing);

            }
        }
        else{ //without screen wrapping

            for(int i = 0; i < input.length; i++) for(int j = 0; j < input[i].length; j++){

                //creates snapshot from input image and adds it to 
                char[][] snapshot = new char[snapshotX][snapshotY];
                for(int x = 0; x < snapshotX; x++) for(int y = 0; y < snapshotY; y++){
                    if(x+i >= input.length || y+j >= input[i].length) snapshot[x][y] = '.';
                    else snapshot[x][y] = input[x+i][y+j];
                }
                Snapshot sn = new Snapshot(snapshotX, snapshotY, snapshot);

                //finds four adjacent tiles to current tile at (i,j) - essentially just says whether or not it's on an edge
                Adjacency adjacents = 
                        new Adjacency((i != 0 ? new Tuple(i-1, j) : new Tuple(-1,-1)), //up
                                    (i != input.length-1 ? new Tuple(i+1,j) : new Tuple(-1,-1)), //down
                                    (j != 0 ? new Tuple(i, j-1) : new Tuple(-1,-1)),    //left
                                    (j != input[i].length-1 ? new Tuple(i, j+1) : new Tuple(-1,-1)));    //right
                
                //adds "adjacents" to arraylist of snap-adj parings
                SnapAdjs curPairing = new SnapAdjs(sn, adjacents);
                snapsToAdjs.add(curPairing);

            }
        }
        
        //squash duplicate snapshots, adjacencies, and count occurences
        snapshotInfo = new HashMap<Snapshot, SnapInfo>();
        for(SnapAdjs s : snapsToAdjs){
            ArrayList<Adjacency> curAdjList = new ArrayList<Adjacency>();
            curAdjList.add(s.getAdj());
            if( snapshotInfo.putIfAbsent(s.getSnapshot(), new SnapInfo(1,curAdjList)) != null){
                Snapshot curS = s.getSnapshot();
                SnapInfo curI = snapshotInfo.get(curS);
                curI.increment(1);
                curI.addAdj(s.getAdj());
                snapshotInfo.put(curS, curI);
            }
        }

        //count number of unique snapshots
        noSnapshots = snapshotInfo.size();
        snapshots = new SnapAndInfo[noSnapshots];
        int i = 0;
        for(Map.Entry<Snapshot, SnapInfo> set : snapshotInfo.entrySet()){
            snapshots[i] = new SnapAndInfo(set.getKey(), set.getValue());
            i++;
        }

        //init propagator
        propagator = new int[4][][];
        for (int d = 0; d < 4; d++)
        {
            propagator[d] = new int[noSnapshots][];
            for (int t = 0; t < noSnapshots; t++)
            {
                ArrayList<Integer> list = new ArrayList<Integer>();
                for (int t2 = 0; t2 < noSnapshots; t2++) if (agrees(snapshots[t], snapshots[t2], DX[d], DY[d], snapshotX, snapshotY)) list.add(t2);
                propagator[d][t] = new int[list.size()];
                for (int c = 0; c < list.size(); c++) propagator[d][t][c] = list.get(c);
            }
        }
    }

    Boolean agrees(SnapAndInfo s1, SnapAndInfo s2, int dx, int dy, int snapX, int snapY){
        ArrayList<Adjacency> adjs = s1.getInfo().getAdjs();
        for(int i = 0; i < adjs.size(); i++){
            Snapshot adj = Utilities.coordsToSnapshot(adjs.get(i).getFromDxdy(dx, dy), input, snapX, snapY, screenWrap);
            if(adj != null && adj.equals(s2.getSn())){
                return true;
            }
        }
        return false;
    }

    Boolean OnBoundary(int x, int y){ 
        return (x + snapshotX > outX + (snapshotX-1) || y + snapshotY > outY + (snapshotY-1) || x < 0 || y < 0 || (x + snapshotX > outX+1 && y + snapshotY > outY+1));
        //    protected override bool OnBoundary(int x, int y) => !periodic && (x + N > FMX || y + N > FMY || x < 0 || y < 0);

    };

    int RandomNum(double[] a, double r, boolean print)
    {
        double sum = 0.0;
        for (int j = 0; j < a.length; j++) sum += a[j];
        for (int j = 0; j < a.length; j++) a[j] /= sum;

        if(print){
        for (int j = 0; j < a.length; j++) System.out.print(a[j]+",");
        System.out.println();}

        int i = 0;
        double x = 0;

        while (i < a.length)
        {
            x += a[i];
            if (r <= x) return i;
            i++;
        }

        return 0;
    }
   
    Boolean[][] wave;
    int[][][] compatible;

    int[] sumsOfOnes;
    double[] weightLogWeights, sumsOfWeights, sumsOfWeightLogWeights, entropies;
    double sumOfWeights, sumOfWeightLogWeights, startingEntropy;

    Tuple[] stack;
    int stackSize;
    void buildPropogator(int outputX, int outputY){
        wave = new Boolean[outputX*outputY][];
        compatible = new int[wave.length][][];

        //populates wave and compatible matrix - might need to be converted to arrayLists
        for(int i = 0; i < wave.length; i++){

            wave[i] = new Boolean[noSnapshots];
            compatible[i] = new int[noSnapshots][];
            for(int t = 0; t < noSnapshots; t++) compatible[i][t] = new int[4];
        }

        weightLogWeights = new double[noSnapshots];
        sumOfWeights = 0;
        sumOfWeightLogWeights = 0;

        //this bit handles everything to do with setting up weights
        int it = 0;
        for(Map.Entry<Snapshot, SnapInfo> set : snapshotInfo.entrySet()){
            int curF = set.getValue().getFreqency();
            weightLogWeights[it] = curF * Math.log(curF);
            sumOfWeights += curF;
            sumOfWeightLogWeights += weightLogWeights[it];
            it++;
        }
        startingEntropy = Math.log(sumOfWeights) - sumOfWeightLogWeights / sumOfWeights;

        sumsOfOnes = new int[outputX*outputY];
        sumsOfWeights = new double[outputX*outputY];
        sumsOfWeightLogWeights = new double[outputX*outputY];
        entropies = new double[outputX*outputY];
        stack = new Tuple[wave.length * noSnapshots];
        stackSize = 0;
    }

    void Clear(){
        for(int i = 0; i < wave.length; i++){
            for(int t = 0; t < noSnapshots; t++){
                wave[i][t] = true;
                for (int d = 0; d < 4; d++) compatible[i][t][d] = propagator[opposite[d]][t].length;
            }

            sumsOfOnes[i] = snapshotInfo.size();
            sumsOfWeights[i] = sumOfWeights;
            sumsOfWeightLogWeights[i] = sumOfWeightLogWeights;
            entropies[i] = startingEntropy;

        }
    }

    int[] observed;
    //true = finished with successful generation
    //false = finished with contradiction
    //null = not yet finished
    Boolean Observe(int outputX, int outputY){
        double min = 1E+3;
        int argmin = -1;

        //finds the minimum entropy out of all the stacks
        for(int i = 0; i < wave.length; i++){
            
            if( OnBoundary(i%outputX, i/outputX) ) continue;

            int amount = sumsOfOnes[i];

            if(amount == 0){ 
                return false;
            }

            //gets entropy for current "stack" in the wave
            double entropy = entropies[i];

            if(amount > 1 && entropy <= min){
                double noise = 1E-6 * random.nextDouble();
                if(entropy+noise < min){
                    min = entropy + noise;
                    argmin = i;
                }
            }
        }

        //final case when observation is done
        if(argmin == -1){
            
            observed = new int[outputX*outputY];
            for (int i = 0; i < wave.length; i++) for (int t = 0; t < noSnapshots; t++){
                 if (wave[i][t]) { observed[i] = t; break; }
            }
            return true;
        }

        double[] distribution = new double[noSnapshots];
        for(int t = 0; t < noSnapshots; t++) distribution[t] = wave[argmin][t] ? snapshots[t].getInfo().getFreqency() : 0;
        int r = RandomNum(distribution, new Random().nextDouble(), false);

        Boolean[] w = wave[argmin];
        for(int t = 0; t < noSnapshots; t++) if(w[t] != (t == r)){
            //System.out.print("Ban from Observe: argmin, r, t: " + argmin + ", " + r + ", " + t + " || "); 
            Ban(argmin, t);
        }
        return null;
    }

    void Ban(int i, int t){
        wave[i][t] = false;

        int[] comp = compatible[i][t];
        for (int d = 0; d < 4; d++) comp[d] = 0;
        stack[stackSize] = new Tuple(i, t);
        stackSize++;

        sumsOfOnes[i] -= 1;
        sumsOfWeights[i] -= snapshots[t].getInfo().getFreqency();
        sumsOfWeightLogWeights[i] -= weightLogWeights[t];

        double sum = sumsOfWeights[i];
        entropies[i] = Math.log(sum) - sumsOfWeightLogWeights[i] / sum;
    }

    void Propagate(){
        while(stackSize > 0){
            var e1 = stack[stackSize-1];
            stackSize--;

            int i1 = e1._1();
            int x1 = i1 % outX, y1 = i1 / outX;

            for(int d = 0; d < 4; d++){
                int dx = DX[d], dy = DY[d];
                int x2 = x1 + dx, y2 = y1 + dy;
                if (!screenWrap && OnBoundary(x2, y2)) continue;

                if (x2 < 0) x2 += outX;
                else if (x2 >= outX) x2 -= outX;
                if (y2 < 0) y2 += outY;
                else if (y2 >= outY) y2 -= outY;

                int i2 = x2 + y2 * outX;
                int[] p = propagator[d][e1._2()];
                int[][] compat = compatible[i2];

                for (int l = 0; l < p.length; l++)
                {
                    int t2 = p[l];
                    int[] comp = compat[t2];

                    comp[d]--;
                    if (comp[d] == 0){
                        //System.out.println("Ban from Propagate (stack, snapshot): " + i2 + " " + t2 + " ");
                        Ban(i2, t2);
                    }
                }
            }
        }
    }


//----------------------------------
// WFC Modifications
//----------------------------------

    //bans all specified snapshots from a stack
    //auxiliary function to help with some of the modifications
    void BanAll(int i, boolean[] t){
        //Bans all tiles except for tile t from stack i in wave
        for(int n = 0; n < wave[i].length; n++){ if(!t[n] && wave[i][n]) Ban(i, n);}
    }

    //forced tile placement function
    void RunForcedRules(Preset[] ps){
        for(Preset p : ps){
            int x2 = p.getLoc()._1();
            int y2 = p.getLoc()._2();
            int i2 = x2 + y2 * outX;
            boolean[] validT = new boolean[noSnapshots];
            for (int t = 0; t < noSnapshots; t++){
                if((char)snapshots[t].getSn().output() == p.getC()){
                    validT[t] = true;
                }
            }
            BanAll(i2, validT);
        }
    }

    //bounded tile placement function
    void RunBoundedRules(Preset[] bd){
        for(Preset b : bd){
            int x2 = b.getLoc()._1();
            int y2 = b.getLoc()._2();

            int sX = x2 >= 0 ? 1 : -1;
            int sY = y2 >= 0 ? 1 : -1;

            for(int i = 0; i< outX*outY; i++){
                
                if( (i%outX)*sX < x2 || (i/outX)*sY < y2 ){
                    boolean[] validT = new boolean[noSnapshots];
                    for (int t = 0; t < noSnapshots; t++){
                        if((char)snapshots[t].getSn().output() == b.getC()){
                            validT[t] = true;
                        }
                    }
                    for(int n = 0; n < wave[i].length; n++){ if(validT[n]) Ban(i, n);}

                }

            }
        }
    }

    //screen-wrapping removal function
    void PrepareNonWrapping(){
        //for corners
        boolean topl = false, topr = false, bottoml = false, bottomr = false;
        for(int t = 0; t < noSnapshots; t++){
            for(Adjacency j : snapshots[t].getInfo().getAdjs()){
                Tuple up = j.up(), down = j.down(), left = j.left(), right = j.right(); 
                if(up._1() == -1 && up._2() == -1){
                    if(right._1() == -1 && right._2() == -1) {topr = true;}
                    if(left._1() == -1 && left._2() == -1) {topl = true;}
                    
                }
                else if(down._1() == -1 && down._2() == -1){
                    if(right._1() == -1 && right._2() == -1) {bottomr = true;}
                    if(left._1() == -1 && left._2() == -1) {bottoml = true;}
                }
            }
            
            if(!topl){ Ban(0, t);}
            if(!topr){ Ban(outX-1, t); }
            if(!bottoml) Ban((outX*outY)-outX, t);
            if(!bottomr) Ban((outX*outY)-1, t);

            topl = false; topr = false; bottoml = false; bottomr = false;
        }

        //for edges and middle
        boolean[] validTu = new boolean[noSnapshots];
        boolean[] validTd = new boolean[noSnapshots];
        boolean[] validTl = new boolean[noSnapshots];
        boolean[] validTr = new boolean[noSnapshots];

        boolean u = false, d = false, l = false, r = false;

        for (int t = 0; t < noSnapshots; t++){
            for(Adjacency j : snapshots[t].getInfo().getAdjs()){
                Tuple up = j.up(), down = j.down(), left = j.left(), right = j.right();
                
                if(up._1() == -1 && up._2() == -1){ u = true; }
                if(right._1() == -1 && right._2() == -1) { r = true; }
                if(left._1() == -1 && left._2() == -1) { l = true; }
                if(down._1() == -1 && down._2() == -1){ d = true; }

                if(u & !l & !r){ validTu[t] = true; }
                if(r & !u & !d) { validTr[t] = true; }
                if(l & !u & !d) { validTl[t] = true; }
                if(d & !l & !r){ validTd[t] = true; }

                u = false; d = false; l = false; r = false;
            }
        }

        //top border
        for(int i = 1; i < outX-1; i++) BanAll(i, validTu);
        //left border
        for(int i = outX; i < wave.length-outX; i+=outX) BanAll(i, validTl);
        //right border
        for(int i = outX*2-1; i < wave.length-1; i+= outX) BanAll(i, validTr);
        //bottom border
        for(int i = outX*outY-outX; i < wave.length-1; i++) BanAll(i, validTd);

        Propagate();
    }

    //The next two functions are an unfinished implementation of the forced-frequency modification - currently commented out
    int[] freqs; 
    void LoadFrequency(char[] ffs){
        // //first count frequencies from input - flatten, then group by count
        // freqs = new int[ffs.length];
        // int count = 0;
        // int index = 0;
        // for(char c : ffs){
            
        //     for(char[] l : input){
        //         for(char z : l){
        //             if(z == c) count++;
        //         }
        //     }
        //     //theoretically, the index of freqs should match ffs
        //     freqs[index] = count;
        //     index += 1;
            
        // }
    }
    void DecrementFrequency(char c, char[] ffs){
        // //if c exists in ffs, get its index
        // int ind = -1;
        // for(int i = 0; i < ffs.length; i++){
        //     if(ffs[i] == c) {ind = i; break;}
        // }

        // if(ind == -1) return;
        // freqs[ind] -= 1;

        // //if, at end, number in freqs is 0, then call banAll on character c
        // if(freqs[ind] == 0){
        //     for(int i = 0; i< outX*outY; i++){
        //         boolean[] validT = new boolean[noSnapshots];
        //         for (int t = 0; t < noSnapshots; t++){
        //             if((char)snapshots[t].getSn().output() == c){
        //                 validT[t] = true;
        //             }
        //         }
        //         for(int n = 0; n < wave[i].length; n++){ if(validT[n]) Ban(i, n);}
        //     }
        // }
    } 

    
    protected static int[] DX = { -1, 0, 1, 0 };
    protected static int[] DY = { 0, 1, 0, -1 };
    static int[] opposite = { 2, 3, 0, 1 };

}