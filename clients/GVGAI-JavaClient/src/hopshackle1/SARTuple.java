package hopshackle1;

import serialization.Types.*;

public class SARTuple {

    public State state;
    public ACTIONS action;
    public double reward;

    public SARTuple(State state, ACTIONS action, double reward) {
        this.state = state;
        this.action = action;
        this.reward = reward;
    }
}
