package hopshackle1.models;

import hopshackle1.*;
import org.javatuples.Pair;
import serialization.*;
import serialization.Types.*;
import java.util.*;

public class BehaviouralLookaheadFunction implements LookaheadFunction, BehaviourModel {

    private Map<Integer, BehaviourModel> spriteTypeToModel = new HashMap();
    private SimpleAvatarModel avatarModel;
    private Map<Integer, Integer> IDToSpriteType = new HashMap();
    private int blockSize = -1;

    @Override
    public SerializableStateObservation rollForward(SerializableStateObservation sso, Types.ACTIONS action) {
        SerializableStateObservation retValue = SSOModifier.copy(sso);
        for (BehaviourModel model : spriteTypeToModel.values()) {
            model.apply(retValue, action);
        }
        return retValue;
    }

    @Override
    public void associateWithSprite(int type) {
        // do nothing
    }

    @Override
    public void reset(SerializableStateObservation sso) {
        for(BehaviourModel model : spriteTypeToModel.values()) {
            model.reset(sso);
        }
    }

    @Override
    public void updateModelStatistics(SerializableStateObservation sso) {
        if (blockSize == -1) blockSize = sso.blockSize;
        checkForNewSpriteTypes(sso);
        for(BehaviourModel model : spriteTypeToModel.values()) {
            model.updateModelStatistics(sso);
        }
    }

    @Override
    public void apply(SerializableStateObservation sso, ACTIONS avatarMove) {
        for(BehaviourModel model : spriteTypeToModel.values()) {
            model.apply(sso, avatarMove);
        }
    }

    @Override
    public Vector2d nextMoveMAP(int objID, ACTIONS avatarMove) {
        BehaviourModel model = getModelForID(objID);
        return model.nextMoveMAP(objID, avatarMove);
    }

    @Override
    public Vector2d nextMoveRandom(int objID, ACTIONS avatarMove) {
        BehaviourModel model = getModelForID(objID);
        return model.nextMoveRandom(objID, avatarMove);
    }

    @Override
    public List<Pair<Double, Vector2d>> nextMovePdf(int objID, ACTIONS avatarMove) {
        BehaviourModel model = getModelForID(objID);
        return model.nextMovePdf(objID, avatarMove);
    }

    private BehaviourModel getModelForID(int objID) {
        if (!IDToSpriteType.containsKey(objID))
            throw new AssertionError("No record of objID " + objID);
        int spriteType =  IDToSpriteType.get(objID);
        return spriteTypeToModel.get(spriteType);
    }

    private void checkForNewSpriteTypes(SerializableStateObservation sso) {
        if (avatarModel == null) {
            avatarModel = new SimpleAvatarModel(sso.blockSize);
            spriteTypeToModel.put(sso.avatarType, avatarModel);
        }
        checkForNewSpriteTypesIn(sso.NPCPositions);
        checkForNewSpriteTypesIn(sso.movablePositions);
        checkForNewSpriteTypesIn(sso.fromAvatarSpritesPositions);
    }

    private void checkForNewSpriteTypesIn(Observation[][] positions) {
        for (Observation[] o1 : positions) {
            if (o1.length > 0 && !spriteTypeToModel.containsKey(o1[0].itype)) {
                BehaviourModel newModel = new SimpleSpriteModel(blockSize);
                newModel.associateWithSprite(o1[0].itype);
                spriteTypeToModel.put(o1[0].itype, newModel);
            }
            for (Observation o : o1) {
                if (!IDToSpriteType.containsKey(o.obsID)) {
                    IDToSpriteType.put(o.obsID, o.itype);
                }
            }
        }
    }

}
