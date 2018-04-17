package hopshackle1;

import hopshackle1.models.GameStatusTracker;
import hopshackle1.models.SSOModifier;
import serialization.SerializableStateObservation;
import serialization.Types.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SARTuple {

    private static AtomicInteger counter = new AtomicInteger(0);
    public GameStatusTracker startGST, nextGST;
    public ACTIONS action, actionFromEnd;
    public List<ACTIONS> availableStartActions, availableEndActions;
    public double reward, rewardToEnd, finalDiscount;
    public SerializableStateObservation rewardState;
    public final int ref;

    public SARTuple(GameStatusTracker gst, SerializableStateObservation nextSSO, ACTIONS actionChosen, List<ACTIONS> allActionsFromStart, List<ACTIONS> allActionsFromNext, double reward) {
        this.startGST = new GameStatusTracker(gst);
        if (nextSSO != null) {
            this.nextGST = new GameStatusTracker(gst);
            nextGST.update(SSOModifier.copy(nextSSO));
        }
        this.action = actionChosen;
        this.reward = reward;
        this.availableStartActions = HopshackleUtilities.cloneList(allActionsFromStart);
        this.availableEndActions = HopshackleUtilities.cloneList(allActionsFromNext);
        ref = counter.incrementAndGet();
    }

    public void setTarget(SerializableStateObservation end, ACTIONS nextAction, double fullReward, double discount) {
        rewardState = end;
        rewardToEnd = fullReward;
        finalDiscount = discount;
        actionFromEnd = nextAction;
    }

    @Override
    public String toString() {
        return String.format("Ref: %d\t%s\tR: %.2f\tT: %.2f", ref, action, reward, rewardToEnd);
    }

}
