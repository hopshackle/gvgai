package hopshackle1;

import serialization.Observation;
import serialization.SerializableStateObservation;

public class GlobalPopulationFeatureSet implements  FeatureSet {

    public void describeObservation(SerializableStateObservation obs, State retValue) {
        extractFeaturesFrom(retValue, obs.getNPCPositions());
        extractFeaturesFrom(retValue, obs.getMovablePositions());
        extractFeaturesFrom(retValue, obs.getImmovablePositions());
        extractFeaturesFrom(retValue, obs.getPortalsPositions());
        extractFeaturesFrom(retValue, obs.getResourcesPositions());
        extractFeaturesFrom(retValue, obs.getFromAvatarSpritesPositions());
    }

    public void extractFeaturesFrom(State base, Observation[][] obs) {
        // the first dimension of the array is sprite type
        if (obs == null) return;
        for (int i = 0; i < obs.length; i++) {
            if (obs[i].length > 0 && obs[i][0] != null) {
                int spriteType = obs[i][0].itype;
                int numberOfSprites = obs[i].length;
                if (numberOfSprites > 0) {
                    base.setFeature(spriteType * 113, 1.0);
                }
                if (numberOfSprites > 1) {
                    base.setFeature(spriteType * 199, 1.0);
                }
                if (numberOfSprites > 2) {
                    base.setFeature(spriteType * 941, 1.0);
                }
                if (numberOfSprites > 5) {
                    base.setFeature(spriteType * 101323, 1.0);
                }
                if (numberOfSprites > 9) {
                    base.setFeature(spriteType * 2531, 1.0);
                }
                if (numberOfSprites > 19) {
                    base.setFeature(spriteType * 2113, 1.0);
                }
            }
        }
    }
}
