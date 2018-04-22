package hopshackle1.models;

import serialization.*;
import serialization.Types.*;
import hopshackle1.*;

import java.util.*;

import org.javatuples.*;

public class GameStatusTracker {
    //  7 0 1
    //  6 - 2
    //  5 4 3       direction change is one of these numbers - half-right hand turns from straight up

    private int blockSize = -1;
    private int currentTick;
    private double[] worldDimension = new double[2];
    protected Map<Integer, Vector2d> currentPositions = new HashMap(); // last position for each tracked sprite
    private Map<Integer, Vector2d> currentVelocities = new HashMap(); // last velocity for each tracked sprite
    private Map<Integer, Vector2d> lastDirection = new HashMap(); // last non-zero velocity for each tracked sprite
    private Map<Integer, Pair<Integer, Integer>> IDToCategoryAndType = new HashMap();
    private SerializableStateObservation currentSSO;
    private static final Vector2d stationary = new Vector2d(0, 0);
    private boolean integrityHolds = true;

    public GameStatusTracker() {

    }

    public GameStatusTracker(GameStatusTracker gst) {
        currentSSO = SSOModifier.copy(gst.currentSSO);
        blockSize = gst.blockSize;
        currentTick = gst.currentTick;
        worldDimension = gst.worldDimension;
        currentPositions = HopshackleUtilities.cloneMap(gst.currentPositions);
        currentVelocities = HopshackleUtilities.cloneMap(gst.currentVelocities);
        lastDirection = HopshackleUtilities.cloneMap(gst.lastDirection);
        IDToCategoryAndType = HopshackleUtilities.cloneMap(gst.IDToCategoryAndType);
        integrityHolds = gst.integrityHolds;
    }

    /*
    This assumes the provided SerializableStateObservation is the next one in sequence
     */
    public void update(SerializableStateObservation sso) {
        if (!integrityHolds) {
            throw new AssertionError("Should only be called when in integral state");
        }
        if (blockSize == -1) {
            blockSize = sso.blockSize;
            worldDimension[0] = sso.worldDimension[0];
            worldDimension[1] = sso.worldDimension[1];
            IDToCategoryAndType.put(0, new Pair(SSOModifier.TYPE_AVATAR, sso.avatarType));
        } else {
            if (sso.gameTick - currentTick != 1) {
                //  throw new AssertionError("We expect to have every game tick registered. Actual difference is " + (sso.gameTick - currentTick));
            }
        }

        Vector2d lastAvatarPos = currentPositions.get(0);
        currentTick = sso.gameTick;
        Vector2d newAvatarPos = new Vector2d(sso.avatarPosition[0], sso.avatarPosition[1]);
        currentPositions.put(0, new Vector2d(sso.avatarPosition[0], sso.avatarPosition[1]));
        Vector2d avatarVelocity = (lastAvatarPos == null) ? stationary : new Vector2d(newAvatarPos).subtract(lastAvatarPos);
        currentVelocities.put(0, avatarVelocity);
        if (!avatarVelocity.equals(stationary)) {
            lastDirection.put(0, avatarVelocity);
        }

        Set<Integer> allCurrentIDs = new HashSet();
        if (sso.isAvatarAlive) allCurrentIDs.add(0);
        for (int i = 1; i <= 6; i++) {
            List<Pair<Integer, Integer>> sprites = SSOModifier.getSpritesOfCategory(i, sso);
            for (Pair<Integer, Integer> sprite : sprites) {
                int id = sprite.getValue0();
                int type = sprite.getValue1();
                allCurrentIDs.add(id);
                Vector2d lastPos = currentPositions.getOrDefault(id, null);
                Vector2d currentPos = SSOModifier.positionOf(id, sso);
                Vector2d velocity = lastPos == null ? new Vector2d(0, 0) : new Vector2d(currentPos).subtract(lastPos);
                currentPositions.put(id, currentPos);
                currentVelocities.put(id, velocity);
                if (!velocity.equals(stationary)) {
                    lastDirection.put(id, velocity);
                }
                if (!IDToCategoryAndType.containsKey(id)) {
                    IDToCategoryAndType.put(id, new Pair(i, type));
                }
            }
        }
        // now we check for any previously tracked sprites that have vanished
        List<Integer> idsToRemove = new ArrayList();
        for (int id : currentPositions.keySet()) {
            if (!allCurrentIDs.contains(id)) {
                // has vanished
                idsToRemove.add(id);
            }
        }
        for (Integer id : idsToRemove) {
            currentPositions.remove(id);
            currentVelocities.remove(id);
            lastDirection.remove(id);
        }
        currentSSO = sso;
    }


    public Vector2d getCurrentPosition(int id) {
        return currentPositions.getOrDefault(id, null);
    }

