package hopshackle1.RL;

import hopshackle1.*;
import serialization.Types;

import java.util.*;


public class MonteCarloReward implements RLTargetCalculator, ReinforcementLearningAlgorithm {

    private double alpha, lambda, gamma;
    private boolean normalise;

    public MonteCarloReward(double alpha, double gamma, double lambda, boolean normaliseAlpha) {
        this.alpha = alpha;
        this.lambda = lambda;
        this.gamma = gamma;
        normalise = normaliseAlpha;
    }

    @Override
    public void crystalliseRewards(LinkedList<SARTuple> data) {
        Iterator<SARTuple> backwardsChain = data.descendingIterator();

        double mcReward = 0.0;
        while (backwardsChain.hasNext()) {
            SARTuple previous = backwardsChain.next();
            previous.setTarget(null, Types.ACTIONS.ACTION_NIL, mcReward + previous.reward, 1.0);
            mcReward = (mcReward + previous.reward) * gamma;
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
