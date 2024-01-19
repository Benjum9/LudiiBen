import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import game.Game;
import other.AI;
import other.GameLoader;
import other.context.Context;
import other.model.Model;
import other.trial.Trial;
import search.mcts.MCTS;

public class UCBvsRAVE {

    private static final int NUM_TRIALS = 100;

    private static final String GAME = "Pentago.lud" ;

    private static final double thinkingTime = 10.0 ;

    public static void main(final String[] args) {

        String filename = "/home/I6256403/project/UCBvsRAVEPentago.txt";
        // String filename = "/Users/benjamingauthier/Desktop/multiBamboo1not.txt";


        File file = new File(filename);

        // Check if the file exists and delete it if it does
        if (file.exists()) {
            boolean isDeleted = file.delete();
            if (isDeleted) {
                System.out.println("Existing file was deleted successfully.");
            } else {
                System.out.println("Failed to delete the existing file, please check permissions or if the file is in use.");
                // You might want to add more handling here, like exiting the program or throwing an exception
            }
        } else {
            System.out.println("No existing file found, a new one will be created.");
        }

        try (FileWriter fw = new FileWriter(filename, true);  // true will append to file, false (or omitted) will overwrite
             PrintWriter pw = new PrintWriter(fw)) {

            // Load our game -- we only need to do this once, and can use it for many trials
            final Game game = GameLoader.loadGameFromName(GAME);
            System.out.println(GAME);
            System.out.println(NUM_TRIALS);

            // Prepare Context and Trial objects; these are also re-usable by resetting them,
            // but we'd have to copy them if we wanted to preserve all of the different objects
            // corresponding to different trials
            final Trial trial = new Trial(game);
            final Context context = new Context(game, trial);

            AI uct = MCTS.createUCT(1.0);
            //AI multi = MCTS.createMAST(0.8) ;
            //AI multi = MCTS.createMultiMast(0.4,false, 0.4) ;
            AI multi = MCTS.createUCTRAVE(1.0, 300) ;

            pw.println("Num Trials : "+NUM_TRIALS);
            pw.println("Game : "+ GAME);
            pw.println("Thinking Time : "+ thinkingTime);
            pw.flush();



            int numUCT = 0;
            int numMulti = 0;

            for (int j = 1; j < NUM_TRIALS + 1; j++) {


                // restart trail and context for new trial
                trial.reset(game);
                context.reset();

                // ais that will play the trial
                final List<AI> ais = new ArrayList<AI>();
                ais.add(null);

                // every trials first ai to play is changed
                if (j % 2 == 0) {
                    ais.add(uct);
                    ais.add(multi);
                } else {
                    ais.add(multi);
                    ais.add(uct);
                }


                // This starts a new trial (resetting the Context and Trial objects if necessary)
                game.start(context);
                System.out.println("Starting a new trial!" + " number: " + j);

                // for all AIs at the start of every new trial
                for (int p = 1; p <= game.players().count(); ++p) {
                    ais.get(p).initAI(game, p);
                }


                // This "model" object lets us go through a trial step-by-step using a single API
                // that works correctly for alternating-move as well as simultaneous-move games
                final Model model = context.model();


                // We keep looping for as long as the trial is not over
                while (!trial.over()) {
                    // This call simply takes a single "step" in the game, using the list of AIs we give it,
                    // and 1.0 second of "thinking time" per move.
                    //
                    // A step is a single move in an alternating-move game (by a single player), or a set of
                    // moves (one per active player) in a simultaneous-move game.
                    //model.startNewStep(context, ais, -1, 4000, -1, 0.0);
                    model.startNewStep(context, ais, thinkingTime);
                }



                // When we reach this code, we know that the trial is over and we can see what ranks the
                // different players achieved
                final double[] ranking = trial.ranking();

                for (int p = 1; p <= game.players().count(); ++p) {

                    if (ais.get(p).friendlyName().startsWith("UCT") && ranking[p] == 1) {
                        numUCT = numUCT + 1;
                        //System.out.println("numUCT "+numUCT);
                    }
                    if (ais.get(p).friendlyName().startsWith("rave") && ranking[p] == 1) {
                        numMulti = numMulti + 1;
                        // System.out.println("numUCT2 "+numUCT2);
                    }


                    if (ais.get(p).friendlyName().startsWith("UCT")) {
                        System.out.println("Agent " + context.state().playerToAgent(p) + " " + ais.get(p).friendlyName() + " " + "achieved rank: " + ranking[p]);
                        System.out.println("Mean " + ais.get(p).friendlyName() + " = " + (double) numUCT / j);
                        System.out.println("Wins "+numUCT);
                        if (j == NUM_TRIALS) {
                            pw.println("Mean " + ais.get(p).friendlyName() + " = " + (double) numUCT / (double) NUM_TRIALS);
                            pw.println("Wins " + ais.get(p).friendlyName() + " = " + numUCT);
                            pw.flush();
                            if (p == 2) {
                                pw.println("---------------------------");
                                pw.flush();
                            }
                        }
                    }else {
                        System.out.println("Agent " + context.state().playerToAgent(p) + " " + ais.get(p).friendlyName() + " " + "achieved rank: " + ranking[p]);
                        System.out.println("Mean " + ais.get(p).friendlyName() + " = " + (double) numMulti / j);
                        System.out.println("Wins "+numMulti);
                        if (j == NUM_TRIALS) {
                            pw.println("Mean " + ais.get(p).friendlyName() + " = " + (double) numMulti / (double) NUM_TRIALS);
                            pw.println("Wins " + ais.get(p).friendlyName() + " = " + numMulti);
                            pw.flush();
                            if (p == 2) {
                                pw.println("---------------------------");
                                pw.flush();
                            }
                        }
                    }
                }
            }


        } catch (IOException e) {
            // Handle the exception, typically this means printing an error message
            System.err.println("An I/O error occurred: " + e.getMessage());
        }
    }
}
