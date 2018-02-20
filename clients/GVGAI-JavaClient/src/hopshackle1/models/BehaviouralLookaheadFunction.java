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
        SSOModifier.constructGrid(sso);
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
            IDToSpriteType.put(0, sso.avatarType);
            // There is no record of the Avatar ID in SSO, other than in Observation Grid.
            // For testing we always use an ID of 0 (or when we construct/roll forward any new SSO
            /*
            for (int i = 0; i < sso.observationGrid.length; i++) {
                for (int j = 0; j < sso.observationGrid[i].length; j++) {
                    for (int k = 0; k < sso.observationGrid[i][j].length ; k++) {
                        if (sso.observationGrid[i][j][k] != null && sso.observationGrid[i][j][k].itype == sso.avatarType) {
                            IDToSpriteType.put(sso.observationGrid[i][j][k].obsID, sso.avatarType);
                        }
                    }
                }
            } */
        }
        checkForNewSpriteTypesIn(sso.NPCPositions);
        checkForNewSpriteTypesIn(sso.movablePositions);
        checkForNewSpriteTypesIn(sso.fromAvatarSpritesPositions);
    }

    private void checkForNewSpriteTypesIn(Observation[][] positions) {
        if (positions == null) return;
        for (Observation[] o1 : positions) {
            if (o1 != null && o1.length > 0 && !spriteTypeToModel.containsKey(o1[0].itype)) {
                BehaviourModel newModel = new SimpleSpriteModel(blockSize);
                newModel.associateWithSprite(o1[0].itype);
                spriteTypeToModel.put(o1[0].itype, newModel);
            }
            for (Observation o : o1) {
                if (o != null && !IDToSpriteType.containsKey(o.obsID)) {
                    IDToSpriteType.put(o.obsID, o.itype);
                }
            }
        }
    }

}
