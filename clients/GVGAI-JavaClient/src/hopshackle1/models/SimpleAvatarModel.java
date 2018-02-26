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
        for (Pair<Integer, Vector2d> point : avatarVelocities) {
            int tick = point.getValue0();
            ACTIONS lastMove = gst.getAvatarAction(tick);
            Vector2d v = point.getValue1();

            int[] count = counts.get(lastMove);
            int heading = HopshackleUtilities.directionOf(v).getValue1();
            if (v.mag() < blockSize / 2.0) {
                count[8]++;
            } else {
                count[heading]++;
            }
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
                Pair<Double, Vector2d> option = new Pair(p, new Vector2d(new_x, new_y));
                retValue.add(option);
            }
        }

        return retValue;
    }

    @Override
    public boolean isValidFor(GameStatusTracker gst, int id) {
        return (id == 0);
    }
}
