package levelGenerators.ttWFC;

import engine.core.MarioGame;
import engine.core.MarioLevelModel;
import engine.core.MarioResult;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

public class BatchRunner {
    public static void main(String[] args) throws Exception {
        String[] samples   = { "lvl-1", "lvl-2", "lvl-3", "lvl-4", "lvl-5", "lvl-6", "lvl-7", "lvl-8", "lvl-9", "lvl-10", "lvl-11", "lvl-12", "lvl-13", "lvl-14", "lvl-15"};
        int[]    Ms        = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int[]    Ns        = { 1, 2, 3, 4, 5, 6, 7, 8 };
        int      repeats   = 1;             // how many outputs repeats per MxN size and sample
        int      maxAttempts = 2000;        // max WFC seeds to try per repeat 
        MarioGame game     = new MarioGame();
        try (PrintWriter csv = new PrintWriter(new FileWriter("ttwfc_results.csv"))) {
            csv.println("sample,M,N,seed,gameStatus,completion,lives,coins,time,jumps,kills");

            Random rnd = new Random(12345);
            for (String sample : samples) {
                List<String> lines = Files.readAllLines(
                  Paths.get("src/levelGenerators/ttWFC/samples/" + sample + ".txt")
                );
                int origW = lines.get(0).length();
                int outH  = 16;

                for (int M : Ms) {
                    for (int N : Ns) {
                        int outW = origW;
                        for (int r = 0; r < repeats; r++) {
                            boolean success = false;
                            int seed = -1;
                            OverlappingModel wfc = null;

                            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                                seed = rnd.nextInt();
                                wfc = new OverlappingModel(
                                  sample, M, N, outW / M, outH / N,
                                  false, false, 1, true,
                                  OverlappingModel.Heuristic.Entropy
                                );
                                if (wfc.Run(seed, -1)) {
                                    success = true;
                                    break;
                                }
                            }

                            if (!success) {
                                csv.printf(
                                  "%s,%d,%d,%d,FAIL,0.00,0,0,0,0,0%n",
                                  sample, M, N, -1
                                );
                                csv.flush();
                                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                System.out.println("   AFTER " + maxAttempts + "ATTEMPTS WFC FAILED ON " + sample+ " ON Window M="+M+", N="+N);
                                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                continue;
                            }

                            String tmp = "output/tmp-" + sample + "-M" + M + "-N" + N + "-s" + seed;
                            wfc.Save(tmp);

                            MarioLevelModel model = new MarioLevelModel(outW, outH);
                            model.copyFromString(new String(
                              Files.readAllBytes(Paths.get(tmp + ".txt"))
                            ));
                            MarioResult res = game.runGame(
                              new agents.robinBaumgarten.Agent(),
                              model.getMap(),
                              40, 0, true
                            );

                            csv.printf(
                              "%s,%d,%d,%d,%s,%.2f,%d,%d,%d,%d,%d%n",
                              sample, M, N, seed,
                              res.getGameStatus(),
                              res.getCompletionPercentage(),
                              res.getCurrentLives(),
                              res.getCurrentCoins(),
                              (int)Math.ceil(res.getRemainingTime() / 1000f),
                              res.getNumJumps(),
                              res.getKillsTotal()
                            );
                            csv.flush();
                                    System.out.println("****************************************************************");
                                    System.out.println("**SAMPLE: " + sample + ", M: " + M + ", N: " + N + "********");
                                    System.out.println("****************************************************************");
                                    System.out.println("Game Status: " + res.getGameStatus().toString() +
                                            " Percentage Completion: " + res.getCompletionPercentage());
                                    System.out.println("Lives: " + res.getCurrentLives() + " Coins: " + res.getCurrentCoins() +
                                            " Remaining Time: " + (int) Math.ceil(res.getRemainingTime() / 1000f));
                                    System.out.println("Mario State: " + res.getMarioMode() +
                                            " (Mushrooms: " + res.getNumCollectedMushrooms() + " Fire Flowers: " + res.getNumCollectedFireflower() + ")");
                                    System.out.println("Total Kills: " + res.getKillsTotal() + " (Stomps: " + res.getKillsByStomp() +
                                            " Fireballs: " + res.getKillsByFire() + " Shells: " + res.getKillsByShell() +
                                            " Falls: " + res.getKillsByFall() + ")");
                                    System.out.println("Bricks: " + res.getNumDestroyedBricks() + " Jumps: " + res.getNumJumps() +
                                            " Max X Jump: " + res.getMaxXJump() + " Max Air Time: " + res.getMaxJumpAirTime());
                                    System.out.println("****************************************************************");
                        }
                    }
                }
            }

            System.out.println("Batch finished!  See ttwfc_results.csv");
        }
    }
}
