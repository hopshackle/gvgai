package hopshackle1.FeatureSets;

import hopshackle1.FeatureSets.FeatureSet;
import hopshackle1.State;
import serialization.Observation;
import serialization.SerializableStateObservation;

public class GlobalPopulationFeatureSet implements FeatureSet {

    public void describeObservation(SerializableStateObservation obs, State retValue) {
        extractFeaturesFrom(retValue, obs.getNPCPositions());
        extractFeaturesFrom(retValue, obs.getMovablePositions());
        extractFeaturesFrom(retValue, obs.getImmovablePositions());
        extractFeaturesFrom(retValue, obs.getPortalsPositions());
        extractFeaturesFrom(retValue, obs.getResourcesPositions());
        extractFeaturesFrom(retValue, obs.getFromAvatarSpritesPositions());
    }

    private void extractFeaturesFrom(State base, Observation[][] obs) {
        // the first dimension of the array is sprite type
        if (obs == null) return;
        for (int i = 0; i < obs.length; i++) {
            if (obs[i].length > 0 && obs[i][0] != null) {
                int spriteType = obs[i][0].itype;
                int numberOfSprites = obs[i].length;
                if (numberOfSprites > 0) {
                    base.setFeature(spriteType * 113, 1.0);
                    if (FeatureSetLibrary.debug) {
                        FeatureSetLibrary.registerFeature(spriteType * 113,  String.format("At least 1 sprite of type %d", spriteType));
                    }
                }
                if (numberOfSprites > 1) {
                    base.setFeature(spriteType * 199, 1.0);
                    if (FeatureSetLibrary.debug) {
                        FeatureSetLibrary.registerFeature(spriteType * 199,  String.format("At least 2 sprites of type %d", spriteType));
                    }
                }
                if (numberOfSprites > 3) {
                    base.setFeature(spriteType * 941, 1.0);
                    if (FeatureSetLibrary.debug) {
                        FeatureSetLibrary.registerFeature(spriteType * 941,  String.format("At least 4 sprites of type %d", spriteType));
                    }
                }
                if (numberOfSprites > 7) {
                    base.setFeature(spriteType * 101323, 1.0);
                    if (FeatureSetLibrary.debug) {
                        FeatureSetLibrary.registerFeature(spriteType * 101323,  String.format("At least 8 sprites of type %d", spriteType));
                    }
                }
                if (numberOfSprites > 15) {
                    base.setFeature(spriteType * 2531, 1.0);
                    if (FeatureSetLibrary.debug) {
                        FeatureSetLibrary.registerFeature(spriteType * 2531,  String.format("At least 16 sprites of type %d", spriteType));
                    }
                }
            }
        }
    }
}
