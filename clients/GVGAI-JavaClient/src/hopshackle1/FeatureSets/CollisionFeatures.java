package hopshackle1.FeatureSets;

import hopshackle1.*;
import hopshackle1.models.*;
import serialization.*;
import org.javatuples.*;
import java.util.*;

public class CollisionFeatures implements FeatureSet {
    @Override
    public void describeObservation(SerializableStateObservation obs, State state) {
        GameStatusTracker gst = new GameStatusTracker();
        gst.update(obs);    // this is just to provide a convenient method of getting types
        Set<Pair<Integer, Integer>> collisions = SSOModifier.detectCollisions(obs);
        for (Pair<Integer, Integer> collision : collisions) {
            int type1 = gst.getType(collision.getValue0());
            int type2 = gst.getType(collision.getValue1());
            int featureIndex = type1 * 34949 + type2 * 24371 + 821;
            state.setFeature(featureIndex, 1.0);
            if (FeatureSetLibrary.debug) FeatureSetLibrary.registerFeature(featureIndex,  String.format("Collision between %s and %s", type1, type2));
        }
    }
}
