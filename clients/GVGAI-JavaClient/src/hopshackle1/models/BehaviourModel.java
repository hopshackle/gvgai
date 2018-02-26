package hopshackle1.models;

import serialization.*;
import serialization.Types.*;
import org.javatuples.*;
import java.util.*;

public interface BehaviourModel {

    /*
    Analyse all the data in the provided GameStatusTracker
     */
    public void updateModelStatistics(GameStatusTrackerWithHistory gst);

    /*
    Returns the new position of the specified sprite. This uses the MAP estimate, i.e. the mode of the distribution
     */
    public Vector2d nextMoveMAP(GameStatusTracker gst, int objID, ACTIONS move);

    /*
    Returns the new position of the specified sprite. This samples a single possibility from the distribution.
     */
    public Vector2d nextMoveRandom(GameStatusTracker gst, int objID, ACTIONS move);

    /* Returns a psdf (well, pmf) over all possible moves the model thinks exist

     */
    public List<Pair<Double, Vector2d>> nextMovePdf(GameStatusTracker gst, int objID, ACTIONS move);

    public boolean isValidFor(GameStatusTracker gst, int objID);
}
