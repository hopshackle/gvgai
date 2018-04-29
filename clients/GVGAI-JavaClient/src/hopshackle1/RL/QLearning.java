package hopshackle1.RL;

import hopshackle1.*;
import hopshackle1.models.GameStatusTracker;
import org.javatuples.*;
import serialization.*;
import serialization.Types.*;

import java.util.*;

public class QLearning implements RLTargetCalculator, ReinforcementLearningAlgorithm {

    private double alpha, lambda, gamma;
    private int nStepReward;
    private ActionValueFunctionApproximator QFunction;
    private boolean debug = true;
    private boolean normalise;
    private EntityLog logFile;
    public int recrystallisationThreshold = 10;

    public QLearning(int n, double alpha, double gamma, double lambda, boolean normaliseAlpha, ActionValueFunctionApproximator fa) {
        this.alpha = alpha;
        this.lambda = lambda;
        this.gamma = gamma;
        normalise = normaliseAlpha;
        nStepReward = n;
        this.QFunction = fa;
        if (nStepReward < 1) nStepReward = 1;
        if (debug) {
            logFile = new EntityLog("QLearning");
        }
    }

    @Override
    public int recrystalliseRewards(List<SARTuple> data) {
        // in this we do not change the linkage of records, but reconsider what the bst next action is
        int retValue = 0;
        for (SARTuple tuple : data) {
            if (tuple.getProcessCount() >= recrystallisationThreshold) {
                tuple.resetCount();
                ActionValue bestAction = QFunction.valueOfBestAction(tuple.rewardGST, tuple.availableEndActions);
                if (bestAction.action != tuple.actionFromEnd) {
                    retValue++;
                    tuple.actionFromEnd = bestAction.action;
                }
            }
        }
        return retValue;
    }

    @Override
    public void crystalliseRewards(LinkedList<SARTuple> data) {
        Iterator<SARTuple> backwardsChain = data.descendingIterator();
        SARTuple[] nStepTuple = new SARTuple[nStepReward];
        ACTIONS[] nStepBestAction = new ACTIONS[nStepReward];
        double[] nSteps = new double[nStepReward];
        int currentIndex = 0;
        double runningRewardSum = 0.0;
        double gammaN = Math.pow(gamma, nStepReward);

        while (backwardsChain.hasNext()) {
            SARTuple previous = backwardsChain.next();

            // discount running reward, and add new reward to it
            runningRewardSum = gamma * runningRewardSum + previous.reward;

            // subtract the amount that falls off the end (has been discounted n times since its addition
            runningRewardSum = runningRewardSum - gammaN * nSteps[currentIndex];

            // we store current reward after using the old reward ('cos with n=1 these use the same slot)
            nSteps[currentIndex] = previous.reward;

            // then estimate the best action (and also record the state from which we take it)
            ActionValue bestAction = QFunction.valueOfBestAction(previous.nextGST, previous.availableEndActions);
            nStepTuple[currentIndex] = previous;
            nStepBestAction[currentIndex] = bestAction.action;

            currentIndex = (currentIndex + 1) % nStepReward;

            if (debug && previous.rewardGST != null) {
                double startValue = QFunction.value(previous.startGST, previous.action);
                logFile.log(String.format("Data overidden: Ref: %d Action: %s StartValue: %.2f Reward %.2f NextAction: %s NextValue: %.2f",
                        previous.ref, previous.action, startValue, runningRewardSum, bestAction.action, bestAction.value));
            }

            // and add on the discounted value of the state N in the future
            previous.setTarget(nStepTuple[currentIndex], nStepBestAction[currentIndex], runningRewardSum, gammaN);

            if (debug) {
                double startValue = QFunction.value(previous.startGST, previous.action);
                logFile.log(String.format("Ref: %d Action: %s StartValue: %.2f Reward %.2f NextAction: %s NextValue: %.2f",
                        previous.ref, previous.action, startValue, runningRewardSum, bestAction.action, bestAction.value));
                logFile.flush();
            }
        }
    }

    @Override
    public double learningRate() {
        return alpha;
    }

    @Override
    public double regularisation() {
        return lambda;
    }

    @Override
    public boolean normaliseLearningRate() {
        return normalise;
    }
}
