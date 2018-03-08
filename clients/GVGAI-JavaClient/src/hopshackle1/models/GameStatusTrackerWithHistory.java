package hopshackle1.models;

import hopshackle1.*;
import org.javatuples.*;
import serialization.*;

import java.util.*;

public class GameStatusTrackerWithHistory extends GameStatusTracker {

    private Map<Integer, List<Pair<Integer, Vector2d>>> trajectories = new HashMap(); // full trajectories for each sprite, by tick number
    private Map<Integer, List<Pair<Integer, Vector2d>>> velocityHistory = new HashMap(); // full trajectories for each sprite, by tick number
    private Map<Integer, Types.ACTIONS> avatarActions = new HashMap();
    private Map<Integer, Pair<Integer, Integer>> lifeSpan = new HashMap();

    public GameStatusTrackerWithHistory() {

    }

    public GameStatusTrackerWithHistory(GameStatusTrackerWithHistory gst) {
        super(gst);
        for (Integer id : gst.trajectories.keySet()) {
            trajectories.put(id, HopshackleUtilities.cloneList(gst.trajectories.get(id)));
        }
        avatarActions = HopshackleUtilities.cloneMap(gst.avatarActions);
        for (Integer id : gst.lifeSpan.keySet()) {
            Pair<Integer, Integer> old = gst.lifeSpan.get(id);
            lifeSpan.put(id, new Pair(old.getValue0(), old.getValue1()));
        }
    }

    /*
 This assumes the provided SerializableStateObservation is the next one in sequence
  */
    public void update(SerializableStateObservation sso) {
        Map<Integer, Vector2d> previousPositions = HopshackleUtilities.cloneMap(currentPositions);
        // record this before the superclass updates it

        super.update(sso);

        int currentTick = getCurrentTick();
        Vector2d newAvatarPos = getCurrentPosition(0);
        Vector2d avatarVelocity = getCurrentVelocity(0);
        List<Pair<Integer, Vector2d>> avatarTraj = trajectories.getOrDefault(0, new ArrayList());
        avatarTraj.add(new Pair(currentTick, newAvatarPos));
        trajectories.put(0, avatarTraj);
        if (avatarVelocity != null) {
            List<Pair<Integer, Vector2d>> velHist = velocityHistory.getOrDefault(0, new ArrayList());
            velHist.add(new Pair(currentTick, avatarVelocity));
            velocityHistory.put(0, velHist);
        }

        avatarActions.put(currentTick, sso.avatarLastAction);
        for (int i = 1; i <= 6; i++) {
            List<Integer> sprites = getAllSpritesOfCategory(i);
            for (int id : sprites) {
                Vector2d currentPos = getCurrentPosition(id);
                Vector2d velocity = getCurrentVelocity(id);
                List<Pair<Integer, Vector2d>> traj = new ArrayList();
                if (!trajectories.containsKey(id)) {
                    traj.add(new Pair(currentTick, currentPos));
                    trajectories.put(id, traj);
                    lifeSpan.put(id, new Pair(currentTick, -1));
                } else if (i == SSOModifier.TYPE_NPC || i == SSOModifier.TYPE_MOVABLE || i == SSOModifier.TYPE_FROMAVATAR) {
                    // we always track starting position - but we currently assume that only the types below can move
                    traj = trajectories.get(id);
                    traj.add(new Pair(currentTick, currentPos));
                    List<Pair<Integer, Vector2d>> velocityTraj = velocityHistory.getOrDefault(id, new ArrayList());
                    velocityTraj.add(new Pair(currentTick, velocity));
                    velocityHistory.put(id, velocityTraj);
                }
            }
        }
        // now we check for any previously tracked sprites that have vanished
        for (int id : previousPositions.keySet()) {
            if (!currentPositions.containsKey(id)) {
                // has vanished
                lifeSpan.put(id, new Pair(lifeSpan.get(id).getValue0(), currentTick));
            }
        }
    }


    public List<Pair<Integer, Vector2d>> getTrajectory(int id) {
        List<Pair<Integer, Vector2d>> retValue = trajectories.getOrDefault(id, new ArrayList());
        return HopshackleUtilities.cloneList(retValue);
    }

    public List<Pair<Integer, Vector2d>> getVelocityTrajectory(int id) {
        List<Pair<Integer, Vector2d>> retValue = velocityHistory.getOrDefault(id, new ArrayList());
        return HopshackleUtilities.cloneList(retValue);
    }

    public Types.ACTIONS getAvatarAction(int tick) {
        return avatarActions.getOrDefault(tick, Types.ACTIONS.ACTION_NIL);
    }

    public Pair<Integer, Integer> getLifeSpan(int id) {
        return lifeSpan.getOrDefault(id, new Pair(0, 0));
    }

}
