package search.mcts.selection;

import other.move.Move;
import other.state.State;
import search.mcts.MCTS;
import search.mcts.backpropagation.BackpropagationStrategy;
import search.mcts.nodes.BaseNode;

import java.util.concurrent.ThreadLocalRandom;

public class MultiRave implements SelectionStrategy{

    protected double explorationConstant ;
    private double[] tValue = {
            63.657, 9.925, 5.841, 4.604, 4.032, 3.707, 3.499, 3.355, 3.250, 3.169,
            3.106, 3.055, 3.012, 2.977, 2.947, 2.921, 2.898, 2.878, 2.861, 2.845,
            2.831, 2.819, 2.807, 2.797, 2.787, 2.779, 2.771, 2.763, 2.756, 2.750,
            2.744, 2.738, 2.733, 2.728, 2.724, 2.719, 2.715, 2.712, 2.708, 2.704,
            2.701, 2.698, 2.695, 2.692, 2.690, 2.687, 2.685, 2.682, 2.680, 2.678,
            2.660, 2.648, 2.639, 2.632, 2.626, 2.617, 2.611, 2.603, 2.601, 2.586, 2.581, 2.576
    };

    private double maxsec ;

    private String exconst ;

    private boolean normalize ;

    private double fractionTime ;

    /** Threshold number of playouts that a node must have had for its AMAF values to be used */
    protected final int ref;

    /** Hyperparameter used in computation of weight for AMAF term */
    protected final double bias;

    /** Reference node in current MCTS simulation (one per thread, in case of multi-threaded MCTS) */
    protected ThreadLocal<BaseNode> currentRefNode = ThreadLocal.withInitial(() -> null);




    public MultiRave()
    {

        this.explorationConstant = 0.8 ;
        exconst = String.valueOf(explorationConstant) ;
        this.ref = 0;
        this.bias = 300;
    }



   /* public UCBmulti()
    {
        this(Math.sqrt(2.0));
    }*/

    /**
     * Constructor with parameter for exploration constant
     * @param explorationConstant
     */
    public MultiRave(final double explorationConstant)
    {
        this.explorationConstant = explorationConstant;
        exconst = String.valueOf(explorationConstant) ;
        this.ref = 0;
        this.bias = 300;

    }

    public MultiRave(double explorationConstant, boolean normalize, double fractionTime, double bias){
        this.explorationConstant = explorationConstant ;
        this.normalize = normalize ;
        this.fractionTime = fractionTime ;
        this.ref = 0;
        this.bias = bias;
        System.out.println("Bias Multi = "+bias);
    }

    @Override
    public int select(MCTS mcts, BaseNode current) {

        maxsec = mcts.maxsec;

        final int numChildren = current.numLegalMoves();

        //--------------------------------- Case root node Forced Exploration 10 pulls
        if (current.parent() == null) {

            for (int i = 0; i < numChildren; i++) {
                final BaseNode child = current.childForNthLegalMove(i);
                if (child == null || child.numVisits() < 10) {
                    //  System.out.println("Forced exploration");
                    return i;
                }
            }
            //--------------------------------- Case root node UCBT

            double limitUCBT = maxsec*1000*fractionTime; // stop at 1/3 of thinking time
            // System.out.println(-(System.currentTimeMillis()-mcts.stopUCBmulti));
            //System.out.println(maxsec*1000+(System.currentTimeMillis()-mcts.stopUCBmulti));

            if((maxsec*1000+(System.currentTimeMillis()-mcts.stopUCBmulti))<limitUCBT){
                // System.out.println("UCBTTTTTTTTTTTTTTT");
                //  System.out.println(maxsec*1000+(System.currentTimeMillis()-mcts.stopUCBmulti));
                return selectUCBT(mcts,current) ;}

            //--------------------------------- Case root node POLYUCB start at 1/3 of thinking time
            else{
                //  System.out.println("POLYYYYYYYYYYYYYYY");
                return selectPolyUCB1(mcts,current);

            }
        }

        else {
            //  System.out.println("UCT");
            return selectUCB1Rave(mcts, current);
        }
    }

