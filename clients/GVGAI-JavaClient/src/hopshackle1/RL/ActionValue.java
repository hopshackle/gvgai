package hopshackle1.RL;

import serialization.Types;

public class ActionValue {
    public Types.ACTIONS action;
    public double value;

    public ActionValue(Types.ACTIONS a, double v) {
        action = a;
        value = v;
    }
}
