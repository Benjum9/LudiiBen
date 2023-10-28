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
import search.minimax.AlphaBetaSearch;
import utils.RandomAI;

/**
 * Example class showing how we can run trials in Ludii
 *
 * @author Dennis Soemers
 */
public class RunningTrials {

    /**
     * The number of trials that we'd like to run
     */
    private static final int NUM_TRIALS = 50;

    /**
     * Main method
     *
     * @param args Command-line arguments.
     */
    public static void main(final String[] args) {

        // Specify the filename
        String filename = "/home/I6256403/project/results.txt";

        File file = new File(filename);

        // Check if the file exists and delete it if it does
        if(file.exists()) {
            boolean isDeleted = file.delete();
            if(isDeleted) {
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
            final Game game = GameLoader.loadGameFromName("Hex.lud");

            // Prepare Context and Trial objects; these are also re-usable by resetting them,
            // but we'd have to copy them if we wanted to preserve all of the different objects
            // corresponding to different trials
            final Trial trial = new Trial(game);
            final Context context = new Context(game, trial);

            //------------------------------------------------------------------------ UCT with different c value
            final List<AI> ucts = new ArrayList<AI>();
            double exconst = 0.1;
            while (exconst < 2) {
                ucts.add(MCTS.createUCT(exconst));
                exconst = exconst + 0.1;
            }

            //-----------------------------------------------------------------------

            for (int i = 1; i < ucts.size() + 1; i++) {

                pw.println("UCT "+String.valueOf((double)i/(double)10) + " VS " + "UCBT " +"NUM Trials :"+NUM_TRIALS) ;
                pw.flush();
                int numUCT = 0;
                int numUCBT = 0;

                // Create AI objects that we'd like to use to play our Trials
                // Here we just use Ludii's built in Random AI, because it's fast
                // Ludii uses 1-based indexing for players, so we insert a null in the list first

                // Now we play through multiple trials
                for (int j = 1; j < NUM_TRIALS + 1; ++j) {

                    final List<AI> ais = new ArrayList<AI>();
                    ais.add(null);


                    if (j % 2 == 0) {
                        ais.add(ucts.get(i));
                        ais.add(MCTS.createUCBT());
                    } else {
                        ais.add(MCTS.createUCBT());
                        ais.add(ucts.get(i));
                    }

                    // This starts a new trial (resetting the Context and Trial objects if necessary)
                    game.start(context);
                    System.out.println("Starting a new trial!");

                    // Random AI technically doesn't require initialisation, but it's good practice to do so
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
                        model.startNewStep(context, ais, 1.0);
                    }

                    // When we reach this code, we know that the trial is over and we can see what ranks the
                    // different players achieved
                    final double[] ranking = trial.ranking();

                    for (int p = 1; p <= game.players().count(); ++p) {

                        if (ais.get(p).friendlyName().equals("UCT") && ranking[p] == 1) {
                            numUCT = numUCT + 1;
                            // System.out.println("UCT won");
                        }
                        if (ais.get(p).friendlyName().equals("UCBT") && ranking[p] == 1) {
                            numUCBT = numUCBT + 1;
                            //System.out.println("UCBT won");
                        }
                        // Here we print the rankings as achieved by every agent, where
                        // the "agent indices" correspond to the order of agents prior
                        // to the game's start. This order will usually still be the same
                        // at the end of a trial, but may be different if any swaps happened.
                        //
                        // ranking[p] tells you which rank was achieved by the player
                        // who controlled the p'th "colour" at the end of a trial, and
                        // trial.state().playerToAgent(p) tells you which agent (in the list
                        // of AI objects) controls that colour at the end of the trial.
                        if (ais.get(p).friendlyName().equals("UCT")) {
                            System.out.println("Agent " + context.state().playerToAgent(p) + " " + ais.get(p).friendlyName() + " " + String.valueOf((double) (i / 10.0)) + " " + " achieved rank: " + ranking[p]);
                            System.out.println("Mean UCT =" + String.valueOf((double) numUCT / (double) j));
                        } else {
                            System.out.println("Agent " + context.state().playerToAgent(p) + " " + ais.get(p).friendlyName() + " achieved rank: " + ranking[p]);
                            System.out.println("Mean UCBT =" + String.valueOf((double) numUCBT / (double) j));
                        }
                    }
                    System.out.println();

                }
                System.out.println("-----------------------------------");
                pw.println("Mean UCT = " + String.valueOf((double) numUCT / (double) NUM_TRIALS));
                pw.flush();
                pw.println("Mean UCBT = " + String.valueOf((double) numUCBT / (double) NUM_TRIALS));
                pw.flush();

            }
        }
        catch (IOException e) {
            // Handle the exception, typically this means printing an error message
            System.err.println("An I/O error occurred: " + e.getMessage());
        }
    }
}
