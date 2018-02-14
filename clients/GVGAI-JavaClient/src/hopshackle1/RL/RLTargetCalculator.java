package hopshackle1.RL;

import hopshackle1.*;
import org.javatuples.*;
import java.util.*;

import serialization.Types;
import serialization.Types.*;

public interface RLTargetCalculator {

    /*
    Will always need to be called once the full Trajectory is obtained
     */
    public List<Double> processRewards(LinkedList<SARTuple> data);

    public static void processRewardsWith(LinkedList<SARTuple> data, RLTargetCalculator calc) {
        List<Double> values = calc.processRewards(data);
        for (int i = 0; i < values.size(); i++) {
            data.get(i).target = values.get(i);
        }
    }
}
