package hopshackle1.models;

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
    private Vector2d currentPosition;   // current Avatar position

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
    public void associateWithSprite(int type) {
        throw new AssertionError("Only applicable for Avatar");
    }

    @Override
    public void reset(SerializableStateObservation sso) {
        double[] pos = sso.getAvatarPosition();
        currentPosition = new Vector2d(pos[0], pos[1]);
    }

    @Override
    public void updateModelStatistics(SerializableStateObservation sso) {
        double[] pos = sso.getAvatarPosition();
        ACTIONS lastMove = sso.avatarLastAction;
        int[] count = counts.get(lastMove);
        if (currentPosition != null) {
            double deltaXSquares = (pos[0] - currentPosition.x) / blockSize;
            double deltaYSquares = (pos[1] - currentPosition.y) / blockSize;
            int index = 8;
            if (deltaXSquares > 0.5) {
                if (deltaYSquares > 0.5) {
                    index = 3;
                } else if (deltaYSquares < -0.5) {
                    index = 1;
                } else {
                    index = 2;
                }
            } else if (deltaXSquares < -0.5) {
                if (deltaYSquares > 0.5) {
                    index = 5;
                } else if (deltaYSquares < -0.5) {
                    index = 7;
                } else {
                    index = 6;
                }
            } else {
                if (deltaYSquares > 0.5) {
                    index = 4;
                } else if (deltaYSquares < -0.5) {
                    index = 0;
                }
            }
            count[index]++;
        }
        currentPosition = new Vector2d(pos[0], pos[1]);
    }

    @Override
    public void apply(SerializableStateObservation sso, ACTIONS avatarMove) {
        Vector2d newPosition = nextMoveRandom(0, avatarMove);
        sso.avatarPosition[0] = newPosition.x;
        sso.avatarPosition[1] = newPosition.y;
    }

    @Override
    public Vector2d nextMoveMAP(int objID, ACTIONS avatarMove) {
        Vector2d retValue = null;
        double maximum = 0.0;
        for (Pair<Double, Vector2d> option : nextMovePdf(objID, avatarMove)) {
            if (option.getValue0() > maximum) {
                maximum = option.getValue0();
                retValue = option.getValue1();
            }
        }
        return retValue;
    }

    @Override
    public Vector2d nextMoveRandom(int objID, ACTIONS avatarMove) {
        double roll = rnd.nextDouble();
        for (Pair<Double, Vector2d> option : nextMovePdf(objID, avatarMove)) {
            roll -= option.getValue0();
            if (roll <= 0.0) {
                return option.getValue1();
            }
        }
        throw new AssertionError("Should not reach this point");
    }

    @Override
    public List<Pair<Double, Vector2d>> nextMovePdf(int objID, ACTIONS avatarMove) {
        int[] count = counts.get(avatarMove);
        List<Pair<Double, Vector2d>> retValue = new ArrayList();

        double totalCount = 0.0;
        for (int i = 0; i < count.length; i++) {
            totalCount += count[i];
        }
        // then the possibility of each turn
        for (int i = 0; i < 9; i++) {
            if (count[i] > 0) {
                double p = (double) count[i] / totalCount;
                double new_x = currentPosition.x + blockSize * xChange[i];
                double new_y = currentPosition.y + blockSize * yChange[i];
                Pair<Double, Vector2d> option = new Pair(p, new Vector2d(new_x, new_y));
                retValue.add(option);
            }
        }

        return retValue;
    }
}
