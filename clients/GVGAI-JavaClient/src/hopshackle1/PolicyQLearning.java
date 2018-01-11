package hopshackle1;

import java.util.*;

import serialization.Types;

public class PolicyQLearning implements Policy {

    private List<Map<Integer, Double>> theta = new ArrayList();
    private List<Map<Integer, Double>> thetaMinus = new ArrayList();
    private List<Types.ACTIONS> actions = new ArrayList();
    private double gamma, alpha, lambda, epsilon;
    private Random rnd = new Random(56);
    private boolean debug = true;
    private int updateCount = 0;
    private int updatesForThetaMinus = 1000;
    private EntityLog logFile = new EntityLog("QLearning");

    public PolicyQLearning(double learningRate, double discountRate, double regularisation, double exploration) {
        alpha = learningRate;
        gamma = discountRate;
        lambda = regularisation;
        epsilon = exploration;
    }

    @Override
    public Types.ACTIONS chooseAction(List<Types.ACTIONS> availableActions, State state, boolean actionDebug) {
        if (rnd.nextDouble() < epsilon) {
            Types.ACTIONS choice = availableActions.get(rnd.nextInt(availableActions.size()));
            if (debug || actionDebug)
                logFile.log(String.format("Chooses %s with random exploration", choice.toString()));
            return choice;
        }
        ActionValue choice = valueOfBestAction(state, availableActions, false);
        if (debug || actionDebug) {
            logFile.log(String.format("Chooses %s greedily", choice.action.toString()));
            logFile.flush();
        }
        return choice.action;
    }

    @Override
    public void learnFrom(List<SARTuple> trajectories) {
        for (SARTuple tuple : trajectories) {
            learnFrom(tuple);
        }
    }

    @Override
    public void learnFrom(SARTuple tuple) {
        updateCount++;
        if (updateCount >= updatesForThetaMinus) {
            updateCount = 0;
            updateThetaMinus();
        }
        State currentState = tuple.state;
        int actionIndex = getIndexFor(tuple.action);
        double startValue = value(currentState, tuple.action, false);
        double nextValue = valueOfBestAction(tuple.nextState, tuple.availableEndActions, true).value;
        double target = tuple.reward + gamma * nextValue - startValue;
        if (debug) {
            logFile.log(String.format("Ref: %d StartValue: %.2f Reward %.2f NextValue: %.2f Target: %.2f",
                    tuple.ref, startValue, tuple.reward, nextValue, target));
            logFile.flush();
        }
        for (Integer feature : tuple.state.features.keySet()) {
            modifyCoeff(feature, actionIndex, target, tuple.state.features.get(feature));
        }
    }

    private ActionValue valueOfBestAction(State state, List<Types.ACTIONS> actions, boolean useThetaMinus) {
        if (actions.isEmpty()) return new ActionValue(Types.ACTIONS.ACTION_NIL, 0.0);
        double retValue = Double.NEGATIVE_INFINITY;
        Types.ACTIONS actionChosen = null;
        for (Types.ACTIONS action : actions) {
            double actionValue = value(state, action, useThetaMinus);
            if (actionValue > retValue) {
                retValue = actionValue;
                actionChosen = action;
            }
        }
        return new ActionValue(actionChosen, retValue);
    }

    private double value(State state, Types.ACTIONS action, boolean useThetaMinus) {
        double retValue = 0;
        int index = getIndexFor(action);
        for (Integer feature : state.features.keySet()) {
            double coeff = getCoeffFor(index, feature, useThetaMinus);
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

    private int getIndexFor(Types.ACTIONS action) {
        if (!actions.contains(action)) {
            actions.add(action);
            theta.add(new HashMap());
            thetaMinus.add(new HashMap());
        }
        return actions.indexOf(action);
    }

    private double getCoeffFor(int index, int feature, boolean useThetaMinus) {
        List<Map<Integer, Double>> thetaToUse = useThetaMinus ? thetaMinus : theta;
        Map<Integer, Double> actionCoeffs = thetaToUse.get(index);
        if (!actionCoeffs.containsKey(feature)) {
            actionCoeffs.put(feature, 0.0);
        }
        return actionCoeffs.get(feature);
    }

    private void modifyCoeff(int f, int actionIndex, double target, double fValue) {
        double currentValue = getCoeffFor(actionIndex, f, false);
        double newValue = (1.0 - lambda) * (currentValue + (alpha * fValue * target));
        theta.get(actionIndex).put(f, newValue);
    }

    private void updateThetaMinus() {
        thetaMinus = new ArrayList();
        for (int i = 0; i < theta.size(); i++) {
            Map<Integer, Double> actionTheta = theta.get(i);
            Map<Integer, Double> actionThetaMinus = new HashMap();
            for (Integer f : actionTheta.keySet()) {
                actionThetaMinus.put(f, actionTheta.get(f));
            }
            thetaMinus.add(actionThetaMinus);
        }
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
