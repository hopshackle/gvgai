package hopshackle1.RL;

import hopshackle1.*;
import java.util.*;


public class MonteCarloReward implements RLTargetCalculator, ReinforcementLearningAlgorithm {

    private double alpha, lambda, gamma;

    public MonteCarloReward(double alpha, double gamma, double lambda) {
        this.alpha = alpha;
        this.lambda = lambda;
        this.gamma = gamma;
    }

    @Override
    public List<Double> processRewards(LinkedList<SARTuple> data) {
        Iterator<SARTuple> backwardsChain = data.descendingIterator();
        List<Double> retValue = new ArrayList();

        double mcReward = 0.0;
        while (backwardsChain.hasNext()) {
            SARTuple previous = backwardsChain.next();
            mcReward = mcReward * gamma + previous.reward;
            retValue.add(mcReward);
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
