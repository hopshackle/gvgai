package hopshackle1.Policies;

import java.util.*;

import hopshackle1.*;
import hopshackle1.RL.ActionValue;
import hopshackle1.RL.ActionValueFunctionApproximator;
import hopshackle1.models.GameStatusTracker;
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
    public double[] pdfOver(List<Types.ACTIONS> availableActions, GameStatusTracker gst) {
        ActionValue choice = theta.valueOfBestAction(gst, availableActions);
        double[] retValue = new double[availableActions.size()];
        for (int i = 0; i < availableActions.size(); i++) {
            retValue[i] = epsilon / availableActions.size();
            if (choice.action == availableActions.get(i))
                retValue[i] += (1.0 - epsilon);
        }
        return retValue;
    }

    @Override
    public Types.ACTIONS chooseAction(List<Types.ACTIONS> availableActions, GameStatusTracker gst) {
        ActionValue choice = null;

        if (rnd.nextDouble() < epsilon) {
            Types.ACTIONS actionChosen = availableActions.get(rnd.nextInt(availableActions.size()));
            if (debug)
                logFile.log(String.format("Chooses %s with random exploration", choice.toString()));
            return actionChosen;
        }
        choice = theta.valueOfBestAction(gst, availableActions);
        if (debug) {
            logFile.log(String.format("Chooses %s greedily", choice.action.toString()));
            logFile.flush();
        }

        return choice.action;
    }

}
