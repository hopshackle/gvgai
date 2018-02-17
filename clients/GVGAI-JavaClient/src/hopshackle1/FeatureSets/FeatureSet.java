package hopshackle1.FeatureSets;

import hopshackle1.State;
import serialization.SerializableStateObservation;

public interface FeatureSet {

    /*
    This specifies which features we use to define a given state
     */

    void describeObservation(SerializableStateObservation obs, State state);
}
