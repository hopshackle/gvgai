package hopshackle1.RL;

import hopshackle1.*;
import serialization.SerializableStateObservation;
import serialization.Types.*;

import java.util.*;

public class IndependentLinearActionValue implements ActionValueFunctionApproximator, Trainable {

    private double gamma;
    private List<Map<Integer, Double>> coefficients;
    private List<ACTIONS> actions;
    private boolean debug;
    private EntityLog logFile;
    private FeatureSet[] featureSets;

    public IndependentLinearActionValue(List<FeatureSet> features, double discountRate, boolean debug) {
        gamma = discountRate;
        this.debug = debug;
        featureSets = new FeatureSet[features.size()];
        for (int i = 0; i < features.size(); i++) {
            featureSets[i] = features.get(i);
        }
        if (debug) logFile = new EntityLog("CoeffByAction");
        coefficients = new ArrayList();
        coefficients.add(new HashMap());    // the Value function is always at index = 0
        actions = new ArrayList();
        actions.add(null);  // a dummy placeholder for Value, so that coefficients and actions use the same index
    }

    private IndependentLinearActionValue() {
        // for internal use only
    }

    @Override
    public IndependentLinearActionValue copy() {
        IndependentLinearActionValue retValue = new IndependentLinearActionValue();
        retValue.gamma = gamma;
        retValue.actions = HopshackleUtilities.cloneList(actions);
        retValue.coefficients = new ArrayList();
        retValue.featureSets = Arrays.copyOf(featureSets, featureSets.length);
        for (int i = 0; i < coefficients.size(); i++) {
            Map<Integer, Double> actionTheta = coefficients.get(i);
            Map<Integer, Double> cloneActionTheta = new HashMap();
            for (Integer f : actionTheta.keySet()) {
                cloneActionTheta.put(f, actionTheta.get(f));
            }
            coefficients.add(cloneActionTheta);
        }
        return retValue;
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

    @Override
    public State calculateState(SerializableStateObservation sso) {
        State retValue = new State(sso, featureSets);
        return retValue;
    }

    @Override
    public double value(SerializableStateObservation sso) {
        State state = calculateState(sso);
        return value(state);
    }

    private double value(State state) {
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

    @Override
    public double value(SerializableStateObservation sso, ACTIONS action) {
        State state = calculateState(sso);
        return value(state, action);
    }

    private double value(State state, ACTIONS action) {
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


    @Override
    public ActionValue valueOfBestAction(SerializableStateObservation sso, List<ACTIONS> actions) {
        if (actions.isEmpty() || sso == null) return new ActionValue(ACTIONS.ACTION_NIL, 0.0);
        double retValue = Double.NEGATIVE_INFINITY;
        State state = calculateState(sso); // to avoid repeating for every action
        ACTIONS actionChosen = null;
        for (ACTIONS action : actions) {
            double actionValue = value(state, action);
            if (actionValue > retValue) {
                retValue = actionValue;
                actionChosen = action;
            }
        }
        return new ActionValue(actionChosen, retValue);
    }

    @Override
    public void learnFrom(SARTuple tuple, ReinforcementLearningAlgorithm rl) {
        State state = calculateState(tuple.startSSO);
        int actionIndex = getIndexFor(tuple.action);

        modifyCoeff(state.features, actionIndex, tuple.target, rl);
    }

    public void learnValueFrom(SARTuple tuple, ReinforcementLearningAlgorithm rl) {
        State state = calculateState(tuple.startSSO);
        int actionIndex = 0;

        modifyCoeff(state.features, actionIndex, tuple.target, rl);
    }

    private void modifyCoeff(Map<Integer, Double> features, int actionIndex, double target, ReinforcementLearningAlgorithm rl) {
        for (Integer f : features.keySet()) {
            double fValue = features.get(f);
            double currentValue = getCoeffFor(actionIndex, f);
            double newValue = (1.0 - rl.regularisation()) * (currentValue + (rl.learningRate() * fValue * target));
            setCoeffFor(actionIndex, f, newValue);
        }
    }
}

