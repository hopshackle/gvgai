package hopshackle1.models;

import serialization.*;
import serialization.Types.*;
import org.javatuples.*;
import java.util.*;

public interface BehaviourModel {

    /*
    Instruct the BehaviourModel to start modelling the specified sprite type
    Without calling this, the model will not actually do anything
    (Could have been put in a Constructor, but may be helpful later)
     */
    public void associateWithSprite(int type);

    /*
    This wipes all ongoing sprite tracking information, but retains the underlying model learned to date
    Use when a new game is started for example
     */
    public void reset(SerializableStateObservation sso);

    /*
    This assumes the provided SerializableStateObservation is the next one in sequence
     */
    public void updateModelStatistics(SerializableStateObservation sso);

    /*
    Apply the BehaviourModel to predict the next State of the whole system
    This does not construct the Observation grid ... we leave that to be called after all BehaviourModels have been applied
     */
    public void apply(SerializableStateObservation sso, ACTIONS avatarMove);

    /*
    Returns the new position of the specified sprite. This uses the MAP estimate, i.e. the mode of the distribution
     */
    public Vector2d nextMoveMAP(int objID, ACTIONS move);

    /*
    Returns the new position of the specified sprite. This samples a single possibility from the distribution.
     */
    public Vector2d nextMoveRandom(int objID, ACTIONS move);

    /* Returns a psdf (well, pmf) over all possible moves the model thinks exist

     */
    public List<Pair<Double, Vector2d>> nextMovePdf(int objID, ACTIONS move);

}