    @Override
    public int backpropFlags() {
        return BackpropagationStrategy.GRAVE_STATS;
    }

    @Override
    public int expansionFlags() {
        return 0;
    }

    @Override
    public void customise(String[] inputs) {

    }

    // SD from beginning

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

    // SD adapted when there is equality
   /* private double standardDeviation(BaseNode node, int moverAgent){

        double averageScore = node.exploitationScore(moverAgent);
        int numVisits = node.numVisits() ;
        double sd ;
        double sum = 0 ;

        for (int i = 0; i < numVisits; i++) {
            double r = node.getHistoricScore().get(moverAgent).get(i) ;
            //System.out.println(r);

            if(r==0) {
                r = -0.1;
            }
            sum = sum + Math.pow(r-averageScore,2) ;
        }
        sum = sum/(numVisits-1) ;
        sum = Math.sqrt(sum) ;
        sd = sum ;

        return sd ;
    }*/


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

    private double dervieC(BaseNode node, int moveragent, double log ){
        double t = getTscore(node);
        double sd = standardDeviation(node, moveragent) ;
        double c = (t*sd)/(Math.sqrt(log)) ;
        return c ;
    }

    private double avgDerivedC(BaseNode node, int moveragent, double log){
        BaseNode current = node ;
        final int numChildren = current.numLegalMoves();
        double sum = 0 ;
        int  count = 0 ;


        for (int i = 0; i < numChildren; ++i) {
            final BaseNode child = current.childForNthLegalMove(i);
            sum = sum + dervieC(child, moveragent,log) ;
            count = count + 1 ;
        }

        return sum/numChildren ;
    }

    private double normalize(BaseNode child, int moveragent, double log){
        if(normalize)
            return dervieC(child, moveragent,log)+explorationConstant-avgDerivedC(child.parent(), moveragent,log) ;
        else
            return dervieC(child, moveragent, log) ;

    }

    private int selectUCB1Rave(MCTS mcts, BaseNode current){

        int bestIdx = -1;
        double bestValue = Double.NEGATIVE_INFINITY;
        int numBestFound = 0;

        final double parentLog = Math.log(Math.max(1, current.sumLegalChildVisits()));
        final int numChildren = current.numLegalMoves();
        final State state = current.contextRef().state();
        final int moverAgent = state.playerToAgent(state.mover());
        final double unvisitedValueEstimate = current.valueEstimateUnvisitedChildren(moverAgent);

        if (currentRefNode.get() == null || current.numVisits() > ref || current.parent() == null)
            currentRefNode.set(current);

        //System.out.println("selecting for current node = " + current + ". Mover = " + current.contextRef().state().mover());

        for (int i = 0; i < numChildren; ++i)
        {
            final BaseNode child = current.childForNthLegalMove(i);
            final double explore;
            final double meanScore;
            final double meanAMAF;
            final double beta;

            if (child == null)
            {
                meanScore = unvisitedValueEstimate;
                meanAMAF = 0.0;
                beta = 0.0;
                explore = Math.sqrt(parentLog);
            }
            else
            {
                meanScore = child.exploitationScore(moverAgent);
                final Move move = child.parentMove();
                final BaseNode.NodeStatistics graveStats = currentRefNode.get().graveStats(new MCTS.MoveKey(move, current.contextRef().trial().numMoves()));
//        		if (graveStats == null)
//        		{
//        			System.out.println("currentRefNode = " + currentRefNode.get());
//        			System.out.println("stats for " + new MoveKey(move) + " in " + currentRefNode.get() + " = " + graveStats);
//        			System.out.println("child visits = " + child.numVisits());
//        			System.out.println("current.who = " + current.contextRef().containerState(0).cloneWho().toChunkString());
//        			System.out.println("current legal actions = " + Arrays.toString(((Node) current).legalActions()));
//        			System.out.println("current context legal moves = " + current.contextRef().activeGame().moves(current.contextRef()));
//        		}

                final int childVisits = child.numVisits() + child.numVirtualVisits();

                if (graveStats == null)
                {
                    // In single-threaded MCTS this should always be a bug,
                    // but in multi-threaded MCTS it can happen
                    meanAMAF = 0.0;
                    beta = 0.0;
                }
                else
                {
                    final double graveScore = graveStats.accumulatedScore;
                    final int graveVisits = graveStats.visitCount;
                    meanAMAF = graveScore / graveVisits;
                    beta = Math.sqrt(bias/(3.0*childVisits+bias));
                }


                explore = Math.sqrt(parentLog / childVisits);
            }

            final double graveValue = (1.0 - beta) * meanScore + beta * meanAMAF;


            final double ucb1GraveValue = graveValue + explorationConstant * explore;

            if (ucb1GraveValue > bestValue)
            {
                bestValue = ucb1GraveValue;
                bestIdx = i;
                numBestFound = 1;
            }
            else if
            (
                    ucb1GraveValue == bestValue
                            &&
                            ThreadLocalRandom.current().nextInt() % ++numBestFound == 0
            )
            {
                bestIdx = i;
            }
        }

        // This can help garbage collector to clean up a bit more easily
        if (current.childForNthLegalMove(bestIdx) == null)
            currentRefNode.set(null);

        return bestIdx;
    }

