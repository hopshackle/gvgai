package hopshackle1;

import java.util.*;

import serialization.Types;
import utils.ElapsedCpuTimer;

public class PolicyQLearning implements Policy {

    private double gamma, alpha, lambda, epsilon;
    private Random rnd = new Random(56);
    private boolean debug = false;
    private double temperature = 3.0;
    private int updateCount = 0;
    private boolean boltzmannDistribution = true;
    private boolean freezeThetaForValuation = false;
    private int updatesForThetaMinus = 10000;
    private boolean monteCarloReward = false;
    private boolean nStepReward = true;
    private PolicyCoeffCore theta = new PolicyCoeffCore("QTheta", debug);
    private PolicyCoeffCore thetaMinus = new PolicyCoeffCore("QThetaMinus", debug);
    private EntityLog logFile;
    private TupleDataBank databank = new TupleDataBank(1000);

    public PolicyQLearning(double learningRate, double discountRate, double regularisation, double exploration) {
        alpha = learningRate;
        gamma = discountRate;
        lambda = regularisation;
        epsilon = exploration;
        if (debug) logFile = new EntityLog("QLearning");
        if (!freezeThetaForValuation) {
            thetaMinus = theta;
        }
    }

    @Override
    public Types.ACTIONS chooseAction(List<Types.ACTIONS> availableActions, State state) {
        ActionValue choice = null;
        if (boltzmannDistribution) {
            double[] pdf = theta.getProbabilityDistributionOverActions(availableActions, state, temperature);
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
        } else {
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
        }
        return choice.action;
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
    public void learnFrom(SARTuple tuple) {
        updateCount++;
        if (updateCount >= updatesForThetaMinus) {
            updateCount = 0;
            updateThetaMinus();
        }

        double rewardToUse = tuple.reward;
        State nextState = tuple.nextState;
        List<Types.ACTIONS> endActions = tuple.availableEndActions;
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
        int actionIndex = theta.getIndexFor(tuple.action);
        double startValue = theta.value(currentState, tuple.action);
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

    private void modifyCoeff(int f, int actionIndex, double target, double fValue) {
        double currentValue = theta.getCoeffFor(actionIndex, f);
        double newValue = (1.0 - lambda) * (currentValue + (alpha * fValue * target));
        theta.setCoeffFor(actionIndex, f, newValue);
    }

    private void updateThetaMinus() {
        if (freezeThetaForValuation)
            thetaMinus.refreshFrom(theta);
    }
}
