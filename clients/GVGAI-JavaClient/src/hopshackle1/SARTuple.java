package hopshackle1;

import serialization.Types.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SARTuple {

    private static AtomicInteger counter = new AtomicInteger(0);
    public State state;
    public State nextState;
    public State nNextState;
    public ACTIONS action;
    public List<ACTIONS> availableStartActions, availableEndActions, availableNStepActions;
    public double reward;
    public double nStepReward;
    public int nStep;
    public double mcReward;
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

    public boolean isProcessed() {
        return (nStepReward != 0.0);
    }

    /*
    This will update the provided data in situ to populate SARTuple.mcReward and SARTuple.nStepReward
     */
    public static void chainRewardsBackwards(LinkedList<SARTuple> data, int nStepReward, double gamma) {
        Iterator<SARTuple> backwardsChain = data.descendingIterator();
        if (nStepReward < 1) nStepReward = 1;

        double mcReward = 0.0;
        double[] nSteps = new double[nStepReward];
        State[] nStates = new State[nStepReward];
        List<ACTIONS>[] nActions = new List[nStepReward];
        int currentIndex = 0;
        double runningRewardSum = 0.0;
        double gammaN = Math.pow(gamma, nStepReward - 1);

        while (backwardsChain.hasNext()) {
            SARTuple previous = backwardsChain.next();
            previous.nStep = nStepReward;

            // Monte Carlo reward first
            mcReward = mcReward * gamma + previous.reward;
            previous.mcReward = mcReward;

            // set reward
            int nextIndex = (currentIndex + 1) % nStepReward;
            runningRewardSum = gamma * runningRewardSum + previous.reward;

            nSteps[currentIndex] = previous.reward;
            nStates[currentIndex] = previous.nextState;
            nActions[currentIndex] = previous.availableEndActions;

            previous.nStepReward = runningRewardSum;
            previous.nNextState = nStates[nextIndex];
            previous.availableNStepActions = nActions[nextIndex] != null ? nActions[nextIndex] : new ArrayList();

            // subtract the amount that falls off the end (has been discounted n-1 times since its addition
            runningRewardSum = runningRewardSum - gammaN * nSteps[nextIndex];
            currentIndex = nextIndex;
        }

    }
}
