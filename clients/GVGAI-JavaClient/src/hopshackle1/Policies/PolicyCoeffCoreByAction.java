package hopshackle1.Policies;

import hopshackle1.*;
import serialization.Types.*;
import utils.*;

import java.util.*;

public class PolicyCoeffCoreByAction implements PolicyKernel {

    private double gamma, alpha, lambda;
    private List<Map<Integer, Double>> coefficients;
    private List<ACTIONS> actions;
    private boolean debug;
    private EntityLog logFile;
    private int updateCount = 0;
    private int updatesForThetaMinus = 10000;
    private boolean monteCarloReward = false;
    private boolean nStepReward = true;
    private String name;
    private PolicyKernel thetaMinus;
    private boolean freezeThetaForValuation = false;
    private TupleDataBank databank = new TupleDataBank(1000);

    public PolicyCoeffCoreByAction(String name, double learningRate, double discountRate, double regularisation, boolean debug) {
        alpha = learningRate;
        gamma = discountRate;
        lambda = regularisation;
        this.name = name;
        this.debug = debug;
        if (debug) logFile = new EntityLog(name);
        coefficients = new ArrayList();
        coefficients.add(new HashMap());    // the Value function is always at index = 0
        actions = new ArrayList();
        actions.add(null);  // a dummy placeholder for Value, so that coefficients and actions use the same index
        if (freezeThetaForValuation) {
            thetaMinus = this.copy();
        } else {
            thetaMinus = this;
        }
    }

    private PolicyCoeffCoreByAction() {
        // for internal use only
    }

    @Override
    public PolicyCoeffCoreByAction copy() {
        PolicyCoeffCoreByAction retValue = new PolicyCoeffCoreByAction();
        retValue.alpha = alpha;
        retValue.gamma = gamma;
        retValue.lambda = lambda;
        retValue.actions = HopshackleUtilities.cloneList(actions);
        retValue.coefficients = new ArrayList();
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
    public double value(State state) {
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
    public double value(State state, ACTIONS action) {
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


    @Override
    public ActionValue valueOfBestAction(State state, List<ACTIONS> actions) {
        if (actions.isEmpty() || state == null) return new ActionValue(ACTIONS.ACTION_NIL, 0.0);
        double retValue = Double.NEGATIVE_INFINITY;
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
    public void learnFrom(List<SARTuple> trajectories) {
        databank.addData(trajectories);
        List<SARTuple> trainingData = databank.getAllData();
        for (SARTuple tuple : trainingData) {
            learnFrom(tuple);
        }
    }

    @Override
    public void learnUntil(ElapsedCpuTimer cpuTimer, int beforeEnd) {
        if (databank.getAllData().size() < 100) return;
        int count = 0;
        do {
            count++;
            learnFrom(databank.getTuple());
        } while (cpuTimer.remainingTimeMillis() > beforeEnd);
        //      System.out.println(count + " tuples processed");
    }

    @Override
    public void learnFrom(SARTuple tuple) {
        updateCount++;
        if (updateCount >= updatesForThetaMinus) {
            updateCount = 0;
            thetaMinus = this.copy();
        }

        double rewardToUse = tuple.reward;
        State nextState = tuple.nextState;
        List<ACTIONS> endActions = tuple.availableEndActions;
        double effectiveGamma = gamma;
        if (monteCarloReward) {
            rewardToUse = tuple.mcReward;
            nextState = null;
        } else if (nStepReward) {
            rewardToUse = tuple.mcReward;
            nextState = tuple.nNextState;
            endActions = tuple.availableNStepActions;
            effectiveGamma = Math.pow(gamma, tuple.nStep);
        }
        State currentState = tuple.state;
        int actionIndex = getIndexFor(tuple.action);
        double startValue = value(currentState, tuple.action);
        ActionValue next = thetaMinus.valueOfBestAction(nextState, endActions);

        double target = rewardToUse + effectiveGamma * next.value - startValue;
        if (debug) {
            logFile.log(String.format("Ref: %d Action: %s StartValue: %.2f Reward %.2f NextAction: %s NextValue: %.2f Target: %.2f",
                    tuple.ref, tuple.action, startValue, rewardToUse, next.action, next.value, target));
            logFile.flush();
        }
        for (Integer feature : tuple.state.features.keySet()) {
            modifyCoeff(feature, actionIndex, target, tuple.state.features.get(feature));
        }
    }

    private void modifyCoeff(int f, int actionIndex, double target, double fValue) {
        double currentValue = getCoeffFor(actionIndex, f);
        double newValue = (1.0 - lambda) * (currentValue + (alpha * fValue * target));
        setCoeffFor(actionIndex, f, newValue);
    }
}


