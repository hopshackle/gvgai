package hopshackle1.models;

import hopshackle1.*;
import org.javatuples.*;
import serialization.*;
import serialization.Types.*;

import java.util.*;

public class SimpleAvatarModel implements BehaviourModel {

    private Random rnd = new Random(46);
    private Map<ACTIONS, double[]> counts = new HashMap(); // the count of the new position after each action
    private int blockSize;
    //  7 0 1
    //  6 8 2
    //  5 4 3       direction is one of these numbers - half-right hand turns from straight up
    private int[] xChange = {0, 1, 1, 1, 0, -1, -1, -1, 0};
    private int[] yChange = {-1, -1, 0, 1, 1, 1, 0, -1, 0};
    private Map<Integer, Pair<Double, Double>> passable = new HashMap();
    private EntityLog logFile = new EntityLog("SimpleAvatarModel");
    boolean debug = false;
    int count = 0;
    double DISTANCE_THRESHOLD = 1.0;
    private double MAX_X, MAX_Y;

    public SimpleAvatarModel(int block, double maxX, double maxY) {
        blockSize = block;
        MAX_X = maxX;
        MAX_Y = maxY;
        setDefaultPrior();
    }

    private void setDefaultPrior() {
        counts.put(ACTIONS.ACTION_LEFT, new double[]{0, 0, 0, 0, 0, 0, 5, 0, 0});
        counts.put(ACTIONS.ACTION_RIGHT, new double[]{0, 0, 5, 0, 0, 0, 0, 0, 0});
        counts.put(ACTIONS.ACTION_UP, new double[]{5, 0, 0, 0, 0, 0, 0, 0, 0});
        counts.put(ACTIONS.ACTION_DOWN, new double[]{0, 0, 0, 0, 5, 0, 0, 0, 0});
        counts.put(ACTIONS.ACTION_NIL, new double[]{0, 0, 0, 0, 0, 0, 0, 0, 5});
        counts.put(ACTIONS.ACTION_ESCAPE, new double[]{0, 0, 0, 0, 0, 0, 0, 0, 5});
        counts.put(ACTIONS.ACTION_USE, new double[]{0, 0, 0, 0, 0, 0, 0, 0, 5});
    }

