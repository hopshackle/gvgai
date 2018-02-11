package hopshackle1.test;

import hopshackle1.HopshackleUtilities;
import hopshackle1.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class SARTupleTest {

    private double[] rewardData = {0.0, 1.0, 1.0, 0.0, 3.5, 0.0, 0.0, -100.0};
    private double[] mcPredictions = {-43.82334, (1.0 - 49.6926), (1.0 - 56.214), -62.46, (3.5 - 72.9), -81.0, -90.0, -100.0};
    private double[] twoStepPredictions = {0.9, 1.9, 1.0, 3.15, 3.5, 0.0, -90.0, -100.0};
    private double[] fourStepPredictions = {1.71, 4.4515, 3.835, 3.15, (3.5 - 72.9), -81.0, -90.0, -100.0};
    private State[] allStates = new State[9];
    private LinkedList<SARTuple> testData;

    @Before
    public void setup() {
        testData = new LinkedList();
        for (int i = 0; i < 8; i++) {
            allStates[i] = new State(null);
        }
        allStates[8] = null;
        for (int i = 0; i < rewardData.length; i++) {
            SARTuple tuple = new SARTuple(allStates[i], allStates[i+1], null, new ArrayList(), new ArrayList(), rewardData[i]);
            testData.add(tuple);
        }
    }

    @Test
    public void monteCarloRewardChainingTest() {
        SARTuple.chainRewardsBackwards(testData, 1, 0.9);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).reward, rewardData[i], 0.0001);
            assertEquals(testData.get(i).mcReward, mcPredictions[i], 0.0001);
        }
    }

    @Test
    public void oneStepRewardChainingTest() {
        SARTuple.chainRewardsBackwards(testData, 1, 0.9);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).nStepReward, rewardData[i], 0.0001);
        }
        for (int i = 0; i < testData.size() - 1; i++) {
            assertTrue(testData.get(i).nNextState == testData.get(i).nextState);
            assertTrue(testData.get(i).nNextState == testData.get(i+1).state);
        }
    }

    @Test
    public void twoStepRewardChainingTest() {
        SARTuple.chainRewardsBackwards(testData, 2, 0.9);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).nStepReward, twoStepPredictions[i], 0.0001);
        }
        for (int i = 0; i < testData.size() - 2; i++) {
            assertTrue(testData.get(i).nNextState == testData.get(i+2).state);
        }
    }

    @Test
    public void fourStepRewardChainingTest() {
        SARTuple.chainRewardsBackwards(testData, 4, 0.9);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).nStepReward, fourStepPredictions[i], 0.0001);
        }
        for (int i = 0; i < testData.size() - 4; i++) {
            assertTrue(testData.get(i).nNextState == testData.get(i+4).state);
        }
        for (int i = testData.size() - 4; i < testData.size(); i++) {
            assertTrue(testData.get(i).nNextState == null);
        }
    }
}
