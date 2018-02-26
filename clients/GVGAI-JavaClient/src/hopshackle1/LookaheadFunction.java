package hopshackle1;

import serialization.*;
import serialization.Types.*;
import hopshackle1.models.*;

public interface LookaheadFunction {

    public SerializableStateObservation rollForward(GameStatusTracker gst, ACTIONS action);
}
