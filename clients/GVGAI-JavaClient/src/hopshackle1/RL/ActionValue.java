package hopshackle1.RL;

import serialization.*;
import serialization.Types;

public class ActionValue {
    public Types.ACTIONS action;
    public double value;
    public SerializableStateObservation sso;

    /*
    This just records a state, an action and a reward value as a convenient triple
    s is the state that should be valued to get the end value
     */
    public ActionValue(Types.ACTIONS a, double v) {
        action = a;
        value = v;
    }
}
