package hopshackle1;

import serialization.Types;
import serialization.Types.*;

import java.util.*;

public class PolicyCoeffCore {

    private List<Map<Integer, Double>> coefficients;
    private List<ACTIONS> actions;
    private boolean debug;
    private EntityLog logFile;
    private String name;

    public PolicyCoeffCore(String name, boolean debug) {
        this.name = name;
        this.debug = debug;
        if (debug) logFile = new EntityLog(name);
        coefficients = new ArrayList<>();
        coefficients.add(new HashMap());    // the Value function is always at index = 0
        actions = new ArrayList<>();
        actions.add(null);  // a dummy placeholder for Value, so that coefficients and actions use the same index
    }

    public void refreshFrom(PolicyCoeffCore pcc) {
        if(debug) logFile.log("Refreshing from " + pcc.name);
        actions = HopshackleUtilities.cloneList(pcc.actions);
        coefficients = new ArrayList();
        for (int i = 0; i < pcc.coefficients.size(); i++) {
            Map<Integer, Double> actionTheta = pcc.coefficients.get(i);
            Map<Integer, Double> cloneActionTheta = new HashMap();
            for (Integer f : actionTheta.keySet()) {
                cloneActionTheta.put(f, actionTheta.get(f));
            }
            coefficients.add(cloneActionTheta);
        }
    }

    public int getIndexFor(ACTIONS a) {
        int index = actions.indexOf(a);
        if (index == -1) {
            index = actions.size();
            actions.add(a);
            coefficients.add(new HashMap<>());
        }
        return index;
    }

    public double getCoeffFor(int index, int feature) {
        Map<Integer, Double> actionCoeffs = coefficients.get(index);
        if (!actionCoeffs.containsKey(feature)) {
            actionCoeffs.put(feature, 0.0);
        }
        return actionCoeffs.get(feature);
    }

    public void setCoeffFor(int index, int feature, double value) {
        Map<Integer, Double> actionCoeffs = coefficients.get(index);
        actionCoeffs.put(feature, value);
    }

    public void clearCoeffs(int index) {
        coefficients.set(index, new HashMap());
    }

    public double valueState(State state) {
        double retValue = 0.0;
        for (int feature : state.features.keySet()) {
            double coeff = getCoeffFor(0, feature);
            retValue += state.features.get(feature) * coeff;
            if (debug) {
                logFile.log(String.format("\tFeature %d\thas coefficient %.2f for state value", feature, coeff));
            }
        }
        if (debug) {
            logFile.log(String.format("Total value for state is %.2f", retValue));
            logFile.flush();
        }
        return retValue;
    }

    public double value(State state, Types.ACTIONS action) {
        if (state == null) return 0.0;
        double retValue = 0;
        int index = getIndexFor(action);
        for (Integer feature : state.features.keySet()) {
            double coeff = getCoeffFor(index, feature);
            if (debug) {
                logFile.log(String.format("\tFeature %d\thas coefficient %.2f for action %s", feature, coeff, action));
            }
            retValue += state.features.get(feature) * coeff;
        }
        if (debug) {
            logFile.log(String.format("Total value for state with %s is %.2f", action, retValue));
            logFile.flush();
        }
        return retValue;
    }

    public double[] getProbabilityDistributionOverActions(List<ACTIONS> actions, State state, double temperature) {
        double[] retValue = new double[actions.size()];
        int count = 0;
        for (ACTIONS a : actions) {
            int index = getIndexFor(a);
            for (int feature : state.features.keySet()) {
                double coeff = getCoeffFor(index, feature);
                retValue[count] += state.features.get(feature) * coeff;
            }
            retValue[count] /= temperature;
            count++;
        }
        retValue = HopshackleUtilities.expNormalise(retValue);
        return retValue;
    }

    public ActionValue valueOfBestAction(State state, List<Types.ACTIONS> actions) {
        if (actions.isEmpty() || state == null) return new ActionValue(Types.ACTIONS.ACTION_NIL, 0.0);
        double retValue = Double.NEGATIVE_INFINITY;
        Types.ACTIONS actionChosen = null;
        for (Types.ACTIONS action : actions) {
            double actionValue = value(state, action);
            if (actionValue > retValue) {
                retValue = actionValue;
                actionChosen = action;
            }
        }
        return new ActionValue(actionChosen, retValue);
    }
}

class ActionValue {
    public Types.ACTIONS action;
    public double value;

    public ActionValue(Types.ACTIONS a, double v) {
        action = a;
        value = v;
    }
}

