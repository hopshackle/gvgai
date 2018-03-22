package hopshackle1.models;

import hopshackle1.HopshackleUtilities;
import org.javatuples.*;
import serialization.*;
import serialization.Types.*;

import java.util.*;

public class SimpleAvatarModel implements BehaviourModel {

    private Random rnd = new Random(46);
    private Map<ACTIONS, int[]> counts = new HashMap(); // the count of the new position after each action
    private int blockSize;
    //  7 0 1
    //  6 8 2
    //  5 4 3       direction is one of these numbers - half-right hand turns from straight up
    private int[] xChange = {0, 1, 1, 1, 0, -1, -1, -1, 0};
    private int[] yChange = {-1, -1, 0, 1, 1, 1, 0, -1, 0};
    private Map<Integer, Pair<Double, Double>> passable = new HashMap();

    public SimpleAvatarModel(int block) {
        blockSize = block;
        setDefaultPrior();
    }

    private void setDefaultPrior() {
        counts.put(ACTIONS.ACTION_LEFT, new int[]{0, 0, 0, 0, 0, 0, 5, 0, 0});
        counts.put(ACTIONS.ACTION_RIGHT, new int[]{0, 0, 5, 0, 0, 0, 0, 0, 0});
        counts.put(ACTIONS.ACTION_UP, new int[]{5, 0, 0, 0, 0, 0, 0, 0, 0});
        counts.put(ACTIONS.ACTION_DOWN, new int[]{0, 0, 0, 0, 5, 0, 0, 0, 0});
        counts.put(ACTIONS.ACTION_NIL, new int[]{0, 0, 0, 0, 0, 0, 0, 0, 5});
        counts.put(ACTIONS.ACTION_ESCAPE, new int[]{0, 0, 0, 0, 0, 0, 0, 0, 5});
        counts.put(ACTIONS.ACTION_USE, new int[]{0, 0, 0, 0, 0, 0, 0, 0, 5});
    }

    @Override
    public void updateModelStatistics(GameStatusTrackerWithHistory gst) {
        List<Pair<Integer, Vector2d>> avatarVelocities = gst.getVelocityTrajectory(0);
        List<Pair<Integer, Vector2d>> avatarPositions = gst.getTrajectory(0);
        Vector2d lastPosition = null;
        GameStatusTracker lastGST = null;
        for (int i = 0; i < avatarVelocities.size(); i++) {
            int tick = avatarVelocities.get(i).getValue0();
            ACTIONS lastMove = gst.getLastAvatarAction(tick);
            Vector2d v = avatarVelocities.get(i).getValue1();
            Vector2d pos = avatarPositions.get(i).getValue1();
            if (lastPosition != null) {
                if (blockOf(pos) != blockOf(lastPosition)) {    // we did move
                    Set<Integer> collisionIDs = SSOModifier.newCollisionsOf(0, SSOModifier.TYPE_AVATAR, gst.getCurrentSSO(), pos);
                    for (int c : collisionIDs) {
                        updatePassability(gst.getType(c), 1.0);
                    }
                } else { // we stayed in same block
                    // now the counterfactual for moves that did not happen
                    List<Pair<Double, Vector2d>> predictions = nextMovePdf(lastGST, 0, lastMove);
                    for (Pair<Double, Vector2d> pred : predictions) {
                        boolean movedBlock = blockOf(pred.getValue1()) == blockOf(lastPosition);
                        if (movedBlock) { // should have moved; but stayed in same place
                            Set<Integer> collisionIDs = SSOModifier.newCollisionsOf(0, SSOModifier.TYPE_AVATAR, gst.getCurrentSSO(), pred.getValue1());
                            for (int c : collisionIDs) {
                                updatePassability(gst.getType(c), -pred.getValue0());
                            }
                        }
                    }
                }
            }

            int[] count = counts.get(lastMove);
            int heading = HopshackleUtilities.directionOf(v).getValue1();
            if (v.mag() < blockSize / 2.0) {
                count[8]++;
            } else {
                count[heading]++;
            }
            lastPosition = pos;
            lastGST = gst.getGST(tick);
        }
    }

