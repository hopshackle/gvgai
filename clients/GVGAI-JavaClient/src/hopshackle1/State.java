package hopshackle1;

import java.util.*;

public class State {

    public Map<Integer, Double> features;

    /*
    This is simply a wrapper around a map of features
     */
    public State () {
        features = new HashMap<>();
    }

    public void setFeature(int f, double v) {
        features.put(f, v);
    }

    public double getFeature(int f) {
        return features.getOrDefault(f, 0.0);
    }

}
