package hopshackle1.test;

import hopshackle1.HopshackleUtilities;
import hopshackle1.*;

import java.util.*;

import hopshackle1.RL.*;
import hopshackle1.models.SSOModifier;
import org.junit.*;
import serialization.SerializableStateObservation;

import static org.junit.Assert.*;

public class SARTupleTest {

    private double[] rewardData = {0.0, 1.0, 1.0, 0.0, 3.5, 0.0, 0.0, -100.0};
    private double[] mcPredictions = {-43.82334, (1.0 - 49.6926), (1.0 - 56.214), -62.46, (3.5 - 72.9), -81.0, -90.0, -100.0};
    private double[] twoStepPredictions = {0.9, 1.9, 1.0, 3.15, 3.5, 0.0, -90.0, -100.0};
    private double[] fourStepPredictions = {1.71, 4.4515, 3.835, 3.15, (3.5 - 72.9), -81.0, -90.0, -100.0};
    private SerializableStateObservation[] allStates = new SerializableStateObservation[9];
    private LinkedList<SARTuple> testData;

    @Before
    public void setup() {
        testData = new LinkedList();
        for (int i = 0; i < 8; i++) {
            allStates[i] = SSOModifier.constructEmptySSO();
        }
        allStates[8] = null;
        for (int i = 0; i < rewardData.length; i++) {
            SARTuple tuple = new SARTuple(allStates[i], allStates[i + 1], null, new ArrayList(), new ArrayList(), rewardData[i]);
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
}
