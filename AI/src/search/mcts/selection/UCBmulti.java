package search.mcts.selection;

import other.state.State;
import search.mcts.MCTS;
import search.mcts.nodes.BaseNode;

import java.util.concurrent.ThreadLocalRandom;

public class UCBmulti implements SelectionStrategy{

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


    public UCBmulti()
    {

        this.explorationConstant = 0.32 ;
        exconst = String.valueOf(explorationConstant) ;
    }



   /* public UCBmulti()
    {
        this(Math.sqrt(2.0));
    }*/

    /**
     * Constructor with parameter for exploration constant
     * @param explorationConstant
     */
    public UCBmulti(final double explorationConstant)
    {
        this.explorationConstant = explorationConstant;
        exconst = String.valueOf(explorationConstant) ;

    }

    public String getExconst(){
        return exconst ;
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
                    // System.out.println("Forced exploration");
                    return i;
                }
            }
            //--------------------------------- Case root node UCBT

            double limitUCBT = maxsec*1000/3*2 ; // stop at 1/3 of thinking time

            if(-(System.currentTimeMillis()-mcts.stopUCBmulti)>limitUCBT){
                return selectUCBT(mcts,current) ;}

            //--------------------------------- Case root node POLYUCB start at 2/3 of thinking time
            else{
                return selectPolyUCB1(mcts,current);
            }
        }

        else {
            return selectUCB1(mcts, current);
        }
    }


    public int select1(MCTS mcts, BaseNode current) {

        //System.out.println(-(System.currentTimeMillis()-mcts.stopUCBmulti));

        int bestIdx = -1;
        double bestValue = Double.NEGATIVE_INFINITY;
        int numBestFound = 0;

        final double parentLog = Math.log(Math.max(1, current.sumLegalChildVisits()));
        final int numChildren = current.numLegalMoves();
        final State state = current.contextRef().state();
        final int moverAgent = state.playerToAgent(state.mover());
        final double unvisitedValueEstimate = current.valueEstimateUnvisitedChildren(moverAgent);



        //--------------------------------- Case root node Use PolyUCB1 / UCBT
        if(current.parent()==null){
            // System.out.println("yes");
            //----------------------------------------------iterration trought every child
            for (int i = 0; i < numChildren; i++) {
                //------------------------------------------forced exploration
                final BaseNode child = current.childForNthLegalMove(i);
                if(child==null || child.numVisits()<10) {
                    return i;
                }
            }

            //----------------------------------------------------PolyUCB1
            for (int i = 0; i < numChildren; ++i) {

                final BaseNode child = current.childForNthLegalMove(i);
                // System.out.print(child.numVisits());
                //  System.out.println(" "+child.exploitationScore(moverAgent));
                final double exploit;
                final double explore;

                exploit = child.exploitationScore(moverAgent);
                final int numVisits = child.numVisits() + child.numVirtualVisits();
                explore = Math.sqrt(parentLog / numVisits);



                //  final double ucb1Value = exploit + dervieC(child,moverAgent,parentLog) * explore;
                final double ucb1Value = exploit + normalize(child,moverAgent,parentLog) * explore;
                // System.out.println(dervieC(child,moverAgent,parentLog));

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
            //  System.out.println("--------------------------------------------");
            return bestIdx ;
        }


        //-------------------------------------------------------No root Use UCB1
        else{


            for (int i = 0; i < numChildren; ++i)
            {
                final BaseNode child = current.childForNthLegalMove(i);
                final double exploit;
                final double explore;

                if (child == null)
                {
                    exploit = unvisitedValueEstimate;
                    explore = Math.sqrt(parentLog);
                }
                else
                {
                    exploit = child.exploitationScore(moverAgent);
                    final int numVisits = child.numVisits() + child.numVirtualVisits();
                    explore = Math.sqrt(parentLog / numVisits);
                }

                final double ucb1Value = exploit + explorationConstant * explore;

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
            return bestIdx;
        }
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

    private double dervieC(BaseNode node, int moveragent, double log ){
        double t = getTscore(node );
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
        return dervieC(child, moveragent,log)+explorationConstant-avgDerivedC(child.parent(), moveragent,log) ;
    }

    private int selectUCB1(MCTS mcts, BaseNode current){

        int bestIdx = -1;
        double bestValue = Double.NEGATIVE_INFINITY;
        int numBestFound = 0;

        final double parentLog = Math.log(Math.max(1, current.sumLegalChildVisits()));
        final int numChildren = current.numLegalMoves();
        final State state = current.contextRef().state();
        final int moverAgent = state.playerToAgent(state.mover());
        final double unvisitedValueEstimate = current.valueEstimateUnvisitedChildren(moverAgent);

        for (int i = 0; i < numChildren; ++i)
        {
            final BaseNode child = current.childForNthLegalMove(i);
            final double exploit;
            final double explore;

            if (child == null)
            {
                exploit = unvisitedValueEstimate;
                explore = Math.sqrt(parentLog);
            }
            else
            {
                exploit = child.exploitationScore(moverAgent);
                final int numVisits = child.numVisits() + child.numVirtualVisits();
                explore = Math.sqrt(parentLog / numVisits);
            }

            final double ucb1Value = exploit + explorationConstant * explore;

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