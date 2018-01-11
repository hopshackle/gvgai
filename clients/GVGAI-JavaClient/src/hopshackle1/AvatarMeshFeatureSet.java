package hopshackle1;

import serialization.SerializableStateObservation;

public class AvatarMeshFeatureSet implements FeatureSet {

    private boolean debug = false;
    private EntityLog logFile = new EntityLog("AvatarMeshFeatures");

    public void describeObservation(SerializableStateObservation obs, State retValue) {

        if (debug) {
            logFile.log(obs.toString());
        }
        // firstly one-cell meshes around the avatar
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (i == 0 && j == 0) continue;
                MeshHash mesh = new MeshHash(1, obs, i, j);
                int feature = mesh.hashCode();
                if (debug) {
                    logFile.log(String.format("Base feature for %d/%d is %d", i, j, feature));
                    logFile.log("\t" + mesh.toString());
                }
                feature += (i+1) * 6703 + (j+1) * 28693;
                retValue.setFeature(feature, 1.0);
            }
        }
        // then three-cell meshes
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                MeshHash mesh = new MeshHash(3, obs, i*3, j*3);
                int feature = mesh.hashCode();
                if (debug) {
                    logFile.log(String.format("Base feature for %d/%d is %d", i, j, feature));
                    logFile.log("\t" + mesh.toString());
                }
                feature += (i+1) * 709469 + (j+1) * 513067;
                retValue.setFeature(feature, 1.0);
            }
        }

        // giving us a total of 17 features from this
    }
}
