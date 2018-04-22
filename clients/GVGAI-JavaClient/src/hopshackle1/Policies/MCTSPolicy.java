package hopshackle1.Policies;

import hopshackle1.RL.*;
import hopshackle1.models.*;
import hopshackle1.*;
import serialization.Types;

import java.util.*;

import org.javatuples.*;

public class MCTSPolicy implements Policy {

    protected BehaviourModel model;
    protected ActionValueFunctionApproximator evalFunction;
    private Map<Pair<Integer, Types.ACTIONS>, Triplet<Integer, Integer, Double>> tree = new HashMap();
    private Map<Integer, GameStatusTracker> gameStates = new HashMap();
    private List<Types.ACTIONS> possibleActions;
    private int stateIDCounter = 0;
    private double C, temperature, defaultCoeff;
    public boolean debug = false;
    public boolean robust = false;  // true will use visits to decide final action; false will use value
    public EntityLog logFile = hopshackle1.Agent.logFile;
    protected Random rnd = new Random(45);

    public MCTSPolicy(BehaviourModel worldModel, ActionValueFunctionApproximator leafValuer, double C, double defaultCoeff, double temperature) {
        model = worldModel;
        evalFunction = leafValuer;
        this.C = C;
        this.temperature = temperature;
        this.defaultCoeff = defaultCoeff;
    }

    @Override
    public Types.ACTIONS chooseAction(List<Types.ACTIONS> actions, GameStatusTracker gst, int timeBudget) {
        long startTime = System.currentTimeMillis();
        possibleActions = actions;
        if (!possibleActions.contains(Types.ACTIONS.ACTION_NIL))
            possibleActions.add(Types.ACTIONS.ACTION_NIL);
        stateIDCounter = 0;
        tree = new HashMap();
        gameStates = new HashMap();
        int maxDepth = 0;
        ActionValueFunctionApproximator baseFunction = evalFunction;
        if (evalFunction instanceof LookaheadLinearActionValue) {
            evalFunction = evalFunction.copy();
            ((LookaheadLinearActionValue) evalFunction).setDefaultFeatureCoefficient(defaultCoeff);
        }
        do {
            int currentState = 0, lastState = 0;
            Types.ACTIONS lastAction = gst.getCurrentSSO().avatarLastAction;
            gameStates.put(currentState, new GameStatusTracker(gst));
            LinkedList<Pair<Integer, Types.ACTIONS>> trajectory = new LinkedList();
            boolean atLeaf = false;
            do {
                Types.ACTIONS nextAction = nextTreeActionFrom(currentState, lastAction);
                if (nextAction == null) {
                    // now outside tree
                    atLeaf = true;
                    nextAction = expandNode(currentState, lastAction);
                }
                Pair<Integer, Types.ACTIONS> stateAction = new Pair(currentState, nextAction);
                if (debug) logFile.log(stateAction.toString());
                trajectory.add(stateAction);
                lastState = currentState;
                currentState = tree.get(stateAction).getValue0();
                lastAction = nextAction;
            } while (!atLeaf); // terminate loop once we have a leaf

            double value = tree.get(new Pair(lastState, lastAction)).getValue2();
            if (debug) logFile.log(String.format("final score is %.2f", value));
            backPropagate(value, trajectory);
            if (trajectory.size() > maxDepth) maxDepth = trajectory.size();

        } while (System.currentTimeMillis() - startTime < timeBudget);

        logFile.log(String.format("%d total nodes expanded with maxDepth of %d", stateIDCounter + 1, maxDepth));
        logFile.flush();

        evalFunction = baseFunction;

        Types.ACTIONS retValue = (temperature < 0.001) ? bestActionFrom(0, actions) : chooseFinalAction(0, actions);
        return retValue;
    }

    protected Types.ACTIONS expandNode(int state, Types.ACTIONS lastAction) {
        // should be here because <state> does not have all it's actions instantiated
        // so, we pick one that is available at random, and use this one
        List<Types.ACTIONS> permittedActions = getPossibleActionsAfter(lastAction);
        int count = 0;
        boolean finished = false;
        Types.ACTIONS action = Types.ACTIONS.ACTION_NIL;
        do {
            finished = true;
            int roll = rnd.nextInt(permittedActions.size());
            action = permittedActions.get(roll);
            if (tree.containsKey(new Pair(state, action))) {
                // already tried
                permittedActions.remove(roll);
                count++;
                finished = false;
            }
            if (count > 7) {
                throw new AssertionError("Infinite Loop somehwhere");
            }
        } while (!finished);

        // now we can roll forward and value this
        GameStatusTracker newGST = new GameStatusTracker(gameStates.get(state));
        newGST.rollForward(model, action);
        gameStates.put(nextStateCounter(), newGST);
        double value = evalFunction.value(newGST.getCurrentSSO());
        tree.put(new Pair(state, action), new Triplet(stateIDCounter, 1, value));
            // initialise with one count of a visit using current evalFunction

        return action;
    }


    private Types.ACTIONS nextTreeActionFrom(int state, Types.ACTIONS lastAction) {
        Types.ACTIONS retValue = Types.ACTIONS.ACTION_NIL;
        int totalVisits = 0;
        double maxScore = Double.NEGATIVE_INFINITY;
        List<Types.ACTIONS> permittedActions = getPossibleActionsAfter(lastAction);

        for (Types.ACTIONS a : permittedActions) {
            Triplet<Integer, Integer, Double> node = tree.get(new Pair(state, a));
            if (node == null) {
                // we have an unselected option
                // we stop here, and go and expand the node later in the algorithm. We have completed the tree policy
                return null;
            } else {
                totalVisits += node.getValue1();
            }
        } // calculate total visits first

        for (Types.ACTIONS a : permittedActions) {
            Triplet<Integer, Integer, Double> node = tree.get(new Pair(state, a));
            double nodeScore = node.getValue2() + C * Math.sqrt(Math.log(totalVisits) / node.getValue1());
            if (nodeScore > maxScore) {
                maxScore = nodeScore;
                retValue = a;
            }
        }

        return retValue;
    }

