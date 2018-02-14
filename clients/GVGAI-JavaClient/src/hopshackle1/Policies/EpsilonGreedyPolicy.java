package hopshackle1.Policies;

import java.util.*;

import hopshackle1.*;
import hopshackle1.RL.ActionValue;
import hopshackle1.RL.ActionValueFunctionApproximator;
import serialization.SerializableStateObservation;
import serialization.Types;

public class EpsilonGreedyPolicy implements Policy {

    private double epsilon;
    private Random rnd = new Random(56);
    private boolean debug = false;
    private ActionValueFunctionApproximator theta;
    private EntityLog logFile;
    ;

    public EpsilonGreedyPolicy(ActionValueFunctionApproximator kernel, double exploration) {
        epsilon = exploration;
        theta = kernel;
        if (debug) logFile = new EntityLog("EpsilonGreedy");
    }

    @Override
    public Types.ACTIONS chooseAction(List<Types.ACTIONS> availableActions, SerializableStateObservation sso) {
        ActionValue choice = null;

        if (rnd.nextDouble() < epsilon) {
            Types.ACTIONS actionChosen = availableActions.get(rnd.nextInt(availableActions.size()));
            if (debug)
                logFile.log(String.format("Chooses %s with random exploration", choice.toString()));
            return actionChosen;
        }
        choice = theta.valueOfBestAction(sso, availableActions);
        if (debug) {
            logFile.log(String.format("Chooses %s greedily", choice.action.toString()));
            logFile.flush();
        }

        return choice.action;
    }

}
