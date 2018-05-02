package hopshackle1.FeatureSets;

import hopshackle1.*;
import serialization.*;

public class RareSpriteProximity implements FeatureSet {

    public int MIN_NUMBER = 5;

    @Override
    public void describeObservation(SerializableStateObservation obs, State state) {
        Vector2d avatarPosition = new Vector2d(obs.avatarPosition[0], obs.avatarPosition[1]);
        extractFeaturesFrom(state, obs.getNPCPositions(), avatarPosition, obs.blockSize);
        extractFeaturesFrom(state, obs.getMovablePositions(), avatarPosition, obs.blockSize);
        extractFeaturesFrom(state, obs.getImmovablePositions(), avatarPosition, obs.blockSize);
        extractFeaturesFrom(state, obs.getPortalsPositions(), avatarPosition, obs.blockSize);
        extractFeaturesFrom(state, obs.getResourcesPositions(), avatarPosition, obs.blockSize);
        extractFeaturesFrom(state, obs.getFromAvatarSpritesPositions(), avatarPosition, obs.blockSize);
    }

    private void extractFeaturesFrom(State base, Observation[][] obs, Vector2d avatarPosition, double blockSize) {
        // the first dimension of the array is sprite type
        if (obs == null) return;
        for (int i = 0; i < obs.length; i++) {
            if (obs[i].length < MIN_NUMBER && obs[i].length > 0 && obs[i][0] != null) {
                int spriteType = obs[i][0].itype;
                double closest = Double.POSITIVE_INFINITY;
                for (int j = 0; j < obs[i].length; j++) {
                    if (obs[i][j].position.dist(avatarPosition) < closest) {
                        closest = obs[i][j].position.dist(avatarPosition);
                    }
                }
                closest /= blockSize;
                int f = 0;
                if (closest < 2.0) {
                    f = spriteType * 263 + 89;
                    setFeature(base, f, spriteType, 2.0);
                }
                if (closest < 4.0) {
                    f = spriteType * 491 - 373;
                    setFeature(base, f, spriteType, 4.0);
                }
                if (closest < 8.0) {
                    f = spriteType * 1307 - 36583;
                    setFeature(base, f, spriteType, 8.0);
                }
            }
        }
    }

    private void setFeature(State base, int f, int spriteType, double distance) {
        base.setFeature(f, 1.0);
        if (FeatureSetLibrary.debug) {
            FeatureSetLibrary.registerFeature(f, "RareProximity", String.format("Sprite %d within %.1f", spriteType, distance));
        } else {
            FeatureSetLibrary.registerFeature(f, "RareProximity");
        }
    }
}
