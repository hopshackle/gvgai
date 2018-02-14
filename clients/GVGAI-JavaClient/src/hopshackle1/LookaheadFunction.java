package hopshackle1;

import serialization.*;
import serialization.Types.*;

public interface LookaheadFunction {

    public SerializableStateObservation rollForward(SerializableStateObservation sso, ACTIONS action);
}
