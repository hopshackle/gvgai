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
    private boolean useMAP = true;
    private boolean allPassable = false;

    @Override
    public SerializableStateObservation rollForward(GameStatusTracker gst, Types.ACTIONS action) {
        GameStatusTracker gstCopy = new GameStatusTracker(gst);
        gstCopy.rollForward(this, action, useMAP);
        SerializableStateObservation retValue = gstCopy.getCurrentSSO();
        return retValue;
    }


    @Override
    public void updateModelStatistics(GameStatusTrackerWithHistory gst) {
        if (blockSize == -1) blockSize = gst.getBlockSize();
        checkForNewSpriteTypes(gst);
        for (BehaviourModel model : spriteTypeToModel.values()) {
            model.updateModelStatistics(gst);
        }
    }

    @Override
    public Vector2d nextMoveMAP(GameStatusTracker gst, int objID, ACTIONS avatarMove) {
        BehaviourModel model = spriteTypeToModel.get(gst.getType(objID));
        if (model == null) {
            return gst.getCurrentPosition(objID);
        }
        return model.nextMoveMAP(gst, objID, avatarMove);
    }

    @Override
    public Vector2d nextMoveRandom(GameStatusTracker gst, int objID, ACTIONS avatarMove) {
        BehaviourModel model = spriteTypeToModel.get(gst.getType(objID));
        if (model == null) {
            return gst.getCurrentPosition(objID);
        }
        return model.nextMoveRandom(gst, objID, avatarMove);
    }

    @Override
    public List<Pair<Double, Vector2d>> nextMovePdf(GameStatusTracker gst, int objID, ACTIONS avatarMove) {
        BehaviourModel model = spriteTypeToModel.get(gst.getType(objID));
        if (model == null) {
            List<Pair<Double, Vector2d>> retValue = new ArrayList();
            retValue.add(new Pair(1.0, gst.getCurrentPosition(objID)));
            return retValue;
        }
        return model.nextMovePdf(gst, objID, avatarMove);
    }

    public void setAllPassable(boolean flag) {
        allPassable = flag;
    }

    private void checkForNewSpriteTypes(GameStatusTracker gst) {
        if (avatarModel == null) {
            avatarModel = new SimpleAvatarModel(blockSize);
            spriteTypeToModel.put(gst.getType(0), avatarModel);
            IDToSpriteType.put(0, gst.getType(0));
        }
        List<Integer> allSpriteIDs = gst.getAllSpritesOfCategory(SSOModifier.TYPE_NPC);
        allSpriteIDs.addAll(gst.getAllSpritesOfCategory(SSOModifier.TYPE_FROMAVATAR));
        allSpriteIDs.addAll(gst.getAllSpritesOfCategory(SSOModifier.TYPE_MOVABLE));

        for (int id : allSpriteIDs) {
            int type = gst.getType(id);
            if (!spriteTypeToModel.containsKey(type)) {
                BehaviourModel newModel = new SimpleSpriteModel(type, allPassable);
                spriteTypeToModel.put(type, newModel);
            }
            if (!IDToSpriteType.containsKey(id))
                IDToSpriteType.put(id, type);
        }
    }

    @Override
    public boolean isValidFor(GameStatusTracker gst, int id) {
        int category = gst.getCategory(id);
        return (category == SSOModifier.TYPE_AVATAR || category == SSOModifier.TYPE_FROMAVATAR
                || category == SSOModifier.TYPE_MOVABLE || category == SSOModifier.TYPE_NPC);
    }

    @Override
    public String toString() {
        StringBuilder retValue = new StringBuilder();
        for (Integer type : spriteTypeToModel.keySet()) {
            retValue.append(String.format("\tType: %d\t%s\n", type, spriteTypeToModel.get(type).toString()));
        }
        return retValue.toString();
    }
}
