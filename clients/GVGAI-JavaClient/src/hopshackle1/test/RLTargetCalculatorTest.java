package hopshackle1.test;

import hopshackle1.HopshackleUtilities;
import hopshackle1.*;

import java.util.*;

import hopshackle1.Policies.PolicyGuide;
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

    private double[] oneStepValPred = {1.8, 3.7, 4.6, 4.5, 5.4 + 3.5, 6.3, 7.2, -100.0};
    private double[] twoStepValPred = {2.43 + 0.9, 3.24 + 1.9, 0.81 * 5.0 + 1.0, 0.81 * 6.0 + 3.15, 0.81 * 7.0 + 3.5, 0.81 * 8.0 + 0.0, -90.0, -100.0};
    private double[] fourStepValPred = {0.6561 * 5.0 + 1.71, 0.6561 * 6.0 + 4.4515, 0.6561 * 7.0 + 3.835, 0.6561 * 8.0 + 3.15, 3.5 - 72.9, -81.0, -90.0, -100.0};
    private SerializableStateObservation[] allStates = new SerializableStateObservation[9];
    private LinkedList<SARTuple> testData;
    private GameStatusTrackerWithHistory gst;
    private ConstantActionValueFunctionApproximator constantValuer = new ConstantActionValueFunctionApproximator();

    @Before
    public void setup() {
        testData = new LinkedList();
        for (int i = 0; i < 8; i++) {
            allStates[i] = SSOModifier.constructEmptySSO();
            allStates[i].gameTick = i;
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
        RLTargetCalculator monteCarloReward = new MonteCarloReward(0.01, 0.9, 0.00, false);
        monteCarloReward.crystalliseRewards(testData);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).reward, rewardData[i], 0.0001);
            assertEquals(testData.get(i).rewardToEnd, mcPredictions[i], 0.0001);
        }
    }

    @Test
    public void oneStepRewardChainingTest() {
        ActionValueFunctionApproximator fa = new IndependentLinearActionValue(new ArrayList(), 0.9, false);
        RLTargetCalculator oneStep = new QLearning(1, 0.01, 0.9, 0.00, false, fa);
        oneStep.crystalliseRewards(testData);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).reward, rewardData[i], 0.0001);
            assertEquals(testData.get(i).rewardToEnd, rewardData[i], 0.0001);
        }
    }

    @Test
    public void twoStepRewardChainingTest() {
        ActionValueFunctionApproximator fa = new IndependentLinearActionValue(new ArrayList(), 0.9, false);
        RLTargetCalculator twoStep = new QLearning(2, 0.01, 0.9, 0.00, false, fa);
        twoStep.crystalliseRewards(testData);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).reward, rewardData[i], 0.0001);
            assertEquals(testData.get(i).rewardToEnd, twoStepPredictions[i], 0.0001);
        }
    }

    @Test
    public void fourStepRewardChainingTest() {
        ActionValueFunctionApproximator fa = new IndependentLinearActionValue(new ArrayList(), 0.9, false);
        RLTargetCalculator fourStep = new QLearning(4, 0.01, 0.9, 0.00, false, fa);
        fourStep.crystalliseRewards(testData);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).reward, rewardData[i], 0.0001);
            assertEquals(testData.get(i).rewardToEnd, fourStepPredictions[i], 0.0001);
        }
    }

    private double getQValueFrom(SARTuple data) {
        if (data.rewardGST == null || data.rewardGST.getCurrentSSO() == null) return 0.00;
        GameStatusTracker gst = new GameStatusTracker(data.rewardGST);
        double gamma = data.finalDiscount;
        gst.rollForward(new ArrayList(), data.action, true);
        SerializableStateObservation nextState = gst.getCurrentSSO();

        return gamma * constantValuer.value(nextState);
    }

    @Test
    public void oneStepRewardChainingWithFuncApproxTest() {
        RLTargetCalculator oneStep = new QLearning(1, 0.01, 0.9, 0.00, false, constantValuer);
        oneStep.crystalliseRewards(testData);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).reward, rewardData[i], 0.0001);
            double endValue = getQValueFrom(testData.get(i));
            assertEquals(testData.get(i).rewardToEnd + endValue, oneStepValPred[i], 0.0001);
        }
    }

    @Test
    public void twoStepRewardChainingWithFuncApproxTest() {
        RLTargetCalculator twoStep = new QLearning(2, 0.01, 0.9, 0.00, false, constantValuer);
        twoStep.crystalliseRewards(testData);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).reward, rewardData[i], 0.0001);
            double endValue = getQValueFrom(testData.get(i));
            assertEquals(testData.get(i).rewardToEnd + endValue, twoStepValPred[i], 0.0001);
        }
    }

    @Test
    public void fourStepRewardChainingWithFuncApproxTest() {
        RLTargetCalculator fourStep = new QLearning(4, 0.01, 0.9, 0.00, false, constantValuer);
        fourStep.crystalliseRewards(testData);
        for (int i = 0; i < testData.size(); i++) {
            assertEquals(testData.get(i).reward, rewardData[i], 0.0001);
            double endValue = getQValueFrom(testData.get(i));
            assertEquals(testData.get(i).rewardToEnd + endValue, fourStepValPred[i], 0.0001);
        }
    }
}

class ConstantActionValueFunctionApproximator implements ActionValueFunctionApproximator {

    ConstantActionValueFunctionApproximator(){
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
    public double value(SerializableStateObservation s) {
        return s.gameTick;
    }

    @Override
    public double value(GameStatusTracker gst, Types.ACTIONS a) {
        return gst.getCurrentTick() + 1.0;
    }

    @Override
    public ActionValue valueOfBestAction(GameStatusTracker gst, List<Types.ACTIONS> actions) {
        return new ActionValue(Types.ACTIONS.ACTION_LEFT, (gst == null) ? 0.0 : gst.getCurrentTick() + 1.0);
    }
    @Override
    public void injectPolicyGuide(PolicyGuide guide) {}

    @Override
    public double valueOfCoefficient(int feature) {
        return 0;
    }
}