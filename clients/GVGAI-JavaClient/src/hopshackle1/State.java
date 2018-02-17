package hopshackle1;

import hopshackle1.FeatureSets.FeatureSet;
import serialization.SerializableStateObservation;

import java.util.*;

public class State {

    public static Map<Integer, Integer> featureToIndexMap = new HashMap();
    public static Map<Integer, Integer> indexToFeatureMap = new HashMap();
    private static int highestIndex = -1;
    public Map<Integer, Double> features;

    /*
    This is simply a wrapper around a map of features
     */

    public static int getHighestIndex() {
        return highestIndex;
    }

    public State() {
        features = new HashMap();
    }
    public State(SerializableStateObservation sso, FeatureSet[] featureSets) {
        features = new HashMap();
        for (FeatureSet fs : featureSets)
            fs.describeObservation(sso, this);
    }

    public void setFeature(int f, double v) {
        if (!featureToIndexMap.containsKey(f)) {
            highestIndex++;
            featureToIndexMap.put(f, highestIndex);
            indexToFeatureMap.put(highestIndex, f);
        }
        features.put(f, v);
    }

    public double getFeature(int f) {
        return features.getOrDefault(f, 0.0);
    }

    public String toString() {
        if (features.keySet().size() < 25) {
            StringBuilder output = new StringBuilder(String.format("State with %d total features:\n", features.size()));
            for (int f : features.keySet()) {
                output.append(String.format("\t%20d\t%.3f\n",f, features.get(f)));
            }
            return output.toString();
        } else {
            return String.format("State with %d total features", features.size());
        }
    }
}
