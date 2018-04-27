package hopshackle1.FeatureSets;

import java.util.*;

public class FeatureSetLibrary {

    static Map<Integer, String> mapOfNames = new HashMap();
    static Map<Integer, String> mapOfSets = new HashMap();
    static Map<String, Integer> featureSetCounts = new HashMap();
    static boolean debug = true;

    public static void registerFeature(int f, String set, String description) {
        if (!mapOfSets.containsKey(f)) {
            mapOfSets.put(f, set);
            mapOfNames.put(f, description);
            featureSetCounts.put(set, featureSetCounts.getOrDefault(set, 0) + 1);
        }
    }

    public static void registerFeature(int f, String set) {
        if (!mapOfSets.containsKey(f)) {
            mapOfSets.put(f, set);
            featureSetCounts.put(set, featureSetCounts.getOrDefault(set, 0) + 1);
        }
    }

    public static String getDescription(int f) {
        return mapOfNames.getOrDefault(f, "N/A");
    }

    public static String getFeatureSet(int f) {
        return mapOfSets.getOrDefault(f, "N/A");
    }

    public static int getNumberOf(String featureSet) {
        return featureSetCounts.getOrDefault(featureSet, 0);
    }

}