    @Override
    public void updateModelStatistics(GameStatusTrackerWithHistory gst) {
        List<Pair<Integer, Vector2d>> avatarVelocities = gst.getVelocityTrajectory(0);
        List<Pair<Integer, Vector2d>> avatarPositions = gst.getTrajectory(0);
        Vector2d lastPosition = null;
        GameStatusTracker lastGST = null;
        if (debug)
            logFile.log(String.format("\nProcessing trajectory %d of %d/%d items", count, avatarPositions.size(), avatarVelocities.size()));
        count++;
        for (int i = 0; i < avatarPositions.size(); i++) {
            int tick = avatarVelocities.get(i).getValue0();
            ACTIONS lastMove = gst.getLastAvatarAction(tick);
            Vector2d v = avatarVelocities.get(i).getValue1();
            Vector2d pos = avatarPositions.get(i).getValue1();
            if (debug) {
                logFile.log(String.format("Tick: %d\t%s\tPos: %s\tV: %s", tick, lastMove, pos, v));
            }
            double pPassable = 1.0, pBaseMove = 1.00, pMove = 1.00;

            if (lastPosition != null && !offScreen(pos)) {
                List<Pair<Double, Vector2d>> predictions = nextMovePdf(lastGST, 0, lastMove);
                List<Pair<Double, Vector2d>> basePredictions = nextMovePdfWithoutPassability(lastGST, lastMove);

                for (int j = 0; j < predictions.size(); j++) {
                    if (predictions.get(j).getValue1().dist(pos) < DISTANCE_THRESHOLD) {
                        pMove = predictions.get(j).getValue0();
                        pBaseMove = basePredictions.get(j).getValue0();
                        pPassable = pMove / pBaseMove;
                    }
                }

                GameStatusTracker newGST = gst.getGST(tick);
                if (blockOf(pos) != blockOf(lastPosition)) {    // we did move
                    List<Pair<Integer, Vector2d>> collisionIDs = SSOModifier.newCollisionsOf(0, SSOModifier.TYPE_AVATAR, newGST.getCurrentSSO(), pos);
                    if (debug)
                        logFile.log(String.format("Block has changed with %d new collisions", collisionIDs.size()));
                    for (Pair<Integer, Vector2d> c : collisionIDs) {
                        if (debug) logFile.log(String.format("Object %d at %s", c.getValue0(), c.getValue1()));
                        updatePassability(gst.getType(c.getValue0()), 1.0 / pBaseMove);
                    }
                } else { // we stayed in same block
                    // now the counterfactual for moves that did not happen
                    if (debug) logFile.log("Block has not changed");
                    for (int j = 0; j < predictions.size(); j++) {
                        Pair<Double, Vector2d> pred = predictions.get(j);
                        Pair<Double, Vector2d> basePred = basePredictions.get(j);
                        if (debug)
                            logFile.log(String.format("Processing counterfactual for %.2f (%.2f) probability of %s",
                                    pred.getValue0(), basePred.getValue0(), pred.getValue1().toString()));
                        boolean movedBlock = blockOf(pred.getValue1()) != blockOf(lastPosition);
                        if (movedBlock) { // should have moved; but stayed in same place
                            List<Pair<Integer, Vector2d>> collisionIDs = SSOModifier.newCollisionsOf(0, SSOModifier.TYPE_AVATAR, newGST.getCurrentSSO(), pred.getValue1());
                            if (debug)
                                logFile.log(String.format("Would have moved and had %d collisions in that scenario", collisionIDs.size()));
                            for (Pair<Integer, Vector2d> c : collisionIDs) {
                                if (debug)
                                    logFile.log(String.format("Object %d at %s", c.getValue0(), c.getValue1()));
                                updatePassability(gst.getType(c.getValue0()), -basePred.getValue0());
                            }
                        }
                    }
                }

                double[] count = counts.get(lastMove);
                int heading = HopshackleUtilities.directionOf(v).getValue1();
                if (v.mag() < blockSize / 2.0) {
                    heading = 8;
                }
                if (debug)
                    logFile.log(String.format("Incrementing count for heading %d by %.2f", heading, 1.0 / pPassable));
                count[heading] += 1.0 / pPassable;
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

    private boolean offScreen(Vector2d pos) {
        return (pos == null || pos.x < 0.0 || pos.y < 0.0 || pos.x >= MAX_X || pos.y >= MAX_Y);
    }

    private void updatePassability(int type, double change) {
        if (!passable.containsKey(type)) {
            passable.put(type, new Pair(1.0, 1.0));
        }
        Pair<Double, Double> currentVal = passable.get(type);
        if (debug) logFile.log(String.format("Updating passability of type %d by amount %.2f", type, change));
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
        List<Pair<Double, Vector2d>> startingPoint = nextMovePdfWithoutPassability(gst, avatarMove);
        List<Pair<Double, Vector2d>> retValue = new ArrayList();
        List<Double> updatedPdf = new ArrayList();
        Vector2d currentPosition = gst.getCurrentPosition(0);

        for (Pair<Double, Vector2d> base : startingPoint) {
            Vector2d newPosition = base.getValue1();
            if (blockOf(newPosition) != blockOf(currentPosition)) {
                // modify p to take account of passability
                double p = passableProbability(gst, newPosition);
                updatedPdf.add(p * base.getValue0());
            } else {
                updatedPdf.add(base.getValue0());
            }
        }
        double totalP = 0.00;
        for (double p : updatedPdf) {
            totalP += p;
        }

        double[] newPdf = HopshackleUtilities.normalise(updatedPdf);

        for (int i = 0; i < newPdf.length; i++) {
            retValue.add(new Pair(newPdf[i], startingPoint.get(i).getValue1()));
        }

        return retValue;
    }

    public List<Pair<Double, Vector2d>> nextMovePdfWithoutPassability(GameStatusTracker gst, ACTIONS avatarMove) {
        double[] count = counts.get(avatarMove);
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
            if (count[i] > 0 || i == 8) { // always include the option of not moving at all
                double p = count[i] / totalCount;
                if (i == 8 && p < 10e-8) p = 10e-8;
                double new_x = currentPosition.x + blockSize * xChange[i];
                double new_y = currentPosition.y + blockSize * yChange[i];
                Vector2d newPosition = new Vector2d(new_x, new_y);

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

    private double passableProbability(GameStatusTracker gst, Vector2d nextPosition) {
        double p = 1.00;
        if (offScreen(nextPosition)) {
            return 0.00;
        }
        List<Pair<Integer, Vector2d>> collisionIDs = SSOModifier.newCollisionsOf(0, SSOModifier.TYPE_AVATAR, gst.getCurrentSSO(), nextPosition);
        for (Pair<Integer, Vector2d> c : collisionIDs) {
            int type = gst.getType(c.getValue0());
            if (passable.containsKey(type)) {
                p *= passable.get(type).getValue0() / (passable.get(type).getValue0() + passable.get(type).getValue1());
            }
        }
        return p;
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
                    HopshackleUtilities.formatArray(counts.get(action), "|", "%.1f"))
            );
        }
        for (int k : passable.keySet()) {
            double p = passable.get(k).getValue0() / (passable.get(k).getValue0() + passable.get(k).getValue1());
            retValue.append(String.format("P%d=%.2f (%.1f : %.1f)\t", k, p, passable.get(k).getValue0(), passable.get(k).getValue1()));
        }
        return retValue.toString();
    }
}
