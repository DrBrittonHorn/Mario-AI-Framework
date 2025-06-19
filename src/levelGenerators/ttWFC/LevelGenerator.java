package levelGenerators.ttWFC;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

import engine.core.MarioLevelGenerator;
import engine.core.MarioLevelModel;
import engine.core.MarioTimer;

public class LevelGenerator implements MarioLevelGenerator {
  @Override
  public String getGeneratorName() {
    return "ttWFC";
  }

  @Override
  public String getGeneratedLevel(MarioLevelModel model, MarioTimer timer) {
    try {
      return runAndGetLevel();
    } catch (Exception e) {
      e.printStackTrace();
      MarioLevelModel blank = new MarioLevelModel(model.getWidth(), model.getHeight());
      blank.clearMap();
      return blank.getMap();
    }
  }
      public static String runAndGetLevel() throws Exception {
        File folder = new File("output");
        folder.mkdirs();
        for (File f : folder.listFiles()) f.delete();

        Random random = new Random();
        int seed = random.nextInt();
        String name = "lvl-13";
        int M = 6;
        int N = 3;
        List<String> lines = Files.readAllLines(
            Paths.get("src/levelGenerators/ttWFC/samples/" + name + ".txt")
        );
        int origSX = lines.get(0).length();
        int width = origSX;
        if (width % M > 0) width += M - (width % M);
        int height = 16;
        if (height % N > 0) height += N - (height % N);

        boolean periodicInput = false;
        boolean periodic = false;
        int symmetry = 1;
        boolean ground = true;
        Model.Heuristic heuristic = Model.Heuristic.Entropy;

        Model model = new OverlappingModel(
            name, M, N, width / M, height / N,
            periodicInput, periodic,
            symmetry, ground,
            heuristic
        );

        boolean success;
        do {
            seed = random.nextInt();
            model = new OverlappingModel(
                name, M, N, width / M, height / N,
                periodicInput, periodic,
                symmetry, ground,
                heuristic
            );
            success = model.Run(seed, -1);
        } while (!success);

        String outFile = "output/ttwfc_" + seed;
        model.Save(outFile);

        byte[] bytes = Files.readAllBytes(Paths.get(outFile + ".txt"));
        return new String(bytes);
    }
}
