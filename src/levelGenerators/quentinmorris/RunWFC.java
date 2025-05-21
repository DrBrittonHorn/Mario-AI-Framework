package levelGenerators.quentinmorris;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

//import WFC.Ret;

public class RunWFC {

    protected char[][] input;
    
    protected String inFile;
    protected Preset[] presets = 
        {};
        //{ new Preset(new Tuple(3, 15), 'X') };
        //{ new Preset(new Tuple(3, 15), 'X'),  new Preset(new Tuple(198, 15), 'X')}; //for lvl-1.txt
        //{ new Preset(new Tuple(3, 15), 'X'),  new Preset(new Tuple(198, 15), 'X'), new Preset(new Tuple(101, 15), 'X')};
        //note for lvl-3: mario not present, instead force starting/ending platforms - just add mario in text file
        //{ new Preset(new Tuple(3, 14), 'X') };
        //{ new Preset(new Tuple(3, 14), 'X'),  new Preset(new Tuple(148, 14), 'X')};

    protected Preset[] bounds = 
        {};
        //{ new Preset(new Tuple(0, 10), 'X') }; //not great for lvl-2.txt
        //{ new Preset(new Tuple(0, 13), 'X') }; //not great for lvl-2.txt
        //for lv3
        //{ new Preset(new Tuple(0, 7), '%') }; //not great for lvl-2.txt
        //{ new Preset(new Tuple(0, 10), '%') }; //not great for lvl-2.txt
    protected char[] forcedFs = {};//{'F'};
    protected int snapshotX = 4, snapshotY = 4; //snapshot dimensions are reversed
    protected int outX, outY;
    
    

    public static void main( String[] args ) throws FileNotFoundException{
        RunWFC r = new RunWFC();

        r.Instantiate("output/params.txt");

        //---------------------------
        // For Generation
        //---------------------------
        // System.out.println("Generating levels...");
        // for(int i = 0; i < 1000; i++){
        //     String outFile = "test-output-" + i + ".txt";
        //     if(i == 999) r.Run(r.inFile, outFile, true);
        //     else r.Run(r.inFile, outFile, false);
        // }
        // System.out.println("Running diagnostics...");
        // r.Diagnostics(1000);

        //---------------------------
        // For Printing
        //---------------------------
        String outFile = "aaa";
        r.Run(r.inFile, outFile, false);

        //---------------------------
        // Analyse Original Levels
        //---------------------------
        // r.Instantiate("input-levels-with-analysis/params.txt");
        // r.Diagnostics(15);

    }

    String Run(String inFile, String outFile, boolean last) throws FileNotFoundException{

        WFC wfc = new WFC(input, presets, bounds, forcedFs, snapshotX, snapshotY, outX, outY, false);
        WFC.Ret r = wfc.startTest(true);
        char[][] map = r.getImage();
        int[][] frequencyCompare = r.getFs();
        
        //--------------------------
        // Print the generated level
        //--------------------------
        StringBuilder mapRet = new StringBuilder();
        System.out.println(outX + ", " + outY);
        for (int y = 0; y < outY; y++){
            for (int x = 0; x < outX; x++) {
                System.out.print( map[x][y]);
                mapRet.append(map[x][y]);
            }
            System.out.println();
            mapRet.append('\n');
        }

        //---------------------------
        // Output the generated level
        //---------------------------
        // OutputToFile("output/"+outFile, map);
        // recordFrequency(frequencyCompare, last, "output/frequencyRecord.csv");

        return mapRet.toString();

    }

    void Instantiate(String outFile) throws FileNotFoundException{
        
        inFile = "lvl-1.txt";
        input = GetInputFromFile("levels/original/"+ inFile);
        // char[][] temp =
        // // { { 'A', 'A', 'A', 'A', 'A'},
        // // { 'A', 'A', 'A', 'A', 'A'},
        // // { 'A', 'A', 'X', 'A', 'A'},
        // // { 'B', 'B', 'B', 'B', 'B'},
        // // { 'B', 'B', 'B', 'B', 'B'}
        // // };
        // { { 'A', 'A', 'A', 'A'},
        // { 'A', 'A', 'A', 'A'},
        // { 'A', 'A', 'A', 'A'},
        // { 'B', 'B', 'B', 'B'}
        // };
        // input = temp;
        // outX = 202;
        // outY = 16;
        outX = input[0].length;
        outY = input.length;

        //RecordParams(outFile);
    }

