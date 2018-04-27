package hopshackle1.FeatureSets;

import hopshackle1.EntityLog;
import hopshackle1.State;
import serialization.SerializableStateObservation;

public class AvatarMeshWidthOneFeatureSet implements FeatureSet {

    private boolean debug = false;
    private EntityLog logFile = new EntityLog("AvatarMeshFeatures");
    private int distanceFromAvatar = 1;
    private String setName;

    public AvatarMeshWidthOneFeatureSet(int distance) {
        setName = "AvatarMeshOne-" + distance;
        distanceFromAvatar = distance;
    }

    public void describeObservation(SerializableStateObservation obs, State retValue) {

        if (debug) {
            logFile.log(obs.toString());
        }
        // one-cell meshes centred at each cell in a 3x3 grid centered on the Avatar
        for (int i = -distanceFromAvatar; i <= distanceFromAvatar; i++) {
            for (int j = -distanceFromAvatar; j <= distanceFromAvatar; j++) {
                MeshHash mesh = new MeshHash(1, obs, i, j);
                int meshHash = mesh.hashCode();
                if (debug) {
                    logFile.log(String.format("Base feature for %d/%d is %d", i, j, meshHash));
                    logFile.log("\t" + mesh.toString());
                }
                if (meshHash != 0) {
                    int feature = meshHash + (i + 1) * 6703 + (j + 1) * 28697;
                    retValue.setFeature(feature, 1.0);
                    if (FeatureSetLibrary.debug) {
                        FeatureSetLibrary.registerFeature(feature,  setName, String.format("%d %s at %d:%d", meshHash, mesh.abbrev(), i, j));
                    } else {
                        FeatureSetLibrary.registerFeature(feature,  setName);
                    }
                }
            }
        }
    }
}
