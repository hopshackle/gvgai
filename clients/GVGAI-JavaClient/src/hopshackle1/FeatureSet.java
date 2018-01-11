package hopshackle1;

import serialization.SerializableStateObservation;

public interface FeatureSet {

    /*
    This specifies which features we use to define a given state
     */

    void describeObservation(SerializableStateObservation obs, State state);
}