    void Diagnostics(int noFiles) throws FileNotFoundException{

        String outFile = "output/output-diagnostics.csv";
        try {
            File myObj = new File(outFile);
            if (!myObj.createNewFile()) {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        FileWriter myWriter = null;
        try {
            myWriter = new FileWriter(outFile);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        int noMario = 0;
        int noFlag = 0;
        double totalDensityS = 0;
        double totalDensity15 = 0;
        double totalDensity3 = 0;
        double totalLinearity = 0;
        for(int i = 0; i < noFiles; i++){

            String name = "output/test-output-" + i +".txt";
            char[][] level = GetInputFromFile(name);

            try {
                myWriter.write(i+","+density(level, 0)+","+density(level, 351)+","+density(level, 318)+","+linearity(level)+"\n");
                // myWriter.write("level: "+name+"\n");
                // myWriter.write("-----------------\n");
                // myWriter.write("density, level size norm: "+density(level, 0)+"\n");
                // myWriter.write("density, all 15 norm: "+density(level, 351)+"\n");
                // myWriter.write("density, 3 selected norm: "+density(level, 318)+"\n");
                // myWriter.write("linearity: "+linearity(level)+"\n");
                // myWriter.write("\n");
    
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }

            totalDensityS += density(level, 0);
            totalDensity15 += density(level, 351);
            totalDensity3 += density(level, 318);
            totalLinearity += linearity(level);
        }

    
        try {
            // myWriter.write("----------------------------------------------------------------\n");
            // myWriter.write("                        Aggregate Stats                         \n");
            // myWriter.write("----------------------------------------------------------------\n");
            // myWriter.write("average density, level size norm: "+totalDensityS/noFiles+"\n");
            // myWriter.write("average density, all 15 norm: "+totalDensity15/noFiles+"\n");
            // myWriter.write("average density, 3 selected norm: "+totalDensity3/noFiles+"\n");
            // myWriter.write("average linearity: "+totalLinearity/noFiles+"\n");

            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }


        //printing diagnostics
//issue, this currently doesn't account for >1 mario/flag per level
        // System.out.println("Number of levels analysed = " + noFiles);
        // System.out.println("Number of Mario occurrences = " + noMario);
        // System.out.println("Number of Flagpole occurrences = " + noFlag);


    }

    public Double density(char[][] level, int norm){

        //cache of stand-able chars
        ArrayList<Character> dict = new ArrayList<Character>(
            Arrays.asList('X', '#', '%', 'B', 'b', '@', '!', '1','2','D','S','C', 'U', 'L', 't', 'T', '<', '>', '[',']')
        );

        //cache of transparent chars
        ArrayList<Character> dictT = new ArrayList<Character>(
            Arrays.asList('-', 'M', 'F', 'y', 'Y' , 'g', 'G', 'k', 'K', 'r', 'R', '|', '*', 'o', '%', '1', '2')
        );

        double totalD = 0;
        double curD = 0;

        int levelX = level[0].length;
        int levelY = level.length;

        //make sure these x and y are actually what I think they are
        for(int x = 0; x < levelX; x++){
            curD = 0;
            for(int y = 1; y < levelY; y++){
                // char above = level[x][y-1];
                // char here = level[x][y];
                char above = level[y-1][x];
                char here = level[y][x];
                if(dict.contains(here) && dictT.contains(above)){
                    curD++;
                }
            }
            totalD += curD/*/outY*/;
        }

        if(norm == 0) return totalD/(levelX*levelY);
        else return totalD/norm;
    }

    public Double linearity(char[][] level){

        ArrayList<Character> dict = new ArrayList<Character>(
            Arrays.asList('X', '#', '%', 'B', 'b', '@', '!', '1','2','D','S','C', 'U', 'L', 't', 'T', '<', '>', '[',']')
        );

        ArrayList<Character> dictT = new ArrayList<Character>(
            Arrays.asList('-', 'M', 'F', 'y', 'Y' , 'g', 'G', 'k', 'K', 'r', 'R', '|', '*', 'o', '%', '1', '2')
        );

        //store list of platform end-points
        ArrayList<Tuple> endpoints = new ArrayList<Tuple>();

        boolean prevWasPlatform = false;
        Tuple prevPlatform = null;

        int levelX = level[0].length;
        int levelY = level.length;

        for(int y = 1; y < levelY; y++){
            for(int x = 0; x < levelX; x++){
                // char above = level[x][y-1];
                // char here = level[x][y];
                char above = level[y-1][x];
                char here = level[y][x];
                if(dict.contains(here) && dictT.contains(above)){
                    prevWasPlatform = true;
                    prevPlatform = new Tuple(x,y);

                    //if end of row, store
                    if(x == outX -1){
                        prevWasPlatform = false;
                        endpoints.add(prevPlatform);
                    }
                }
                else if(prevWasPlatform){
                    prevWasPlatform = false;
                    endpoints.add(prevPlatform);
                }
            }
            prevWasPlatform = false;
            prevPlatform = null;
        }
        
        //best fit computation

        int n = endpoints.size();
        if(n == 1) return 1.0;
        //System.out.println(n);
        //E(xy), E(x), E(y)
        long exy = 0, ex = 0, ey = 0;
        //E(x^2), E(y^2)
        long ex2 = 0, ey2 = 0;

        for(Tuple t : endpoints){
            int x = t._1(), y = t._2();
            //t.printTuple();
            exy += x*y; ex += x; ey += y;
            ex2 += x*x; ey2 += y*y;
        }

        //calculate and square r
        double top = (n*exy) - (ex*ey);
        // System.out.println(top);
        long a = (n*ex2 - ex*ex);
        if(a == 0) return 0.0;
        long b = (n*ey2 - ey*ey);
        if(b == 0) return 1.0;
        double bottom = Math.sqrt(a*b);


        // System.out.println("n = " + n + " ex2 = " + ex2*n + " ex = " + ex + " ey2 = " + ey2*n + " ey = " + ey + " ex^2 = " + ex*ex + " ey^2 = " + ey*ey);
        // System.out.printf("a = %d b = %d", (n*ex2 - ex*ex), (n*ey2 - ey*ey));
        // System.out.println(bottom);
        return Math.pow(top/bottom, 2);
    }

    public char[][] GetInputFromFile(String inFile) throws FileNotFoundException{
        char[][] ret;

        File file = new File(inFile);
        Scanner scanner = new Scanner(file);     
        
        String out = "";
        while(scanner.hasNext()){
            out = out + "\n" + scanner.nextLine().toString();
        }
        
        String[] between = out.split("\n");
        ret = new char[between.length-1][between[0].length()];
        for(int i = 1; i < between.length; i++){
            ret[i-1] = between[i].toCharArray();
        }
        
        scanner.close();
        return ret;

    }

    void OutputToFile(String outFile, char[][] map){

        try {
            File myObj = new File(outFile);
            if (!myObj.createNewFile()) {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter(outFile);
            for (int y = 0; y < outY; y++){
                for (int x = 0; x < outX; x++) myWriter.write(map[x][y]);
                myWriter.write('\n');
            }
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }


    }

    void RecordParams(String outFile){
        try {
            System.out.println("Recording parameters to " + outFile);
            File myObj = new File(outFile);
            if (!myObj.createNewFile()) {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter(outFile);

            myWriter.write("input = " + inFile + '\n');
            myWriter.write("snapshot x = " + snapshotX + " snapshot y = " + snapshotY + '\n');
            myWriter.write("output x = " + outX + " output y = " + outY + '\n');
            myWriter.write("Forced Tiles :\n");
            for(Preset p : presets){
                myWriter.write("    "+(char)p.getC()+" at (" + p.getLoc()._1() + "," + p.getLoc()._2() + ")\n");
            }
            myWriter.write("Bounded Tiles :\n");
            for(Preset p : bounds){
                myWriter.write("    "+(char)p.getC()+" at (" + p.getLoc()._1() + "," + p.getLoc()._2() + ")\n");
            }
            
            myWriter.write("\nInput Level Stats\n");
            char[][] level = GetInputFromFile("input/"+inFile);
            //char[][] level = GetInputFromFile("output/test-output-0 copy.txt");
            myWriter.write("density, level size norm: "+density(level, 0)+"\n");
            myWriter.write("density, all 15 norm: "+density(level, 351)+"\n");
            myWriter.write("density, 3 selected norm: "+density(level, 318)+"\n");
            myWriter.write("linearity: "+linearity(level)+"\n");
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    
    public void recordFrequency(int[][] frequencyCompare, boolean last, String outFile) {
        try {

            File myObj = new File(outFile);
            if (!myObj.exists()) {
                myObj.createNewFile();
            }
            
            FileWriter myWriter = null;
            myWriter = new FileWriter(outFile, true);

            for(int i = 0; i < frequencyCompare[1].length; i++) myWriter.write(frequencyCompare[1][i]+",");
            myWriter.write("\n");
            if(last) for(int i = 0; i < frequencyCompare[0].length; i++) myWriter.write(frequencyCompare[0][i]+",");

            myWriter.close();


        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        
    }


}
