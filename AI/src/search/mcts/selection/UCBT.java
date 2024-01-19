package search.mcts.selection;

import other.state.State;
import search.mcts.MCTS;
import search.mcts.nodes.BaseNode;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class UCBT implements SelectionStrategy {
    private int numSelect = 0 ;

    double[] tValue = {
            63.657, 9.925, 5.841, 4.604, 4.032, 3.707, 3.499, 3.355, 3.250, 3.169,
            3.106, 3.055, 3.012, 2.977, 2.947, 2.921, 2.898, 2.878, 2.861, 2.845,
            2.831, 2.819, 2.807, 2.797, 2.787, 2.779, 2.771, 2.763, 2.756, 2.750,
            2.744, 2.738, 2.733, 2.728, 2.724, 2.719, 2.715, 2.712, 2.708, 2.704,
            2.701, 2.698, 2.695, 2.692, 2.690, 2.687, 2.685, 2.682, 2.680, 2.678,
            2.660, 2.648, 2.639, 2.632, 2.626, 2.617, 2.611, 2.603, 2.601, 2.586, 2.581, 2.576
    };

    @Override
    public int select(MCTS mcts, BaseNode current) {



        int bestIdx = -1; // index of best child
        double bestValue = Double.NEGATIVE_INFINITY; // best UCBT value
        int numBestFound = 0; // num of child with best UCBT value

        final int numChildren = current.numLegalMoves(); //num of children of current
        final State state = current.contextRef().state(); //state of current node
        final int moverAgent = state.playerToAgent(state.mover()); // agent index ?
        final double unvisitedValueEstimate = current.valueEstimateUnvisitedChildren(moverAgent); // who to move


        for (int i = 0; i < numChildren; ++i) {
            final BaseNode child = current.childForNthLegalMove(i);
            // Force exploration for every child
            if(child==null) {
                // System.out.println("yes");
                return i;

            }
            // Force num of visit minimum 2 before computing UCBT value
            if (child.numVisits() < 2) {
                //System.out.println("yesyes");
                return i;
            }
        }




        for (int i = 0; i < numChildren; ++i) {


            final BaseNode child = current.childForNthLegalMove(i);



          /*  // Force exploration for every child
                if(child==null || child.numVisits()==0) {
                    return i;
                }


                // Force num of visit minimum 2 before computing UCBT value
                if (child.numVisits() < 2) {
                    return i;
                }*/


            // average score of the node
            double exploit = child.exploitationScore(moverAgent);
            double sd = standardDeviation(child,moverAgent) ;
            if(sd==0){
                //  System.out.println("yes");
                sd = sd + 0.00001 ;
            }


            // computation of ucbt = (mean score) + (t-score)*(sdt/sqrt(numvisited of this node))
            final int numVisits = child.numVisits() + child.numVirtualVisits();

            double ucbt = exploit + getTscore(child) *sd/Math.sqrt(numVisits) ;
            //System.out.println(ucbt);

         /*   System.out.println(child.getHistoricScore().get(moverAgent).toString());
            System.out.println("Num Visit: "+numVisits);
            System.out.println("Num visit 2: "+child.numVisits());
            System.out.println("avg: "+child.exploitationScore(moverAgent));
            System.out.println("sd: "+standardDeviation(child,moverAgent));
            System.out.println("T-score: "+getTscore(child));
            System.out.println("ucbt: "+ ucbt);*/


            if (ucbt > bestValue)
            {
                bestValue = ucbt;
                bestIdx = i;
                numBestFound = 1;

            }
            else if
            (
                    ucbt == bestValue
                            &&
                            ThreadLocalRandom.current().nextInt() % ++numBestFound == 0
            )
            {
                bestIdx = i;

            }
        }

        // System.out.println("no");
        //  System.out.println(current.childForNthLegalMove(bestIdx).toString());
        return bestIdx ;
    }

    @Override
    public int backpropFlags() {
        return 0;
    }

    @Override
    public int expansionFlags() {
        return 0;
    }

    @Override
    public void customise(String[] inputs) {
        System.err.println("UCB1 ignores unknown customisation: " + Arrays.toString(inputs));
    }

    private double standardDeviation(BaseNode node, int moverAgent){

        double averageScore = node.exploitationScore(moverAgent);
        int numVisits = node.numVisits() ;
        double sd ;
        double sum = 0 ;

        for (int i = 0; i < numVisits; i++) {
            sum = sum + Math.pow(node.getHistoricScore().get(moverAgent).get(i)-averageScore,2) ;
        }
        sum = sum/(numVisits-1) ;
        sum = Math.sqrt(sum) ;
        sd = sum ;

        return sd ;
    }

    private double getTscore(BaseNode node){
        int numVisits = node.numVisits();

        int df = numVisits - 1 ;
        double t ;

        if (df <50)
            t = tValue[df-1] ;
        else if (df<60)
            t = tValue[50] ;
        else if(df<70)
            t = tValue[51] ;
        else if(df<80)
            t = tValue[52] ;
        else if(df<90)
            t = tValue[53] ;
        else if(df<100)
            t = tValue[54] ;
        else if(df<120)
            t = tValue[55] ;
        else if(df<140)
            t = tValue[56] ;
        else if(df<160)
            t = tValue[57] ;
        else if(df<180)
            t = tValue[58] ;
        else if(df<=200)
            t = tValue[59] ;
        else t = 2.326 ;

        return t ;
    }

}

