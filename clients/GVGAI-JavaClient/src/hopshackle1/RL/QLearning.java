package hopshackle1.RL;

import hopshackle1.*;
import org.javatuples.*;
import serialization.*;
import serialization.Types.*;

import java.util.*;

public class QLearning implements RLTargetCalculator, ReinforcementLearningAlgorithm {

    private double alpha, lambda, gamma;
    private int nStepReward;
    private ActionValueFunctionApproximator QFunction;
    private boolean debug = false;
    private EntityLog logFile;

    public QLearning(int n, double alpha, double gamma, double lambda, ActionValueFunctionApproximator fa) {
        this.alpha = alpha;
        this.lambda = lambda;
        this.gamma = gamma;
        nStepReward = n;
        this.QFunction = fa;
        if (nStepReward < 1) nStepReward = 1;
    }

    @Override
    public List<Double> processRewards(LinkedList<SARTuple> data) {
        Iterator<SARTuple> backwardsChain = data.descendingIterator();
        List<Double> retValue = new ArrayList();
        double[] nStepValue = new double[nStepReward];
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

            // and add on the discounted value of the state N in the future
            double target = runningRewardSum + gammaN * nStepValue[currentIndex];
            retValue.add(target);

            // then estimate the value of the next state
            ActionValue bestAction = QFunction.valueOfBestAction(previous.nextSSO, previous.availableEndActions);
            nStepValue[currentIndex] = bestAction.value;

            currentIndex = (currentIndex + 1) % nStepReward;

            if (debug) {
                double startValue = QFunction.value(previous.startSSO, previous.action);
                logFile.log(String.format("Ref: %d Action: %s StartValue: %.2f Reward %.2f NextAction: %s NextValue: %.2f Target: %.2f",
                        previous.ref, previous.action, startValue, runningRewardSum, bestAction.action, bestAction.value, target));
                logFile.flush();
            }
        }
        Collections.reverse(retValue);
        return retValue;
    }

    @Override
    public double learningRate() {
        return alpha;
    }

    @Override
    public double regularisation() {
        return lambda;
    }
}
