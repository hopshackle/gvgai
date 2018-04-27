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
                int f = 0;
                if (numberOfSprites > 0) {
                    f = spriteType * 113;
                    setFeature(base, f, 1, spriteType);
                }
                if (numberOfSprites > 1) {
                    f = spriteType * 199;
                    setFeature(base, f, 2, spriteType);
                }
                if (numberOfSprites > 3) {
                    f = spriteType * 941;
                    setFeature(base, f, 4, spriteType);
                }
                if (numberOfSprites > 7) {
                    f = spriteType * 101323;
                    setFeature(base, f, 8, spriteType);
                }
                if (numberOfSprites > 15) {
                    f = spriteType * 2531;
                    setFeature(base, f, 16, spriteType);
                }
            }
        }
    }

    private void setFeature (State base, int f, int n, int spriteType) {
        base.setFeature(f, 1.0);
        if (FeatureSetLibrary.debug) {
            FeatureSetLibrary.registerFeature(f,  "Global", String.format("At least %d sprites of type %d", n, spriteType));
        } else {
            FeatureSetLibrary.registerFeature(f,  "Global");
        }
    }
}
