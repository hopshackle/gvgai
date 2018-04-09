package hopshackle1.models;

import hopshackle1.HopshackleUtilities;
import serialization.*;
import org.javatuples.*;

import java.util.*;

public class SimpleSpriteModel implements BehaviourModel {

    private int spriteType = -1;
    private Random rnd = new Random(46);
    private double[] counts = new double[8]; // the count of the turns made each move
    private double totalCount, staticCount;
    private double[] speed = new double[8];
    //  7 0 1
    //  6 - 2
    //  5 4 3       direction change is one of these numbers - half-right hand turns from straight up
    // xChange and yChange are the arrays of x, y co-ord changes for each of the 8 movement directions
    // We encode a turn as the number of half right turns compared to last known direction
    private Map<Integer, Double> passable = new HashMap();
    public static final Vector2d stationary = new Vector2d(0, 0);


    public SimpleSpriteModel(int spriteType) {
        staticCount = 5;
        totalCount = 5;
        this.spriteType = spriteType;
    }

    public SimpleSpriteModel(int[] countData, int stationaryCount, int type) {
        if (countData.length != 8)
            throw new AssertionError("CountData must have length of 8 in SimpleSpriteModel");
        for (int i = 0; i < 8; i++) {
            counts[i] = countData[i];
            totalCount += countData[i];
        }
        staticCount = stationaryCount;
        spriteType = type;
    }

    public void setRandom(Random newR) {
        rnd = newR;
    }


    @Override
    public Vector2d nextMoveMAP(GameStatusTracker gst, int objID, Types.ACTIONS move) {
        Vector2d retValue = null;
        double maximum = 0.0;
        for (Pair<Double, Vector2d> option : nextMovePdf(gst, objID, move)) {
            if (option.getValue0() > maximum) {
                maximum = option.getValue0();
                retValue = option.getValue1();
            }
        }
        return retValue;
    }

    @Override
    public Vector2d nextMoveRandom(GameStatusTracker gst, int objID, Types.ACTIONS move) {
        double roll = rnd.nextDouble();
        for (Pair<Double, Vector2d> option : nextMovePdf(gst, objID, move)) {
            roll -= option.getValue0();
            if (roll <= 0.0) {
                return option.getValue1();
            }
        }
        throw new AssertionError("Should not reach this point");
    }

    @Override
    public List<Pair<Double, Vector2d>> nextMovePdf(GameStatusTracker gst, int objID, Types.ACTIONS move) {
        Vector2d heading = gst.getLastDirection(objID);
        List<Pair<Double, Vector2d>> retValue = new ArrayList();
        List<Vector2d> positions = new ArrayList();
        List<Double> pdf = new ArrayList();

        // first the possibility of not moving
        double p = staticCount / totalCount;
        if (heading == null || heading.equals(stationary) || gst.getCurrentPosition(objID) == null) p = 1.0;
        Vector2d currentPos = gst.getCurrentPosition(objID);
        Pair<Double, Vector2d> option = new Pair(p, currentPos);
        retValue.add(option);
        pdf.add(p);
        positions.add(currentPos);

        if (p == 1.0) return retValue;

        // then the possibility of each turn

        for (int i = 0; i < 8; i++) {
            if (counts[i] > 0) {
                double theta = i * Math.PI / 4.0 + heading.theta();
                Vector2d newHeading = new Vector2d(Math.cos(theta), Math.sin(theta));
                p = counts[i] / totalCount;
                Vector2d newPosition = newHeading.mul(speed[i]).add(currentPos);
                if (!(blockOf(newPosition, gst.getBlockSize()) == blockOf(currentPos, gst.getBlockSize()))) {
                    // modify p to take account of passability changes
                    List<Pair<Integer, Vector2d>> collisions = SSOModifier.newCollisionsOf(objID, gst.getCategory(objID), gst.getCurrentSSO(), newPosition);
                    for (Pair<Integer, Vector2d> c : collisions) {
                        int type = gst.getType(c.getValue0());
                        if (passable.containsKey(type)) {
                            p *= passable.get(type) / (passable.get(type) + 10.0);
                        } else {
                            p = 0.0;
                        }
                    }
                }
                pdf.add(p);
                positions.add(newPosition);
            }
        }
        retValue.clear();
        double[] newPdf = HopshackleUtilities.normalise(pdf);
        for (int i = 0; i < positions.size(); i++) {
            retValue.add(new Pair(newPdf[i], positions.get(i)));
        }
        return retValue;
    }

