package hopshackle1.RL;

import java.util.*;

import serialization.*;
import hopshackle1.*;

public interface ActionValueFunctionApproximator {

    public <T extends ActionValueFunctionApproximator> T copy();

    public State calculateState(SerializableStateObservation sso);

    public double value(SerializableStateObservation s, Types.ACTIONS a);

    public double value(SerializableStateObservation s);

    public ActionValue valueOfBestAction(SerializableStateObservation s, List<Types.ACTIONS> actions);

}

