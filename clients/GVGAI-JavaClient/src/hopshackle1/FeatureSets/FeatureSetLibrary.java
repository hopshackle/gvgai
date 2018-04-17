package hopshackle1.FeatureSets;

import java.util.*;

public class FeatureSetLibrary {

    static Map<Integer, String> map = new HashMap();
    static boolean debug = true;

    public static void registerFeature(int f, String description) {
        if (!map.containsKey(f)) {
            map.put(f, description);
        }
    }

    public static String getDescription(int f) {
        return map.getOrDefault(f, "N/A");
    }
}