    private int blockOf(Vector2d pos, int blockSize) {
        int x = (int) pos.x / blockSize;
        int y = (int) pos.y / blockSize;
        return x + 307 * y;
    }

    @Override
    public void updateModelStatistics(GameStatusTrackerWithHistory gst) {
        // we need to run through every sprite of the relevant type, and determine how it moved

        List<Integer> allSprites = gst.getAllSpritesOfType(spriteType);
        for (int id : allSprites) {
            List<Pair<Integer, Vector2d>> velocities = gst.getVelocityTrajectory(id);
            Vector2d lastV = stationary;
            GameStatusTracker lastPos = null;
            int tick, lastTick = 0;
            for (int i = 0; i < velocities.size(); i++) {
                tick = velocities.get(i).getValue0();
                Vector2d v = velocities.get(i).getValue1();
                GameStatusTracker newPos = gst.getGST(tick);

                if (i > 0 && newPos.getCurrentPosition(id) != null) {
                    lastPos = gst.getGST(lastTick);
                    if (!(blockOf(lastPos.getCurrentPosition(id), gst.getBlockSize()) == blockOf(newPos.getCurrentPosition(id), gst.getBlockSize()))) {
                        // new collisions of the sprite if it moved (based on the sso *before* move to allow for annihilations
                        List<Pair<Integer, Vector2d>> newCollisions = SSOModifier.newCollisionsOf(id, gst.getCategory(id), lastPos.getCurrentSSO(), newPos.getCurrentPosition(id));

                        Set<Integer> typesOfCollision = new HashSet();
                        for (Pair<Integer, Vector2d> collision : newCollisions) {
                            typesOfCollision.add(lastPos.getType(collision.getValue0()));
                        }
                        // we now have a list of the types we would have collided with
                        // when the most recent move was chosen
                        for (int type : typesOfCollision) {
                            if (passable.containsKey(type)) {
                                passable.put(type, passable.get(type) + 1.0);
                            } else {
                                passable.put(type, 1.0);
                            }
                        }
                    }
                }

                if (v.equals(stationary)) {
                    staticCount++;
                } else if (lastV.equals(stationary)) {
                    // which indicates we are now moving from a cold start, which we treat as 'geradeaus'
                    counts[0]++;
                } else {
                    double oldHeading = HopshackleUtilities.directionOf(lastV).getValue0();
                    double newHeading = HopshackleUtilities.directionOf(v).getValue0();
                    int headingChange = (int) ((newHeading - oldHeading + 2.0 * Math.PI) / (Math.PI / 4.0) + 0.5) % 8;
                    counts[headingChange]++;
                    double lastSpeed = v.mag();
                    speed[headingChange] = (speed[headingChange] * (counts[headingChange] - 1.0) + lastSpeed) / counts[headingChange];
                }
                if (!v.equals(stationary)) // we update last known direction only if we moved
                    lastV = v;
                lastTick = tick;
                totalCount++;
            }
        }
    }

    @Override
    public boolean isValidFor(GameStatusTracker gst, int id) {
        return gst.getType(id) == spriteType;
    }

    @Override
    public String toString() {
        StringBuilder retValue = new StringBuilder(String.format("Total: %.0f, Static: %.0f, Speed: %s, Direction shifts: %s ", totalCount, staticCount,
                HopshackleUtilities.formatArray(speed, "|", "%.1f"),
                HopshackleUtilities.formatArray(counts, "|", "%.0f"))
        );
        for (int k : passable.keySet()) {
            retValue.append(String.format("P%d=%.2f ", k, (passable.get(k) / (passable.get(k) + 10.0))));
        }
        return retValue.toString();
    }
}
