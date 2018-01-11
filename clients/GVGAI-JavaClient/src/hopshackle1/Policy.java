package hopshackle1;

import serialization.Types;

import java.util.*;

public interface Policy {

    public void learnFrom(List<SARTuple> trajectories);

    public void learnFrom(SARTuple tuple);

    public Types.ACTIONS chooseAction(List<Types.ACTIONS> actions, State state, boolean actionDebug);
}
