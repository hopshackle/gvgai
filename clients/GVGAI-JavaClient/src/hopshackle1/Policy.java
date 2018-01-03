package hopshackle1;

import java.util.*;

import serialization.Types.*;

public class Policy {

    private List<Map<Integer, Double>> coefficients;
    private List<ACTIONS> actions;
    private Random rdm = new Random(45);

    public Policy() {
        coefficients = new ArrayList<>();
        actions = new ArrayList<>();
    }

    public ACTIONS chooseAction(List<ACTIONS> actions, State state) {
        double[] pdf = getProbabilityDistributionOverActions(actions, state.features);
        double roll = rdm.nextDouble();
        double cdf = 0.0;
        for (int i = 0; i < pdf.length; i++) {
            cdf += pdf[i];
            if (roll <= cdf) return actions.get(i);
        }
        throw new AssertionError("Should not be able to get here in theory. " + HopshackleUtilities.formatArray(pdf, ", ", "%.5f"));
    }

    public double[] getProbabilityDistributionOverActions(List<ACTIONS> actions, Map<Integer, Double> stateFeatures) {
        double[] retValue = new double[actions.size()];
        int count = 0;
        for (ACTIONS a : actions) {
            int index = getIndexForAction(a);
            for (int feature : stateFeatures.keySet()) {
                double coeff = getCoeffFor(index, feature);
                retValue[count] += stateFeatures.get(feature) * coeff;
            }
            count++;
        }
        retValue = HopshackleUtilities.expNormalise(retValue);
        return retValue;
    }

    private int getIndexForAction(ACTIONS a) {
        int index = actions.indexOf(a);
        if (index == -1) {
            index = actions.size();
            actions.add(a);
            coefficients.add(new HashMap<>());
        }
        return index;
    }


    private double getCoeffFor(int index, int feature) {
        Map<Integer, Double> actionCoeffs = coefficients.get(index);
        if (!actionCoeffs.containsKey(feature)) {
            actionCoeffs.put(feature, 0.0);
        }
        return actionCoeffs.get(feature);
    }
}