    protected Types.ACTIONS chooseFinalAction(int state, List<Types.ACTIONS> actions) {
        double[] pdf = new double[actions.size()];
        for (int i = 0; i < actions.size(); i++) {
            Types.ACTIONS a = actions.get(i);
            Triplet<Integer, Integer, Double> data = tree.getOrDefault(new Pair(state, a), new Triplet(0, 0, 0.0));
            int visits = data.getValue1();
            double value = data.getValue2();
            if (debug) logFile.log(String.format("%s has %d visits with score of %.2f", a, visits, value));
            if (robust) {
                pdf[i] = visits;
            } else {
                pdf[i] = value;
            }
            if (visits == 0) // if not visited, then default value of 0.00 is incorrect
                pdf[i] = Double.NEGATIVE_INFINITY;
            pdf[i] /= temperature;
        }
        pdf = HopshackleUtilities.expNormalise(pdf);
        double roll = rnd.nextDouble();
        if (debug) {
            logFile.log("PDF for actions is:");
            for (int i = 0; i < pdf.length; i++) {
                logFile.log(String.format("\t%.3f\t%s", pdf[i], actions.get(i)));
            }
        }
        double cdf = 0.0;
        for (int i = 0; i < pdf.length; i++) {
            cdf += pdf[i];
            if (roll <= cdf) {
                if (debug) logFile.log("Chooses action " + actions.get(i));
                return actions.get(i);
            }
        }
        throw new AssertionError("Should not be reachable");
    }

    protected Types.ACTIONS bestActionFrom(int state, List<Types.ACTIONS> actions) {
        Types.ACTIONS retValue = Types.ACTIONS.ACTION_NIL;
        int maxVisits = 0;
        double maxScore = Double.NEGATIVE_INFINITY;
        for (Types.ACTIONS a : actions) {
            Triplet<Integer, Integer, Double> data = tree.getOrDefault(new Pair(state, a), new Triplet(0, 0, 0.0));
            int visits = data.getValue1();
            if (visits == 0)
                continue;
            if (debug) logFile.log(String.format("%s has %d visits with score of %.2f", a, visits, data.getValue2()));
            if (robust && visits > maxVisits) {
                retValue = a;
                maxVisits = visits;
                maxScore = data.getValue2();
            } else if (!robust || visits == maxVisits) {
                if (data.getValue2() > maxScore) {
                    retValue = a;
                    maxVisits = visits;
                    maxScore = data.getValue2();
                }
            }
        }
        return retValue;
    }

    protected void backPropagate(double finalValue, LinkedList<Pair<Integer, Types.ACTIONS>> trajectory) {
        Iterator<Pair<Integer, Types.ACTIONS>> backwards = trajectory.descendingIterator();
        while (backwards.hasNext()) {
            Pair<Integer, Types.ACTIONS> node = backwards.next();
            Triplet<Integer, Integer, Double> data = tree.get(node);
 //           if (debug) logFile.log(String.format("Back-propagating %.2f from %s to %s", finalValue, node, data));
            int currentVisits = data.getValue1();
            double newValue = (data.getValue2() * currentVisits + finalValue) / (currentVisits + 1);
            Triplet<Integer, Integer, Double> newData = new Triplet(data.getValue0(), currentVisits + 1, newValue);
            tree.put(node, newData);
        }
    }

    @Override
    public double[] pdfOver(List<Types.ACTIONS> actions, GameStatusTracker gst) {
        return new double[0];
    }

    public List<Types.ACTIONS> getPossibleActions() {
        return possibleActions;
    }
    public List<Types.ACTIONS> getPossibleActionsAfter(Types.ACTIONS lastAction) {
        Types.ACTIONS prohibited = Types.ACTIONS.ACTION_NIL;
        switch (lastAction) {
            case ACTION_LEFT:
                prohibited = Types.ACTIONS.ACTION_RIGHT;
                break;
            case ACTION_RIGHT:
                prohibited = Types.ACTIONS.ACTION_LEFT;
                break;
            case ACTION_DOWN:
                prohibited = Types.ACTIONS.ACTION_UP;
                break;
            case ACTION_UP:
                prohibited = Types.ACTIONS.ACTION_DOWN;
                break;
            default:
                prohibited = lastAction;
        }
        List<Types.ACTIONS> retValue = new ArrayList();
        for (Types.ACTIONS a: possibleActions) {
            if (a != prohibited)
                retValue.add(a);
        }
        return retValue;
    }

    public boolean treeContains(Pair<Integer, Types.ACTIONS> key) {
        return tree.containsKey(key);
    }
    public void updateTree(Pair<Integer, Types.ACTIONS> key, Triplet<Integer, Integer, Double> node) {
        tree.put(key, node);
    }
    public Triplet<Integer, Integer, Double> treeEntryFor(Pair<Integer, Types.ACTIONS> key) {
        return tree.get(key);
    }
    public GameStatusTracker gameState(int state) {
        return gameStates.get(state);
    }
    public void updateGameState(int key, GameStatusTracker gst) {
        gameStates.put(key, gst);
    }
    public int nextStateCounter() {
        stateIDCounter++;
        return stateIDCounter;
    }
}
