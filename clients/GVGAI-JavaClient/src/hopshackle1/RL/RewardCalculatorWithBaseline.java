package hopshackle1.RL;

import hopshackle1.*;

import java.util.*;

public class RewardCalculatorWithBaseline implements RLTargetCalculator {

    private RLTargetCalculator underlyingCalc;

    public RewardCalculatorWithBaseline(RLTargetCalculator underlying) {
        underlyingCalc = underlying;
    }

    @Override
    public void crystalliseRewards(LinkedList<SARTuple> data) {
        underlyingCalc.crystalliseRewards(data);

        double meanReward = 0.0;
        for (SARTuple tuple : data) {
            meanReward += tuple.rewardToEnd;
        }

        meanReward /= (double) data.size();

        for (SARTuple tuple : data) {
            tuple.rewardToEnd -= meanReward;
        }
    }
}
