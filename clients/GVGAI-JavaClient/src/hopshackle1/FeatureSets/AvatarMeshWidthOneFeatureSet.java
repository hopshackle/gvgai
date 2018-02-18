package hopshackle1.FeatureSets;

import hopshackle1.EntityLog;
import hopshackle1.State;
import serialization.SerializableStateObservation;

public class AvatarMeshWidthOneFeatureSet implements FeatureSet {

    private boolean debug = false;
    private EntityLog logFile = new EntityLog("AvatarMeshFeatures");
    private int distanceFromAvatar = 1;

    public AvatarMeshWidthOneFeatureSet(int distance) {
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
                int feature = mesh.hashCode();
                if (debug) {
                    logFile.log(String.format("Base feature for %d/%d is %d", i, j, feature));
                    logFile.log("\t" + mesh.toString());
                }
                if (feature != 0) {
                    feature += (i + 1) * 6703 + (j + 1) * 28693;
                    retValue.setFeature(feature, 1.0);
                }
            }
        }
    }
}
