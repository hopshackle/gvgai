package hopshackle1;

import serialization.SerializableStateObservation;

public class AvatarMeshFeatureSet implements FeatureSet {

    public State describeObservation(SerializableStateObservation obs) {
        State retValue = new State();
        // firstly one-cell meshes around the avatar
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (i == 0 && j == 0) continue;
                int feature = (new MeshHash(1, obs, i, j)).hashCode();
                feature += (i+1) * 6703 + (j+1) * 28693;
                retValue.setFeature(feature, 1.0);
            }
        }
        // then three-cell meshes
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                int feature = (new MeshHash(3, obs, i*3, j*3)).hashCode();
                feature += (i+1) * 709469 + (j+1) * 513067;
                retValue.setFeature(feature, 1.0);
            }
        }

        // giving us a total of 17 features from this
        return retValue;
    }
}
