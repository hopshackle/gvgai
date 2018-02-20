package hopshackle1.models;

import serialization.*;
import hopshackle1.*;

import java.util.*;

import org.javatuples.*;

public class GameStatusTracker {
    //  7 0 1
    //  6 - 2
    //  5 4 3       direction change is one of these numbers - half-right hand turns from straight up

    private int blockSize;
    private int currentTick;
    private Map<Integer, List<Pair<Integer, Vector2d>>> trajectories = new HashMap(); // full trajectories for each sprite, by tick number
    private Map<Integer, Vector2d> currentPositions = new HashMap(); // last position for each tracked sprite
    private Map<Integer, Pair<Integer, Integer>> IDToCategoryAndType = new HashMap();
    private Map<Integer, Vector2d> avatarTrajectory = new HashMap();
    private Map<Integer, Pair<Integer, Integer>> lifeSpan = new HashMap();

    public GameStatusTracker(SerializableStateObservation sso) {
        blockSize = sso.blockSize;
        currentTick = sso.gameTick;
    }

    /*
    This assumes the provided SerializableStateObservation is the next one in sequence
     */
    public void update(SerializableStateObservation sso) {
        if (sso.gameTick - currentTick > 1)
            throw new AssertionError("We expect to have every game tick registered. Actual difference is " + (sso.gameTick - currentTick));
        currentTick = sso.gameTick;
        avatarTrajectory.put(currentTick, new Vector2d(sso.avatarPosition[0], sso.avatarPosition[1]));
        currentPositions.put(0, new Vector2d(sso.avatarPosition[0], sso.avatarPosition[1]));

        Set<Integer> allCurrentIDs = new HashSet();
        for (int i = 1; i <= 6; i++) {
            List<Pair<Integer, Integer>> sprites = SSOModifier.getSpritesOfCategory(i, sso);
            for (Pair<Integer, Integer> sprite : sprites) {
                int id = sprite.getValue0();
                int type = sprite.getValue1();
                allCurrentIDs.add(id);
                Vector2d currentPos = SSOModifier.positionOf(id, sso);
                currentPositions.put(id, currentPos);
                List<Pair<Integer, Vector2d>> traj = new ArrayList();
                if (!IDToCategoryAndType.containsKey(id)) {
                    IDToCategoryAndType.put(id, new Pair(i, type));
                    traj.add(new Pair(currentTick, currentPos));
                    trajectories.put(id, traj);
                    lifeSpan.put(id, new Pair(currentTick, -1));
                }
                // we always track starting position - but we currently assume that only the types below can move
                if (i == SSOModifier.TYPE_NPC || 1 == SSOModifier.TYPE_MOVABLE || i == SSOModifier.TYPE_FROMAVATAR) {
                    if (trajectories.containsKey(id)) {
                        traj = trajectories.get(id);
                        traj.add(new Pair(currentTick, currentPos));
                    }
                }
            }
        }
        // now we check for any previously tracked sprites that have vanished
        List<Integer> idsToRemove = new ArrayList();
        for (int id : currentPositions.keySet()) {
            if (!allCurrentIDs.contains(id)) {
                // has vanished
                lifeSpan.put(id, new Pair(lifeSpan.get(id).getValue0(), currentTick));
                idsToRemove.add(id);
            }
        }
        for (Integer id : idsToRemove) {
            currentPositions.remove(id);
        }
    }

    public List<Pair<Integer, Vector2d>> getTrajectory(int id) {
        List<Pair<Integer, Vector2d>>  retValue = trajectories.getOrDefault(id, new ArrayList());
        return HopshackleUtilities.cloneList(retValue);
    }

    public Vector2d getCurrentPosition(int id) {
        return currentPositions.getOrDefault(id, null);
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
    public Pair<Integer, Integer> getLifeSpan(int id) {
        return lifeSpan.getOrDefault(id, new Pair(0, 0));
    }
    public int getCurrentTick() {return currentTick;}
    public int getBlockSize() {return blockSize;}
}
