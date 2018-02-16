package hopshackle1.models;

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
    private Map<Integer, Integer> lastDirection = new HashMap(); // last known move direction for each tracked sprite
    private Map<Integer, Vector2d> currentPositions = new HashMap(); // last known position for each tracked sprite

    public SimpleSpriteModel(int block) {
        staticCount = 5;
        totalCount = 5;
        blockSize = block;
    }

    public SimpleSpriteModel(int block, int[] countData, int stationaryCount) {
        blockSize = block;
        if (countData.length != 8)
            throw new AssertionError("CountData must have length of 8 in SimpleSpriteModel");
        for (int i = 0; i < 8; i++) {
            counts[i] = countData[i];
            totalCount += countData[i];
        }
        staticCount = stationaryCount;
    }

    public void setRandom(Random newR) {
        rnd = newR;
    }

    @Override
    public void associateWithSprite(int type) {
        if (spriteType != -1)
            throw new AssertionError("Not yet implemented");
        spriteType = type;
    }

    @Override
    public void reset(SerializableStateObservation sso) {
        lastDirection = new HashMap();
        currentPositions = new HashMap();
        updateModelStatistics(sso);
    }

    @Override
    public void apply(SerializableStateObservation sso, Types.ACTIONS avatarMove) {
        // we need to update the position of each sprite type that we model

        // statistics are maintained on the basis that we see a sequence of frames from the same game
        // when we get an sso here, it may not be the same as from that game...so do we want to use currentPosition?

        // we may just have to assume that the currentPos and directions are correct for *our* spriteType (although others may have been changed)
        // we could then roll forward beyond one step, but we'd have to feed in the updated sso to all the BehaviouralModels for them to
        // update their statistics and histories.
        // A central problem is that the SSO does not include a record of the last direction moved, which we need

        updateObservations(sso.getNPCPositions(), avatarMove);
        updateObservations(sso.getMovablePositions(), avatarMove);
        updateObservations(sso.getFromAvatarSpritesPositions(), avatarMove);
    }

    private void updateObservations(Observation[][] observations, Types.ACTIONS move) {
        for (Observation[] npc : observations) {
            for (Observation obs : npc) {
                if (obs.itype == spriteType && lastDirection.containsKey(obs.obsID)) {
                    // one of ours
                    Vector2d newPos = nextMoveRandom(obs.obsID, move);
                    obs.position = newPos;
                }
            }
        }
    }


    @Override
    public Vector2d nextMoveMAP(int objID, Types.ACTIONS move) {
        Vector2d retValue = null;
        double maximum = 0.0;
        for (Pair<Double, Vector2d> option : nextMovePdf(objID, move)) {
            if (option.getValue0() > maximum) {
                maximum = option.getValue0();
                retValue = option.getValue1();
            }
        }
        return retValue;
    }

    @Override
    public Vector2d nextMoveRandom(int objID, Types.ACTIONS move) {
        double roll = rnd.nextDouble();
        for (Pair<Double, Vector2d> option : nextMovePdf(objID, move)) {
            roll -= option.getValue0();
            if (roll <= 0.0) {
                return option.getValue1();
            }
        }
        throw new AssertionError("Should not reach this point");
    }

    @Override
    public List<Pair<Double, Vector2d>> nextMovePdf(int objID, Types.ACTIONS move) {
        int heading = lastDirection.getOrDefault(objID, 0);
        List<Pair<Double, Vector2d>> retValue = new ArrayList();
        // first the possibility of not moving
        double p = (double) staticCount / totalCount;
        Vector2d currentPos = getCurrentPosition(objID);
        Pair<Double, Vector2d> option = new Pair(p, currentPos);
        retValue.add(option);

        // then the possibility of each turn
        for (int i = 0; i < 8; i++) {
            if (counts[i] > 0) {
                p = (double) counts[i] / totalCount;
                int newAbsoluteHeading = (i + heading) % 8;
                double new_x = currentPos.x + blockSize * xChange[newAbsoluteHeading];
                double new_y = currentPos.y + blockSize * yChange[newAbsoluteHeading];
                option = new Pair(p, new Vector2d(new_x, new_y));
                retValue.add(option);
            }
        }

        return retValue;
    }

    public Vector2d getCurrentPosition(int objID) {
        if (currentPositions.containsKey(objID))
            return currentPositions.get(objID);
        throw new AssertionError("Sprite " + objID + " not found in current state");
    }

    public int getDirection(int objID) {
        return lastDirection.getOrDefault(objID, 0);
    }

    @Override
    public void updateModelStatistics(SerializableStateObservation sso) {
        // we need to run through every sprite of the relevant type, and determine where it
        updateStatisticsFromObservations(sso.getNPCPositions());
        updateStatisticsFromObservations(sso.getMovablePositions());
        updateStatisticsFromObservations(sso.getFromAvatarSpritesPositions());
    }

    private void updateStatisticsFromObservations(Observation[][] data) {
        for (Observation[] npcPosition : data) {
            for (Observation obs : npcPosition) {
                updateStatisticsFrom(obs);
            }
        }
    }

    public void updateStatisticsFrom(Observation obs) {
        if (obs.itype == spriteType) {
            boolean moved = true;
            Vector2d lastPos = currentPositions.get(obs.obsID);
            currentPositions.put(obs.obsID, obs.position);
            if (lastPos != null) {
                int lastD = lastDirection.getOrDefault(obs.obsID, 0);
                int newD = lastD;
                // 0 as a default means not moving
                //  7 8 1
                //  6 - 2
                //  5 4 3       direction change is one of these numbers - half-right hand turns from straight up
                double deltaXSquares = (obs.position.x - lastPos.x) / blockSize;
                double deltaYSquares = (obs.position.y - lastPos.y) / blockSize;
                if (deltaXSquares > 0.5) {
                    if (deltaYSquares > 0.5) {
                        newD = 3;
                    } else if (deltaYSquares < -0.5) {
                        newD = 1;
                    } else {
                        newD = 2;
                    }
                } else if (deltaXSquares < -0.5) {
                    if (deltaYSquares > 0.5) {
                        newD = 5;
                    } else if (deltaYSquares < -0.5) {
                        newD = 7;
                    } else {
                        newD = 6;
                    }
                } else {
                    if (deltaYSquares > 0.5) {
                        newD = 4;
                    } else if (deltaYSquares < -0.5) {
                        newD = 8;
                    } else {
                        newD = lastD; // no movement
                        staticCount++;
                        moved = false;
                    }
                }
                if (moved) {
                    int directionChange = (8 + newD - lastD) % 8;
                    if (lastD == 0) {
                        // we have not yet seen a move
                        directionChange = 0;
                    }
                    counts[directionChange]++;
                    lastDirection.put(obs.obsID, newD);
                }
                totalCount++;

            }
        }
    }
}
