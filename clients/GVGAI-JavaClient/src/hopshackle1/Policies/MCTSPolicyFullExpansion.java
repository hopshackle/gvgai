package hopshackle1.Policies;

import hopshackle1.Agent;
import hopshackle1.EntityLog;
import hopshackle1.RL.ActionValueFunctionApproximator;
import hopshackle1.models.BehaviourModel;
import hopshackle1.models.GameStatusTracker;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import serialization.Types;

import java.util.*;

public class MCTSPolicyFullExpansion extends MCTSPolicy {

    public MCTSPolicyFullExpansion(BehaviourModel worldModel, ActionValueFunctionApproximator leafValuer, double C, double defaultCoeff, double temp) {
        super(worldModel, leafValuer, C, defaultCoeff, temp);
    }

    protected Types.ACTIONS expandNode(int state, Types.ACTIONS lastAction) {
        // should be here because <state> does not have all it's actions instantiated
        // so, we expand all of them, and pick the best
        List<Types.ACTIONS> permittedActions = getPossibleActionsAfter(lastAction);
        Types.ACTIONS chosen = Types.ACTIONS.ACTION_NIL;
        double maxValue = Double.NEGATIVE_INFINITY;
        GameStatusTracker baseGST = new GameStatusTracker(gameState(state));
        baseGST.rollForwardSprites(model, useMAP);

        for (Types.ACTIONS a : permittedActions) {
            int newStateRef = nextStateCounter();
            GameStatusTracker newGST = new GameStatusTracker(baseGST);
            newGST.rollForwardAvatar(model, a, true);
            updateGameState(newStateRef, newGST);
            double value = evalFunction.value(newGST.getCurrentSSO());
            updateTree(new Pair(state, a), new Triplet(newStateRef, 1, value));
            if (debug) logFile.log(String.format("Expanding %s with value %.2f", a, value));
            if (value > maxValue) {
                maxValue = value;
                chosen = a;
            }
        }
        return chosen;
    }

}
