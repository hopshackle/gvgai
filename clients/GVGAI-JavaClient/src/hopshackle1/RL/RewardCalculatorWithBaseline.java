package hopshackle1.RL;

import hopshackle1.*;

import java.util.*;

public class RewardCalculatorWithBaseline implements RLTargetCalculator {

    private RLTargetCalculator underlyingCalc;

    public RewardCalculatorWithBaseline(RLTargetCalculator underlying) {
        underlyingCalc = underlying;
    }

    @Override
    public List<Double> processRewards(LinkedList<SARTuple> data) {
        List<Double> temp = underlyingCalc.processRewards(data);

        double meanReward = 0.0;
        for (double r : temp) {
            meanReward += r;
        }

        meanReward /= (double) temp.size();

        List<Double> retValue = new ArrayList(temp.size());
        for (int i = 0; i < temp.size(); i++) {
            retValue.add(temp.get(i) - meanReward);
        }

        return retValue;
    }
}