    private int blockOf(Vector2d pos) {
        int x = (int) pos.x / blockSize;
        int y = (int) pos.y / blockSize;
        return x + 307 * y;
    }

    private void updatePassability(int type, double change) {
        if (!passable.containsKey(type)) {
            passable.put(type, new Pair(5.0, 5.0));
        }
        Pair<Double, Double> currentVal = passable.get(type);
        if (change > 0.00) {
            passable.put(type, new Pair(currentVal.getValue0() + change, currentVal.getValue1()));
        } else {
            passable.put(type, new Pair(currentVal.getValue0(), currentVal.getValue1() - change));
        }
    }

    @Override
    public Vector2d nextMoveMAP(GameStatusTracker gst, int objID, ACTIONS avatarMove) {
        Vector2d retValue = null;
        double maximum = 0.0;
        for (Pair<Double, Vector2d> option : nextMovePdf(gst, objID, avatarMove)) {
            if (option.getValue0() > maximum) {
                maximum = option.getValue0();
                retValue = option.getValue1();
            }
        }
        return retValue;
    }

    @Override
    public Vector2d nextMoveRandom(GameStatusTracker gst, int objID, ACTIONS avatarMove) {
        double roll = rnd.nextDouble();
        for (Pair<Double, Vector2d> option : nextMovePdf(gst, objID, avatarMove)) {
            roll -= option.getValue0();
            if (roll <= 0.0) {
                return option.getValue1();
            }
        }
        throw new AssertionError("Should not reach this point");
    }

    @Override
    public List<Pair<Double, Vector2d>> nextMovePdf(GameStatusTracker gst, int objID, ACTIONS avatarMove) {
        int[] count = counts.get(avatarMove);
        List<Pair<Double, Vector2d>> retValue = new ArrayList();
        List<Vector2d> positions = new ArrayList();
        List<Double> pdf = new ArrayList();
        Vector2d currentPosition = gst.getCurrentPosition(0);
        double totalCount = 0.0;
        for (int i = 0; i < count.length; i++) {
            totalCount += count[i];
        }
        // then the possibility of each move
        for (int i = 0; i < 9; i++) {
            if (count[i] > 0) {
                double p = (double) count[i] / totalCount;
                double new_x = currentPosition.x + blockSize * xChange[i];
                double new_y = currentPosition.y + blockSize * yChange[i];
                Vector2d newPosition = new Vector2d(new_x, new_y);

                if (blockOf(newPosition) != blockOf(currentPosition)) {
                    // modify p to take account of passability
                    Set<Integer> collisionIDs = SSOModifier.newCollisionsOf(0, SSOModifier.TYPE_AVATAR, gst.getCurrentSSO(), newPosition);
                    for (int c : collisionIDs) {
                        int type = gst.getType(c);
                        if (passable.containsKey(type)) {
                            p *= passable.get(type).getValue0() / (passable.get(type).getValue0() + passable.get(type).getValue1());
                        } else {
                            p *= 0.5;
                        }
                    }
                }
                pdf.add(p);
                positions.add(newPosition);
            }
        }
        double[] newPdf = HopshackleUtilities.normalise(pdf);
        for (int i = 0; i < positions.size(); i++) {
            retValue.add(new Pair(newPdf[i], positions.get(i)));
        }

        return retValue;
    }

    @Override
    public boolean isValidFor(GameStatusTracker gst, int id) {
        return (id == 0);
    }

    @Override
    public String toString() {
        StringBuilder retValue = new StringBuilder();
        for (ACTIONS action : counts.keySet()) {
            retValue.append(String.format("Action: %s\t%s\n", action,
                    HopshackleUtilities.formatArray(counts.get(action), "|", "%d"))
            );
        }
        for (int k : passable.keySet()) {
            double p = passable.get(k).getValue0() / (passable.get(k).getValue0() + passable.get(k).getValue1());
            retValue.append(String.format("P%d=%.2f (%.1f : %.1f)\t", k, p, passable.get(k).getValue0(), passable.get(k).getValue1()));
        }
        return retValue.toString();
    }
}
