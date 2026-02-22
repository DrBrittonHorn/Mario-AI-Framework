package levelGenerators;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import engine.core.MarioGame;
import engine.core.MarioResult;
import engine.helper.GameStatus;

public class researchPlay {
    static final String LEVEL_FOLDER = "./levels/Trials/";
    static final String PRACTICE_LEVEL_PATH = "./levels/original/lvl-1.txt";
    static final String[] LEVEL_LIST = {"26", "40", "15", "2", "6", "9", "23", "22", "16", "37"};
    static final int MAX_ATTEMPTS = 5;
    static final int TIME_PER_LEVEL = 90;

    static final String[] SURVEY_QUESTIONS = {
        "Playability: I was able to make progress through this level.",
        "Enjoyment: I enjoyed playing this level.",
        "Coherence: This level felt intentionally designed (not random).",
        "Challenge: This level was appropriately challenging (not too easy or too hard).",
        "Replayability: I would want to play more levels like this one."
    };

    static int[] attemptsPerLevel = new int[LEVEL_LIST.length];
    static float[] maxCompletionPerLevel = new float[LEVEL_LIST.length];
    static int[][] surveyResponses = new int[LEVEL_LIST.length][5];

    public static String getLevel(String filepath) {
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(filepath)));
        } catch (IOException e) {
            System.out.println("Error loading level: " + filepath);
        }
        return content;
    }

    public static void waitForReady(Scanner scanner) {
        System.out.println("Press Y and enter when ready for next level");
        while (true) {
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("Y")) break;
        }
    }

    public static int getSurveyRating(Scanner scanner, String question) {
        System.out.println("\n" + question);
        System.out.print("Enter your rating (1-7): ");
        while (true) {
            try {
                int rating = Integer.parseInt(scanner.nextLine().trim());
                if (rating >= 1 && rating <= 7) {
                    return rating;
                }
                System.out.print("Please enter a number between 1 and 7: ");
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid number between 1 and 7: ");
            }
        }
    }

    public static void runSurvey(Scanner scanner, int levelIndex) {
        System.out.println("\n****************************************************************");
        System.out.println("SURVEY - Please rate this level");
        System.out.println("****************************************************************");
        System.out.println("Rating Scale:");
        System.out.println("1 = Strongly Disagree");
        System.out.println("2 = Disagree");
        System.out.println("3 = Slightly Disagree");
        System.out.println("4 = Neutral");
        System.out.println("5 = Slightly Agree");
        System.out.println("6 = Agree");
        System.out.println("7 = Strongly Agree");

        for (int i = 0; i < SURVEY_QUESTIONS.length; i++) {
            surveyResponses[levelIndex][i] = getSurveyRating(scanner, SURVEY_QUESTIONS[i]);
        }
    }

    public static void printSummary() {
        System.out.println("SUMMARY");
        for (int i = 0; i < LEVEL_LIST.length; i++) {
            System.out.print("Level " + LEVEL_LIST[i] + ": ");
            System.out.print("Attempts: " + attemptsPerLevel[i] + " | ");
            System.out.print("Max Completion: " + Math.round(maxCompletionPerLevel[i] * 100) + "% | ");
            System.out.print("Responses: [");
            for (int j = 0; j < surveyResponses[i].length; j++) {
                System.out.print(surveyResponses[i][j]);
                if (j < surveyResponses[i].length - 1) System.out.print(", ");
            }
            System.out.println("]");
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("****************************************************************");
        System.out.println("PRACTICE LEVEL - Close the game window when done practicing");
        System.out.println("Move: Left/Right Arrow, Run: S, Jump: A");
        System.out.println("****************************************************************");

        String practiceLevel = getLevel(PRACTICE_LEVEL_PATH);
        MarioGame game = new MarioGame(); 
        game.setupWindow(2);
        game.setAgentForWindow(new agents.human.Agent());

        float maxCompletion = 0;
        while (!game.isWindowClosed()) {
            MarioResult result = game.runGameLoopOnly(practiceLevel, 0, 0, 30);
            if (result != null) {
                float completion = Math.min(1.0f, result.getCompletionPercentage());
                maxCompletion = Math.max(maxCompletion, completion);
            }
        }
        game.disposeWindow();

        waitForReady(scanner);

        for (int i = 0; i < LEVEL_LIST.length; i++) {
            String levelName = LEVEL_LIST[i];
            String levelPath = LEVEL_FOLDER + levelName + ".txt";

            System.out.println("****************************************************************");
            System.out.println("LEVEL " + (i + 1) + " of " + LEVEL_LIST.length + " (" + levelName + ".txt)");
            System.out.println("****************************************************************");

            String level = getLevel(levelPath);
            game = new MarioGame();
            game.setupWindow(2);
            game.setAgentForWindow(new agents.human.Agent());

            maxCompletion = 0;
            int attempt = 0;
            long levelStartTime = System.currentTimeMillis();
            long timeLimitMs = TIME_PER_LEVEL * 1000L;

            while (attempt < MAX_ATTEMPTS && !game.isWindowClosed()) {
                long elapsedMs = System.currentTimeMillis() - levelStartTime;
                long remainingMs = timeLimitMs - elapsedMs;
                if (remainingMs <= 0) break;
                int remainingSeconds = (int) Math.ceil(remainingMs / 1000.0);

                attempt++;
                MarioResult result = game.runGameLoopOnly(level, remainingSeconds, 0, 30);
                if (result != null) {
                    float completion = Math.min(1.0f, result.getCompletionPercentage());
                    maxCompletion = Math.max(maxCompletion, completion);
                    System.out.println("Level " + levelName + " - Attempt " + attempt
                            + " - Status: " + result.getGameStatus()
                            + " - Max Completion: " + maxCompletion);

                    if (result.getGameStatus() == GameStatus.WIN) {
                        break;
                    }
                }
            }

            if (!game.isWindowClosed()) {
                game.disposeWindow();
            }

            System.out.println("Level " + levelName + " FINAL - Max Completion: " + maxCompletion);

            attemptsPerLevel[i] = attempt;
            maxCompletionPerLevel[i] = maxCompletion;

            runSurvey(scanner, i);

            if (i < LEVEL_LIST.length - 1) {
                waitForReady(scanner);
            }
        }

        System.out.println("\n****************************************************************");
        System.out.println("ALL LEVELS COMPLETED. THANK YOU!");
        System.out.println("****************************************************************");

        printSummary();

        scanner.close();
    }
}