    public Vector2d getCurrentVelocity(int id) {
        return currentVelocities.getOrDefault(id, null);
    }

    public Vector2d getLastDirection(int id) {
        return lastDirection.getOrDefault(id, null);
    }

    public boolean isExtant(int id) {
        return currentPositions.containsKey(id);
    }

    public int getCategory(int id) {
        Pair<Integer, Integer> temp = IDToCategoryAndType.getOrDefault(id, new Pair(-1, -1));
        return temp.getValue0();
    }

    public int getType(int id) {
        Pair<Integer, Integer> temp = IDToCategoryAndType.getOrDefault(id, new Pair(-1, -1));
        return temp.getValue1();
    }

    public List<Integer> getAllSpritesOfType(int type) {
        List<Integer> retValue = new ArrayList();
        for (Integer id : IDToCategoryAndType.keySet()) {
            if (IDToCategoryAndType.get(id).getValue1() == type && currentPositions.containsKey(id))
                retValue.add(id);
        }
        return retValue;
    }

    public List<Integer> getAllSpritesOfCategory(int category) {
        List<Integer> retValue = new ArrayList();
        for (Integer id : IDToCategoryAndType.keySet()) {
            if (IDToCategoryAndType.get(id).getValue0() == category && currentPositions.containsKey(id))
                retValue.add(id);
        }
        return retValue;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void rollForward(List<BehaviourModel> models, ACTIONS avatarMove, boolean useMAP) {
        rollForwardSprites(models, useMAP);
        List<Integer> temp = this.getAllSpritesOfCategory(SSOModifier.TYPE_AVATAR);
        int avatar = temp.get(0);
        for (BehaviourModel avatarModel : models) {
            if (avatarModel.isValidFor(this, avatar)) {
                rollForwardAvatar(avatarModel, avatarMove, useMAP);
                break;
            }
        }
    }

    public void rollForwardSprites(List<BehaviourModel> models, boolean useMAP) {
        currentSSO.gameTick++;
        for (BehaviourModel model : models) {
            for (Integer id : currentPositions.keySet()) {
                if (getCategory(id) == SSOModifier.TYPE_AVATAR)
                    continue;
                if (model.isValidFor(this, id)) {
                    Vector2d move = null;
                    if (useMAP) {
                        move = model.nextMoveMAP(this, id, ACTIONS.ACTION_NIL);
                    } else {
                        move = model.nextMoveRandom(this, id, ACTIONS.ACTION_NIL);
                    }
                    SSOModifier.moveSprite(id, getCategory(id), move.x, move.y, currentSSO);
                }
            }
        }
        SSOModifier.constructGrid(currentSSO);
        integrityHolds = false;
    }

    public void rollForwardSprites(BehaviourModel model, boolean useMAP) {
        rollForwardSprites(HopshackleUtilities.listFromInstance(model), useMAP);
    }

    public void rollForwardAvatar(BehaviourModel avatarModel, ACTIONS avatarMove, boolean useMAP) {
        if (integrityHolds) {
            throw new AssertionError("Should only be called when in non-integral state");
        }
        currentSSO.avatarLastAction = avatarMove == null ? ACTIONS.ACTION_NIL : avatarMove;
        List<Integer> temp = this.getAllSpritesOfCategory(SSOModifier.TYPE_AVATAR);
        if (temp.size() != 1)
            throw new AssertionError("Only expecting one Avatar ID");
        int avatar = temp.get(0);
        Vector2d move = null;
        if (useMAP) {
            move = avatarModel.nextMoveMAP(this, avatar, avatarMove);
        } else {
            move = avatarModel.nextMoveRandom(this, avatar, avatarMove);
        }
        SSOModifier.moveSprite(avatar, SSOModifier.TYPE_AVATAR, move.x, move.y, currentSSO);
        integrityHolds = true;
        update(currentSSO);
    }

    public void rollForward(BehaviourModel model, ACTIONS avatarMove, boolean useMAP) {
        rollForward(HopshackleUtilities.listFromInstance(model), avatarMove, useMAP);
    }

    public void rollForward(BehaviourModel model, ACTIONS avatarMove) {
        rollForward(model, avatarMove, false);
    }


    public SerializableStateObservation getCurrentSSO() {
        return currentSSO;
    }

    public Set<Integer> listOfTypes() {
        Set<Integer> retValue = new HashSet();
        for (int id : IDToCategoryAndType.keySet()) {
            retValue.add(IDToCategoryAndType.get(id).getValue1());
        }
        return retValue;
    }

    public Set<Integer> listOfCategories() {
        Set<Integer> retValue = new HashSet();
        for (int id : IDToCategoryAndType.keySet()) {
            retValue.add(IDToCategoryAndType.get(id).getValue0());
        }
        return retValue;
    }

}
