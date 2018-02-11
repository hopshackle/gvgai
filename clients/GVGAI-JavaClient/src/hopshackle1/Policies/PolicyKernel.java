package hopshackle1.Policies;

import java.util.*;
import serialization.*;
import hopshackle1.*;

public interface PolicyKernel {

    public <T extends PolicyKernel> T copy();

    public double value(State s, Types.ACTIONS a);

    public double value(State s);

    public ActionValue valueOfBestAction(State s, List<Types.ACTIONS> actions);

    public void learnFrom(State state, Types.ACTIONS action, double target);
}

class ActionValue {
    public Types.ACTIONS action;
    public double value;

    public ActionValue(Types.ACTIONS a, double v) {
        action = a;
        value = v;
    }
}
