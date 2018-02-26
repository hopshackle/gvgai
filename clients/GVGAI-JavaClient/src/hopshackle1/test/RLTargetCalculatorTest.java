package hopshackle1.test;

import hopshackle1.HopshackleUtilities;
import hopshackle1.*;

import java.util.*;

import hopshackle1.RL.*;
import hopshackle1.models.GameStatusTracker;
import hopshackle1.models.GameStatusTrackerWithHistory;
import hopshackle1.models.SSOModifier;
import org.junit.*;
import serialization.SerializableStateObservation;
import serialization.Types;

import static org.junit.Assert.*;

public class RLTargetCalculatorTest {

    private double[] rewardData = {0.0, 1.0, 1.0, 0.0, 3.5, 0.0, 0.0, -100.0};
    private double[] mcPredictions = {-43.82334, (1.0 - 49.6926), (1.0 - 56.214), -62.46, (3.5 - 72.9), -81.0, -90.0, -100.0};
    private double[] twoStepPredictions = {0.9, 1.9, 1.0, 3.15, 3.5, 0.0, -90.0, -100.0};
    private double[] fourStepPredictions = {1.71, 4.4515, 3.835, 3.15, (3.5 - 72.9), -81.0, -90.0, -100.0};

    private double[] oneStepValPred = {9.0, 10.0, 10.0, 9.0, 12.5, 9.0, 9.0, -100.0};
    private double[] twoStepValPred = {8.1 + 0.9, 8.1 + 1.9, 8.1 + 1.0, 8.1 + 3.15, 8.1 + 3.5, 8.1 + 0.0, -90.0, -100.0};
    private double[] fourStepValPred = {6.561 + 1.71, 6.561 + 4.4515, 6.561 + 3.835, 6.561 + 3.15, 3.5 - 72.9, -81.0, -90.0, -100.0};
    private SerializableStateObservation[] allStates = new SerializableStateObservation[9];
    private LinkedList<SARTuple> testData;
    private GameStatusTrackerWithHistory gst;
    private ConstantActionValueFunctionApproximator constantValuer = new ConstantActionValueFunctionApproximator(10.0);

    @Before
    public void setup() {
        testData = new LinkedList();
        for (int i = 0; i < 8; i++) {
            allStates[i] = SSOModifier.constructEmptySSO();
        }
        allStates[8] = null;

        gst = new GameStatusTrackerWithHistory();
        for (int i = 0; i < rewardData.length; i++) {
            gst.update(allStates[i]);
            SARTuple tuple = new SARTuple(gst, allStates[i + 1], null, new ArrayList(), new ArrayList(), rewardData[i]);
            testData.add(tuple);
        }
    }

    @Test
    public void monteCarloRewardChainingTest() {
        RLTargetCalculator monteCarloReward = new MonteCarloReward(0.01, 0.9, 0.00);
        RLTargetCalculator.processRewardsWith(testData, monteCarloReward);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).reward, rewardData[i], 0.0001);
            assertEquals(testData.get(i).target, mcPredictions[i], 0.0001);
        }
    }

    @Test
    public void oneStepRewardChainingTest() {
        ActionValueFunctionApproximator fa = new IndependentLinearActionValue(new ArrayList(), 0.9, false);
        RLTargetCalculator oneStep = new QLearning(1, 0.01, 0.9, 0.00, fa);
        RLTargetCalculator.processRewardsWith(testData, oneStep);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).reward, rewardData[i], 0.0001);
            assertEquals(testData.get(i).target, rewardData[i], 0.0001);
        }
    }

    @Test
    public void twoStepRewardChainingTest() {
        ActionValueFunctionApproximator fa = new IndependentLinearActionValue(new ArrayList(), 0.9, false);
        RLTargetCalculator twoStep = new QLearning(2, 0.01, 0.9, 0.00, fa);
        RLTargetCalculator.processRewardsWith(testData, twoStep);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).reward, rewardData[i], 0.0001);
            assertEquals(testData.get(i).target, twoStepPredictions[i], 0.0001);
        }
    }

    @Test
    public void fourStepRewardChainingTest() {
        ActionValueFunctionApproximator fa = new IndependentLinearActionValue(new ArrayList(), 0.9, false);
        RLTargetCalculator fourStep = new QLearning(4, 0.01, 0.9, 0.00, fa);
        RLTargetCalculator.processRewardsWith(testData, fourStep);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).reward, rewardData[i], 0.0001);
            assertEquals(testData.get(i).target, fourStepPredictions[i], 0.0001);
        }
    }

    @Test
    public void oneStepRewardChainingWithFuncApproxTest() {
        RLTargetCalculator oneStep = new QLearning(1, 0.01, 0.9, 0.00, constantValuer);
        RLTargetCalculator.processRewardsWith(testData, oneStep);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).reward, rewardData[i], 0.0001);
            assertEquals(testData.get(i).target, oneStepValPred[i], 0.0001);
        }
    }

    @Test
    public void twoStepRewardChainingWithFuncApproxTest() {
        RLTargetCalculator twoStep = new QLearning(2, 0.01, 0.9, 0.00, constantValuer);
        RLTargetCalculator.processRewardsWith(testData, twoStep);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).reward, rewardData[i], 0.0001);
            assertEquals(testData.get(i).target, twoStepValPred[i], 0.0001);
        }
    }

    @Test
    public void fourStepRewardChainingWithFuncApproxTest() {
        RLTargetCalculator fourStep = new QLearning(4, 0.01, 0.9, 0.00, constantValuer);
        RLTargetCalculator.processRewardsWith(testData, fourStep);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).reward, rewardData[i], 0.0001);
            assertEquals(testData.get(i).target, fourStepValPred[i], 0.0001);
        }
    }
}

class ConstantActionValueFunctionApproximator implements ActionValueFunctionApproximator {
    private double c;

    ConstantActionValueFunctionApproximator(double constant) {
        c = constant;
    }

    @Override
    public ActionValueFunctionApproximator copy() {
        return this;
    }

    @Override
    public State calculateState(SerializableStateObservation sso) {
        return new State();
    }

    @Override
    public double value(SerializableStateObservation s, Types.ACTIONS a) {
        return c;
    }

    @Override
    public double value(SerializableStateObservation s) {
        return c;
    }

    @Override
    public double value(GameStatusTracker gst, Types.ACTIONS a) {
        return c;
    }

    @Override
    public ActionValue valueOfBestAction(SerializableStateObservation s, List<Types.ACTIONS> actions) {
        return new ActionValue(Types.ACTIONS.ACTION_LEFT, c);
    }

    @Override
    public ActionValue valueOfBestAction(GameStatusTracker gst, List<Types.ACTIONS> actions) {
        return new ActionValue(Types.ACTIONS.ACTION_LEFT, c);
    }
}