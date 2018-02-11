package hopshackle1.Policies;

import serialization.SerializableStateObservation;
import serialization.Types;
import utils.ElapsedCpuTimer;
import hopshackle1.*;
import java.util.*;

public interface Policy {

    public Types.ACTIONS chooseAction(List<Types.ACTIONS> actions, SerializableStateObservation sso);

}
