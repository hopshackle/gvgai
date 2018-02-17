package hopshackle1.RL;

import hopshackle1.*;
import hopshackle1.FeatureSets.FeatureSet;
import serialization.*;
import serialization.Types.*;

import java.util.*;

public class LookaheadLinearActionValue implements ActionValueFunctionApproximator, Trainable {

    private Map<Integer, Double> coefficients;
    private FeatureSet[] featureSets;
    private boolean debug;
    private double gamma;
    private LookaheadFunction lookahead;
    private EntityLog logFile;

    public LookaheadLinearActionValue(List<FeatureSet> features, double discountRate, boolean debug, LookaheadFunction lookahead) {
        this.debug = debug;
        this.lookahead = lookahead;
        this.gamma = discountRate;
        if (debug) logFile = new EntityLog("LookaheadCoeff");
        coefficients = new HashMap();
        featureSets = new FeatureSet[features.size()];
        for (int i = 0; i < features.size(); i++) {
            featureSets[i] = features.get(i);
        }
    }

    private LookaheadLinearActionValue() {
        // to prevent usage - only for cloning
    }

    @Override
    public LookaheadLinearActionValue copy() {
        LookaheadLinearActionValue retValue = new LookaheadLinearActionValue();
        retValue.gamma = gamma;
        retValue.lookahead = lookahead;
        retValue.featureSets = Arrays.copyOf(featureSets, featureSets.length);
        retValue.coefficients = new HashMap();
        for (Integer f : coefficients.keySet()) {
            retValue.coefficients.put(f, coefficients.get(f));
        }

        return retValue;
    }

    @Override
    public State calculateState(SerializableStateObservation sso) {
        State retValue = new State(sso, featureSets);
        return retValue;
    }

    @Override
    public double value(SerializableStateObservation sso, ACTIONS a) {
        SerializableStateObservation forward = lookahead.rollForward(sso, a);
        return value(forward);
    }

    @Override
    public double value(SerializableStateObservation sso) {
        State state = calculateState(sso);
        return value(state);
    }

    @Override
    public ActionValue valueOfBestAction(SerializableStateObservation sso, List<ACTIONS> actions) {
        if (actions.isEmpty() || sso == null) return new ActionValue(ACTIONS.ACTION_NIL, 0.0);
        double retValue = Double.NEGATIVE_INFINITY;
        ACTIONS actionChosen = null;
        for (ACTIONS action : actions) {
            SerializableStateObservation forward = lookahead.rollForward(sso, action);
            double actionValue = value(forward, action);
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

        for (Integer feature : state.features.keySet()) {
            modifyCoeff(feature, tuple.target, state.features.get(feature), rl);
        }
    }

    private void modifyCoeff(int f, double target, double fValue, ReinforcementLearningAlgorithm rl) {
        double currentValue = getCoeffFor(f);
        double newValue = (1.0 - rl.regularisation()) * (currentValue + (rl.learningRate() * fValue * target));
        setCoeffFor(f, newValue);
    }

    public void refreshFrom(LookaheadLinearActionValue pcc) {
        if (debug) logFile.log("Refreshing");
        coefficients = new HashMap();
        for (int i = 0; i < pcc.coefficients.size(); i++) {
            Map<Integer, Double> cloneActionTheta = new HashMap();
            for (Integer f : pcc.coefficients.keySet()) {
                cloneActionTheta.put(f, pcc.coefficients.get(f));
            }
            coefficients = cloneActionTheta;
        }
    }

    public double getCoeffFor(int feature) {
        if (!coefficients.containsKey(feature)) {
            coefficients.put(feature, 0.0);
        }
        return coefficients.get(feature);
    }

    public void setCoeffFor(int feature, double value) {
        coefficients.put(feature, value);
    }

    public void clearCoeffs(int index) {
        coefficients = new HashMap();
    }

    private double value(State state) {
        double retValue = 0.0;
        for (int feature : state.features.keySet()) {
            double coeff = getCoeffFor(feature);
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
}

