package levelGenerators.quentinmorris;

import java.util.ArrayList;
import java.util.Arrays;


public class Utilities {

    //prints all snapshots taken from the input image along with optionally their adjacencies and their adjacencies' image-representation
    static void printSnapshots(SnapAndInfo[] snapshots, char[][] input, int snapshotX, int snapshotY, boolean printAdj, boolean showAdj, boolean screenWrap){

        System.out.println("printing snapshots");
        for(int i = 0; i < snapshots.length; i++){
            if(i == 2){
                System.out.println("-----"+i+"-----");
                snapshots[i].getSn().printSnapshot();
                System.out.println("frequency = "+snapshots[i].getInfo().getFreqency());
                ArrayList<Adjacency> adjs = snapshots[i].getInfo().getAdjs();//.printAdjs();
                for(int q = 0; q < adjs.size(); q++){
                    Adjacency z = adjs.get(q);
                    if(printAdj) z.printAdj();

                    if(showAdj){
                        Snapshot up = coordsToSnapshot(adjs.get(q).up(), input, snapshotX, snapshotY, screenWrap);
                        if(up != null){System.out.println("up:"); up.printSnapshot();}
                        
                        Snapshot down = coordsToSnapshot(adjs.get(q).down(), input, snapshotX, snapshotY, screenWrap);
                        if(down != null) {System.out.println("down:"); down.printSnapshot();}

                        Snapshot left = coordsToSnapshot(adjs.get(q).left(), input, snapshotX, snapshotY, screenWrap);
                        if(left != null) {System.out.println("left:"); left.printSnapshot();}

                        Snapshot right = coordsToSnapshot(adjs.get(q).right(), input, snapshotX, snapshotY, screenWrap);
                        if(right != null) {System.out.println("right:"); right.printSnapshot();}
                    }
                }
                System.out.println('\n');
            }
        }

    }

    //for displaying 2D arrays when necessary
    public static void PrintArray(char[][] a){
        for(int i = 0; i < a.length; i++){
            for(int j = 0; j < a[i].length; j++){
                System.out.print(a[i][j]);
            }
            System.out.println();
        }
    }

    //Prints out the wave for use at any step in the generation process
    void PrintWave(String title, int outX, int outY, Boolean[][] wave){

        System.out.println("----------------------------");
        System.out.println(title);
        System.out.println("----------------------------");

        for(int i = 0; i < outX*outY; i++){

            // int count = 0;
            // for(Boolean x : wave[i]) if(x) count++;

            if(i != 0 && i%(outX) == 0) System.out.println();
            //System.out.print(count + ",");
            for(int j = 0; j < wave[i].length; j++) if(wave[i][j]) System.out.print(j+",");
            System.out.println();

        }

        System.out.println("****************************");
    }

    //Converts coordinate position in the input image to the snapshot image at that location
    static Snapshot coordsToSnapshot(Tuple c, char[][] input, int snapX, int snapY, boolean screenWrap){
        if(c._1() == -1 || c._2() == -1) return null;
        char[][] snapshot = new char[snapX][snapY];
        if(screenWrap){
            for(int x = 0; x < snapX; x++) for(int y = 0; y < snapY; y++){
                int xVal = (x+c._1() < input.length ? x+c._1() : 0);
                int yVal = (y+c._2() < input[c._1()].length ? y+c._2() : 0);
                snapshot[x][y] = input[xVal][yVal];
            }
        }
        else{
            for(int x = 0; x < snapX; x++) for(int y = 0; y < snapY; y++){
                if(x+c._1() >= input.length || y+c._2() >= input[c._1()].length) snapshot[x][y] = '.';
                else snapshot[x][y] = input[x+c._1()][y+c._2()];
            }
        }
        Snapshot sn = new Snapshot(snapX, snapY, snapshot);
        return sn;
    } 

    //adds a tile on top of a solid block as far left or right on the level as possible
    //  (reserved for adding Mario 'M' of Flagpole 'F' manually for playability purposes)
    static char[][] AddTile(char tile, char[][] level, int outX, int outY, boolean startFromRight){

        int charCount = 0;
        for (int y = 0; y < outY; y++){
            for (int x = 0; x < outX; x++){ 
                if(level[x][y] == tile) charCount += 1;
            }
        }

        if(charCount == 0){
            ArrayList<Character> dict = new ArrayList<Character>(
                Arrays.asList('X', '#', '%', 'B', 'b', '@', '!', '1','2','D','S','C', 'U', 'L', 't', 'T', '<', '>', '[',']')
            );

            boolean skipLines = true;
            int x;
            for(int i = 0; i < outX; i++){
                
                if(startFromRight) x = (outX-1)-i;
                else x = i;

                for(int y = outY - 1; y >= 0; y--){
                    Character c = level[x][y];
                    if(dict.contains(c)){
                        skipLines = false;
                        continue;
                    }
                    if (!skipLines && !dict.contains(c)) {
                        level[x][y] = tile;
                        charCount++;
                        break;
                    }
                }
                if(charCount > 0) break;
            }
            if(charCount == 0) level[0][0] = tile;
        }
        return level;
    }

    //takes the leftmost occurence of a tile
    //  (reserved for removing all except the leftmost Mario appearance)
    static char[][] takeLeftmostTile(char tile, char[][] level, int outX, int outY){

        int charCount = 0;
        for (int y = 0; y < outY; y++){
            for (int x = 0; x < outX; x++){ 
                if(level[x][y] == tile) charCount += 1;
            }
        }

        if(charCount > 1){
            boolean firstMarioFound = false;
            for(int x = 0; x < outX; x++){
                for(int y = 0; y < outY; y++){
                    char c = level[x][y];
                    if(c == 'M'){
                        charCount--;
                        if(firstMarioFound) level[x][y] = '-';
                        else firstMarioFound = true;
                    }
                    if(charCount == 0) break;
                }
                if(charCount == 0) break;
            }
        }
        return level;
    }

    //commented out because I don't think this is necessary anymore
    //populates the input "image" of the 2D int array
    //might want to add: rand seed, variable input image size
    static void PopulateInput(int min, int max, Boolean random){
        // input = new char[3][3];
        // for(int i = 0; i < input.length; i++){
        //     for(int j = 0; j < input[i].length; j++){
        //         if(random) input[i][j] = (char)((Math.random() * (max - min)) + min);
        //         else input[i][j] = 1;
                
        //     }
        // }
    }

    
}
