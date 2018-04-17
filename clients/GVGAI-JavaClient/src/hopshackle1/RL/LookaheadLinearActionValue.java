package hopshackle1.RL;

import hopshackle1.*;
import hopshackle1.FeatureSets.*;
import hopshackle1.models.*;
import serialization.*;
import serialization.Types.*;
import hopshackle1.Policies.*;

import java.util.*;

public class LookaheadLinearActionValue implements ActionValueFunctionApproximator, Trainable {

    private Map<Integer, Double> coefficients;
    private FeatureSet[] featureSets;
    private boolean debug;
    private boolean extremeDebug = false;
    private double gamma;
    private LookaheadFunction lookahead;
    private EntityLog logFile = Agent.logFile;
    private double defaultFeatureCoefficient;
    private PolicyGuide policyGuide;

    public LookaheadLinearActionValue(List<FeatureSet> features, double discountRate, boolean debug, LookaheadFunction lookahead) {
        this.debug = debug;
        this.lookahead = lookahead;
        this.gamma = discountRate;
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
        retValue.policyGuide = policyGuide;
        return retValue;
    }

    @Override
    public void injectPolicyGuide(PolicyGuide newGuide) {
        policyGuide = newGuide;
    }

    @Override
    public State calculateState(SerializableStateObservation sso) {
        State retValue = new State(sso, featureSets);
        return retValue;
    }

    @Override
    public double value(GameStatusTracker gst, ACTIONS a) {
        SerializableStateObservation forward = lookahead.rollForward(gst, a);
        return value(forward);
    }

    @Override
    public double value(SerializableStateObservation sso) {
        if (extremeDebug) {
            logFile.log("New SSO:");
   //         logFile.log(sso.toString());
        }
        State state = calculateState(sso);
        double retValue = value(state);
        Vector2d avatarPosition = new Vector2d(sso.avatarPosition[0], sso.avatarPosition[1]);
        double bonus = policyGuide == null ? 0.00 : policyGuide.locationBonus(avatarPosition);
        retValue += bonus;
        if (debug) {
            logFile.log(String.format("Total value for state at t=%d is %.2f (including policy guide bonus of %.2f)", sso.gameTick, retValue, bonus));
            logFile.flush();
        }
        return retValue;
    }

    @Override
    public ActionValue valueOfBestAction(GameStatusTracker gst, List<ACTIONS> actions) {
        if (actions.isEmpty() || gst == null) return new ActionValue(ACTIONS.ACTION_NIL, 0.0);
        double retValue = Double.NEGATIVE_INFINITY;
        ACTIONS actionChosen = null;
        for (ACTIONS action : actions) {
            SerializableStateObservation forward = lookahead.rollForward(gst, action);
            double actionValue = value(forward);
            if (actionValue > retValue) {
                retValue = actionValue;
                actionChosen = action;
            }
        }
        return new ActionValue(actionChosen, retValue);
    }

    @Override
    public double learnFrom(SARTuple tuple, ReinforcementLearningAlgorithm rl) {
        SerializableStateObservation forward = lookahead.rollForward(tuple.startGST, tuple.action);
        State state = calculateState(forward);
        double currentValuation = value(state);
        double endValuation = tuple.finalDiscount * value(tuple.rewardState);
        double delta = endValuation + tuple.rewardToEnd - currentValuation;
        double normaliser = Math.sqrt(state.features.keySet().size());
        for (Integer feature : state.features.keySet()) {
            modifyCoeff(feature, delta, state.features.get(feature), rl, normaliser);
        }
        return delta;
    }

    private void modifyCoeff(int f, double delta, double fValue, ReinforcementLearningAlgorithm rl, double normaliser) {
        double currentValue = getCoeffFor(f);
        double alpha = rl.learningRate() / normaliser;
        double newValue = (1.0 - rl.regularisation()) * (currentValue + (alpha * fValue * delta));
        setCoeffFor(f, newValue);
    }

    public void refreshFrom(LookaheadLinearActionValue pcc) {
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
            coefficients.put(feature, defaultFeatureCoefficient);
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
        if (extremeDebug) logFile.log("New state valuation for : ");
        for (int feature : state.features.keySet()) {
            double coeff = getCoeffFor(feature);
            retValue += state.features.get(feature) * coeff;
            if (extremeDebug) {
                logFile.log(String.format("\t%.2f\t%d\t%s", coeff, feature, FeatureSetLibrary.getDescription(feature)));
            }
        }
        return retValue;
    }

    public void setDefaultFeatureCoefficient(double newValue) {
        defaultFeatureCoefficient = newValue;
    }

    @Override
    public String toString() {
        StringBuilder retValue = new StringBuilder();
        List<Integer> featureIDs = HopshackleUtilities.convertSetToList(coefficients.keySet());
        Collections.sort(featureIDs);
        for (int f : featureIDs) {
            if (coefficients.get(f) != 1.0)
                retValue.append(String.format("\t%d\t%.3f\t%s\n", f, coefficients.get(f), FeatureSetLibrary.getDescription(f)));
        }
        return retValue.toString();
    }
}

