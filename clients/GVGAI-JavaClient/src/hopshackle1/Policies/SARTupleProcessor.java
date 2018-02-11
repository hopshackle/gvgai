package hopshackle1.Policies;

import hopshackle1.*;
import org.javatuples.*;
import java.util.*;
import serialization.Types.*;

public interface SARTupleProcessor {

    /*
    Will always need to be called once the full Trajectory is obtained
     */
    public List<Double> processRewards(LinkedList<SARTuple> data);

    /*
    Can be called after each action, to calculate the State representation of an SSO
     */
    public State calculateState(SARTuple tuple, List<FeatureSet> featureSets);

    /*
    Instead of calling the previous two methods, we can just call this on the full Trajectory
    It moves all the computational expenditure to the end of game phase
     */
    public List<Triplet<State, ACTIONS, Double>> processTrajectory(LinkedList<SARTuple> data, List<FeatureSet> featureSets);

}