    private int selectUCBT(MCTS mcts, BaseNode current){
        int bestIdx = -1;
        double bestValue = Double.NEGATIVE_INFINITY;
        int numBestFound = 0;

        final double parentLog = Math.log(Math.max(1, current.sumLegalChildVisits()));
        final int numChildren = current.numLegalMoves();
        final State state = current.contextRef().state();
        final int moverAgent = state.playerToAgent(state.mover());
        final double unvisitedValueEstimate = current.valueEstimateUnvisitedChildren(moverAgent);


        for (int i = 0; i < numChildren; ++i) {
            final BaseNode child = current.childForNthLegalMove(i);

            if(child==null) {
                return i;
            }
            if (child.numVisits() < 2) {
                return i;
            }
        }

        for (int i = 0; i < numChildren; ++i) {
            final BaseNode child = current.childForNthLegalMove(i);

            // average score of the node
            double exploit = child.exploitationScore(moverAgent);
            double sd = standardDeviation(child,moverAgent) ;
            if(sd==0){
                sd = sd + 0.00001 ;
            }


            // computation of ucbt = (mean score) + (t-score)*(sdt/sqrt(numvisited of this node))
            final int numVisits = child.numVisits() + child.numVirtualVisits();
            double ucbt = exploit + getTscore(child) *sd/Math.sqrt(numVisits) ;

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

        return bestIdx ;
    }

    private int selectPolyUCB1(MCTS mcts, BaseNode current){

        int bestIdx = -1;
        double bestValue = Double.NEGATIVE_INFINITY;
        int numBestFound = 0;

        final double parentLog = Math.log(Math.max(1, current.sumLegalChildVisits()));
        final int numChildren = current.numLegalMoves();
        final State state = current.contextRef().state();
        final int moverAgent = state.playerToAgent(state.mover());
        final double unvisitedValueEstimate = current.valueEstimateUnvisitedChildren(moverAgent);

        for (int i = 0; i < numChildren; ++i) {



            final BaseNode child = current.childForNthLegalMove(i);
            final double exploit;
            final double explore;

            exploit = child.exploitationScore(moverAgent);
            final int numVisits = child.numVisits() + child.numVirtualVisits();
            explore = Math.sqrt(parentLog / numVisits);

            final double ucb1Value = exploit + normalize(child,moverAgent,parentLog) * explore;

            if (ucb1Value > bestValue)
            {
                bestValue = ucb1Value;
                bestIdx = i;
                numBestFound = 1;
            }
            else if
            (
                    ucb1Value == bestValue
                            &&
                            ThreadLocalRandom.current().nextInt() % ++numBestFound == 0
            )
            {
                bestIdx = i;
            }

        }
        return bestIdx ;
    }

}