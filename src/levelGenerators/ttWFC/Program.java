package levelGenerators.ttWFC;
import java.io.File;
import java.util.List;
import java.util.Random;
import java.nio.file.Files;
import java.nio.file.Paths;

//THIS ONLY PRODUCES THE TEXT FILE
public class Program {
    public static void main(String[] args) throws Exception {

        File folder = new File("output");
        folder.mkdirs();
        for (File f : folder.listFiles()) f.delete();



        Random random = new Random();
        int seed = random.nextInt();
        String name = "lvl-8";    
        int M = 3;  
        int N = 3;
        //SET width = origSX to be same as input file
        List<String> lines = Files.readAllLines(Paths.get("src/levelGenerators/TTWFC/samples/" + name + ".txt"));
        int origSX = lines.get(0).length();
        int width = origSX;
        if(width%M>0) width += M - (width % M);
        int height = 16;        
        if(height%N>0) height += N - (height % N);      
        boolean periodicInput = false;  
        boolean periodic = false;       
        int symmetry = 1;             
        boolean ground = true;       
        Model.Heuristic heuristic = Model.Heuristic.Entropy;

        Model model = new OverlappingModel(
            name, M, N, width/M, height/N,
            periodicInput, periodic,
            symmetry, ground,
            heuristic
        );

        // - run single time
        boolean success = model.Run(seed, -1);
        // if (!success) {
        //     System.out.println("WFC failed (contradiction) on lvl-1.txt");
        //     return;
        // }
        // -

        // - run till success
        int runs = 0;
        do {
            runs ++;
            seed = random.nextInt();
            System.out.println("Trying seed “" + seed + "'");
            success = model.Run(seed, -1);
            if (!success) {
                System.out.println("  → contradiction, retrying");
                model = new OverlappingModel(
                    name, M, N, width/M, height/N,
                    periodicInput, periodic,
                    symmetry, ground,
                    heuristic
                );
            }
        } while (!success);
        // -

        String outFile = "output/" + name + "M=" +M + "_N =" + N + "_seed;" + seed;
        model.Save(outFile);
        if(runs>0) System.out.println("WFC succeeded after " + runs + " attempts\n");
        System.out.println("WFC on " + name + ".txt → " + outFile + ".txt");


    }
}
