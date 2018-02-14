package hopshackle1.Policies;

import serialization.*;
import serialization.Types;
import hopshackle1.RL.*;
import hopshackle1.*;
import java.util.*;

public interface Policy {

    public Types.ACTIONS chooseAction(List<Types.ACTIONS> actions, SerializableStateObservation sso);

}
