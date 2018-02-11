package hopshackle1.Policies;

import java.util.*;

import hopshackle1.*;
import serialization.Types;
import utils.ElapsedCpuTimer;

public class EpsilonGreedyPolicy implements Policy {

    private double epsilon;
    private Random rnd = new Random(56);
    private boolean debug = false;
    private PolicyKernel theta;
    private EntityLog logFile;
    ;

    public EpsilonGreedyPolicy(PolicyKernel kernel, double exploration) {
        epsilon = exploration;
        theta = kernel;
        if (debug) logFile = new EntityLog("EpsilonGreedy");
    }

    @Override
    public Types.ACTIONS chooseAction(List<Types.ACTIONS> availableActions, State state) {
        ActionValue choice = null;

        if (rnd.nextDouble() < epsilon) {
            Types.ACTIONS actionChosen = availableActions.get(rnd.nextInt(availableActions.size()));
            if (debug)
                logFile.log(String.format("Chooses %s with random exploration", choice.toString()));
            return actionChosen;
        }
        choice = theta.valueOfBestAction(state, availableActions);
        if (debug) {
            logFile.log(String.format("Chooses %s greedily", choice.action.toString()));
            logFile.flush();
        }

        return choice.action;
    }

}
