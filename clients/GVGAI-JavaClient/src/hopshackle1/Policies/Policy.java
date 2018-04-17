package hopshackle1.Policies;

import hopshackle1.models.GameStatusTracker;
import serialization.Types;
import java.util.*;

public interface Policy {

    public Types.ACTIONS chooseAction(List<Types.ACTIONS> actions, GameStatusTracker gst, int timeBudget);

    public double[] pdfOver(List<Types.ACTIONS> actions, GameStatusTracker gst);
}
