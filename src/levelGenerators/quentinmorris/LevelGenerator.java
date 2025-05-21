package levelGenerators.quentinmorris;

import java.io.FileNotFoundException;

import engine.core.MarioLevelGenerator;
import engine.core.MarioLevelModel;
import engine.core.MarioTimer;

public class LevelGenerator implements MarioLevelGenerator {
    @Override
    public String getGeneratedLevel(MarioLevelModel model, MarioTimer timer) {
        RunWFC r = new RunWFC();

        try {
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
            String outputLevel = r.Run(r.inFile, outFile, false);
            return outputLevel;
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public String getGeneratorName() {
        return "QuentinMorrisWFC";
    }
}
