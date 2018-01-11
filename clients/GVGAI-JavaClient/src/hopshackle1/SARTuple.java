package hopshackle1;

import serialization.Types.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SARTuple {

    private static AtomicInteger counter = new AtomicInteger(0);
    public State state;
    public State nextState;
    public ACTIONS action;
    public List<ACTIONS> availableStartActions, availableEndActions;
    public double reward;
    public final int ref;

    public SARTuple(State startState, State nextState, ACTIONS actionChosen, List<ACTIONS> allActionsFromStart, List<ACTIONS> allActionsFromNext, double reward) {
        this.state = startState;
        this.nextState = nextState;
        this.action = actionChosen;
        this.reward = reward;
        this.availableStartActions = HopshackleUtilities.cloneList(allActionsFromStart);
        this.availableEndActions = HopshackleUtilities.cloneList(allActionsFromNext);
        ref = counter.incrementAndGet();
    }
}
