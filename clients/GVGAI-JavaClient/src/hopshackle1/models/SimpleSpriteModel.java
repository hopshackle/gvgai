package hopshackle1.models;

import hopshackle1.HopshackleUtilities;
import serialization.*;
import org.javatuples.*;

import java.util.*;

public class SimpleSpriteModel implements BehaviourModel {

    private int spriteType = -1;
    private Random rnd = new Random(46);
    private int[] counts = new int[8]; // the count of the turns made each move
    private int totalCount, staticCount, blockSize;
    //  7 0 1
    //  6 - 2
    //  5 4 3       direction change is one of these numbers - half-right hand turns from straight up
    // xChange and yChange are the arrays of x, y co-ord changes for each of the 8 movement directions
    // We encode a turn as the number of half right turns compared to last known direction
    private int[] xChange = {0, 1, 1, 1, 0, -1, -1, -1};
    private int[] yChange = {-1, -1, 0, 1, 1, 1, 0, -1};
    public static final Vector2d stationary = new Vector2d(0, 0);


    public SimpleSpriteModel(int block, int spriteType) {
        staticCount = 5;
        totalCount = 5;
        this.spriteType = spriteType;
        blockSize = block;
    }

    public SimpleSpriteModel(int block, int[] countData, int stationaryCount, int type) {
        blockSize = block;
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
        Vector2d heading = gst.getCurrentVelocity(objID);
        List<Pair<Double, Vector2d>> retValue = new ArrayList();
        // first the possibility of not moving
        double p = (double) staticCount / totalCount;
        Vector2d currentPos = gst.getCurrentPosition(objID);
        Pair<Double, Vector2d> option = new Pair(p, currentPos);
        retValue.add(option);

        // then the possibility of each turn
        for (int i = 0; i < 8; i++) {
            if (counts[i] > 0) {
                double theta = i * Math.PI / 4.0 + heading.theta();
                Vector2d newHeading = new Vector2d(Math.cos(theta), Math.sin(theta));
                p = (double) counts[i] / totalCount;
                option = new Pair(p, newHeading.mul(blockSize).add(currentPos));
                retValue.add(option);
            }
        }

        return retValue;
    }

    @Override
    public void updateModelStatistics(GameStatusTrackerWithHistory gst) {
        // we need to run through every sprite of the relevant type, and determine how it moved

        List<Integer> allSprites = gst.getAllSpritesOfType(spriteType);
        for (int id : allSprites) {
            List<Pair<Integer, Vector2d>> velocities = gst.getVelocityTrajectory(id);
            Vector2d lastV = stationary;
            for (Pair<Integer, Vector2d> point : velocities) {
                Vector2d v = point.getValue1();
                if (v.equals(stationary)) {
                    staticCount++;
                } else if (lastV.equals(stationary)) {
                    // which indicates we are now moving from a cold start, which we treat as 'geradeaus'
                    counts[0]++;
                } else {
                    Vector2d delV = new Vector2d(v).subtract(lastV);
                    int heading = HopshackleUtilities.directionOf(delV).getValue1();
                    counts[heading]++;
                }
                if (!v.equals(stationary)) // we update last known direction only if we moved
                    lastV = v;
                totalCount++;
            }
        }
    }

    @Override
    public boolean isValidFor(GameStatusTracker gst, int id) {
        return gst.getType(id) == spriteType;
    }

}
