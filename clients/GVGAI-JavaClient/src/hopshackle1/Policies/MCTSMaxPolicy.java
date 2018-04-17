package hopshackle1.Policies;

import hopshackle1.RL.ActionValueFunctionApproximator;
import hopshackle1.models.*;
import org.javatuples.*;
import serialization.Types;

import java.util.*;

public class MCTSMaxPolicy extends MCTSPolicyFullExpansion {

    private double MAX_WEIGHTING = 0.25;

    public MCTSMaxPolicy(BehaviourModel worldModel, ActionValueFunctionApproximator leafValuer, double C, double defaultCoeff, double temp) {
        super(worldModel, leafValuer, C, defaultCoeff, temp);
    }


    protected void backPropagate(double finalValue, LinkedList<Pair<Integer, Types.ACTIONS>> trajectory) {
        Iterator<Pair<Integer, Types.ACTIONS>> backwards = trajectory.descendingIterator();
        while (backwards.hasNext()) {
            Pair<Integer, Types.ACTIONS> node = backwards.next();
            Triplet<Integer, Integer, Double> data = treeEntryFor(node);    // where we end up from node

            Types.ACTIONS bestOnwardAction = bestActionFrom(data.getValue0(), getPossibleActionsAfter(node.getValue1())); // best value to take from node
            // TODO: Not if this is the end of the trajectory
            Triplet<Integer, Integer, Double> nextNodeData = treeEntryFor(new Pair(data.getValue0(), bestOnwardAction));
            double bestOnwardValue = finalValue;
            if (nextNodeData != null && nextNodeData.getValue1() > 1) {
                bestOnwardValue = nextNodeData.getValue2();
            }
            double valueToUse = (1.0 - MAX_WEIGHTING) * finalValue + MAX_WEIGHTING * bestOnwardValue;

            if (debug)
                logFile.log(String.format("Back-propagating %.2f (F: %.2f, MAX: %.2f) from %s to %s", valueToUse, finalValue, bestOnwardValue, node, data));
            int currentVisits = data.getValue1();
            double newValue = (data.getValue2() * currentVisits + valueToUse) / (currentVisits + 1);
            Triplet<Integer, Integer, Double> newData = new Triplet(data.getValue0(), currentVisits + 1, newValue);
            updateTree(node, newData);
        }
    }

}
