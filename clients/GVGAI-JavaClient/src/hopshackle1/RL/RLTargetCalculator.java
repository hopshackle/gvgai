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
    public void crystalliseRewards(LinkedList<SARTuple> data);

}
