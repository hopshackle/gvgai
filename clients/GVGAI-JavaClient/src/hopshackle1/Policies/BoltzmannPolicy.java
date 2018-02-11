package hopshackle1.Policies;

import hopshackle1.*;
import serialization.Types.*;

import java.util.*;

public class BoltzmannPolicy implements Policy {

    private double temperature;
    private PolicyKernel theta;
    private boolean debug = false;
    private EntityLog logFile;
    private Random rnd = new Random(45);

    public BoltzmannPolicy(PolicyKernel kernel, double temperature) {
        this.temperature = temperature;
        theta = kernel;
        if (debug) logFile = new EntityLog("Boltzmann");
    }

    public double[] pdfOver(State state, List<ACTIONS> actions) {
        double[] retValue = new double[actions.size()];
        int count = 0;
        for (ACTIONS a : actions) {
            retValue[count] = theta.value(state, a);
            retValue[count] /= temperature;
            count++;
        }
        retValue = HopshackleUtilities.expNormalise(retValue);
        return retValue;
    }

    @Override
    public ACTIONS chooseAction(List<ACTIONS> availableActions, State state) {
        ActionValue choice = null;

        double[] pdf = pdfOver(state, availableActions);
        double roll = rnd.nextDouble();
        if (debug) {
            logFile.log("PDF for actions is:");
            for (int i = 0; i < pdf.length; i++) {
                logFile.log(String.format("\t%.3f\t%s", pdf[i], availableActions.get(i)));
            }
        }
        double cdf = 0.0;
        for (int i = 0; i < pdf.length; i++) {
            cdf += pdf[i];
            if (roll <= cdf) {
                if (debug) logFile.log("Chooses action " + availableActions.get(i));
                return availableActions.get(i);
            }
        }

        return choice.action;
    }

}
