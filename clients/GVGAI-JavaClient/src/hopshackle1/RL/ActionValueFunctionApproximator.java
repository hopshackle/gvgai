package hopshackle1.RL;

import java.util.*;

import hopshackle1.models.*;
import serialization.*;
import hopshackle1.*;
import hopshackle1.Policies.*;

public interface ActionValueFunctionApproximator {

    public <T extends ActionValueFunctionApproximator> T copy();

    public State calculateState(SerializableStateObservation sso);

//    public double value(SerializableStateObservation s, Types.ACTIONS a);

    public double value(GameStatusTracker gst, Types.ACTIONS a);

    public double value(SerializableStateObservation s);

 //   public ActionValue valueOfBestAction(SerializableStateObservation s, List<Types.ACTIONS> actions);

    public ActionValue valueOfBestAction(GameStatusTracker gst, List<Types.ACTIONS> actions);

    public void injectPolicyGuide(PolicyGuide guide);

    public double valueOfCoefficient(int feature);
}

