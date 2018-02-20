package hopshackle1.test;

import hopshackle1.*;

import java.util.*;

import hopshackle1.FeatureSets.FeatureSet;
import hopshackle1.FeatureSets.GlobalPopulationFeatureSet;
import hopshackle1.RL.*;
import hopshackle1.models.*;
import org.junit.*;
import serialization.*;
import serialization.Types.*;

import static org.junit.Assert.*;

public class ActionValueApproximatorTest {

    List<FeatureSet> features;
    IndependentLinearActionValue independent;
    LookaheadLinearActionValue lookahead;
    LinearValueFunction stateOnly;
    SerializableStateObservation sso, nextSSO;
    final SerializableStateObservation empty = SSOModifier.constructEmptySSO();
    List<ACTIONS> allActions;
    ReinforcementLearningAlgorithm dummy = new ReinforcementLearningAlgorithm() {
        @Override
        public double learningRate() {
            return 0.5;
        }

        @Override
        public double regularisation() {
            return 0.01;
        }
    };

    @Before
    public void setup() {
        allActions = Arrays.asList(ACTIONS.values());
        features = new ArrayList();
        features.add(new GlobalPopulationFeatureSet());

        independent = new IndependentLinearActionValue(features, 0.9, false);
        lookahead = new LookaheadLinearActionValue(features, 0.9, false, new LookaheadFunction() {
            @Override
            public SerializableStateObservation rollForward(SerializableStateObservation sso, ACTIONS action) {
                return sso;
            }
        });
        stateOnly = new LinearValueFunction(features, "ENTROPY", 100, 0.9, false);

        independent.value(empty, ACTIONS.ACTION_LEFT);
        independent.value(empty, ACTIONS.ACTION_ESCAPE);
        independent.value(empty, ACTIONS.ACTION_DOWN);

        sso = SSOModifier.constructEmptySSO();
        sso.NPCPositions = new Observation[1][1];
        sso.movablePositions = new Observation[1][2];
        sso.NPCPositions[0][0] = SSOModifier.createObservation(1, 2, 3, 40, 40);
        sso.movablePositions[0][0] = SSOModifier.createObservation(2, 3, 13, 50, 40);
        sso.movablePositions[0][1] = SSOModifier.createObservation(3, 3, 13, 60, 40);

        nextSSO = SSOModifier.constructEmptySSO();
        nextSSO.NPCPositions = new Observation[1][1];
        nextSSO.movablePositions = new Observation[1][1];
        nextSSO.fromAvatarSpritesPositions = new Observation[1][1];
        nextSSO.NPCPositions[0][0] = SSOModifier.createObservation(1, 2, 3, 40, 40);
        nextSSO.movablePositions[0][0] = SSOModifier.createObservation(2, 3, 13, 50, 40);
        nextSSO.fromAvatarSpritesPositions[0][0] = SSOModifier.createObservation(89, 3, 1, 50, 40);
        /*
        Feature indices for these are going to be:
                        >0      >1      >2
            Type  1      113     199      941
            Type  3      339	 597	 2823
            Type 13     1469	2587	12233
         */
    }

    @Test
    public void simpleStateValuation() {
        assertEquals(independent.value(sso), 0.0, 0.001);
        assertEquals(stateOnly.value(sso), 0.0, 0.001);
        assertEquals(lookahead.value(sso), 0.0, 0.001);

        // set some value-only coefficients
        independent.setCoeffFor(0, 339, 2.0);   // used
        stateOnly.setCoeffFor(339, 3.0);            // used
        lookahead.setCoeffFor(2587, 0.5);               // used
        lookahead.setCoeffFor(597, 2.5);            // not used

        assertEquals(independent.value(sso), 2.0, 0.001);
        assertEquals(stateOnly.value(sso), 3.0, 0.001);
        assertEquals(lookahead.value(sso), 0.5, 0.001);
    }

    @Test
    public void simpleActionStateValuation() {
        // set some value-only coefficients
        independent.setCoeffFor(0, 339, 2.0);   // used
        independent.setCoeffFor(2, 339, -10.0);  // for ACTIONS_ESCAPE
        lookahead.setCoeffFor(2587, 0.5);               // used
        lookahead.setCoeffFor(597, 2.5);            // not used

        assertEquals(independent.value(sso, ACTIONS.ACTION_LEFT), 0.0, 0.001);
        assertEquals(lookahead.value(sso, ACTIONS.ACTION_LEFT), 0.5, 0.001);

        assertEquals(independent.value(sso, ACTIONS.ACTION_ESCAPE), -10.0, 0.001);
        assertEquals(lookahead.value(sso, ACTIONS.ACTION_ESCAPE), 0.5, 0.001);

        // and an unseen action
        assertEquals(independent.value(sso, ACTIONS.ACTION_RIGHT), 0.0, 0.001);
        assertEquals(lookahead.value(sso, ACTIONS.ACTION_RIGHT), 0.5, 0.001);
    }

    @Test
    public void stateValuationAfterLearningSingleTuple() {
        // set some value-only coefficients
        independent.setCoeffFor(0, 339, 2.0);   // used
        stateOnly.setCoeffFor(339, 3.0);            // used
        lookahead.setCoeffFor(2587, 0.5);               // used
        lookahead.setCoeffFor(597, 2.5);            // not used
        assertEquals(independent.value(sso), 2.0, 0.001);
        assertEquals(stateOnly.value(sso), 3.0, 0.001);
        assertEquals(lookahead.value(sso), 0.5, 0.001);

        SARTuple trainingInstance = new SARTuple(sso, nextSSO, ACTIONS.ACTION_LEFT, allActions, allActions, 1.0);
        trainingInstance.target = 1.0;

        independent.learnValueFrom(trainingInstance, dummy);
        stateOnly.learnFrom(trainingInstance, dummy);
        lookahead.learnFrom(trainingInstance, dummy);

        /*
        alpha = 0.5, lambda = 0.01, and the following features are on in sso:
                339, 1469, 2587
                In each case, alpha will increase the parameter by 0.5 * target
                   and then lambda will reduce it to 99% of final value

                 independent target = -1
                 stateOnly target = -2
                 lookahead target = 0.5
         */
        assertEquals(independent.getCoeffFor(0, 339), 0.99 * (2.0 - 0.5), 0.001); // no change
        assertEquals(independent.getCoeffFor(1, 339), 0.0, 0.001); // no change
        assertEquals(independent.getCoeffFor(2, 339), 0.0, 0.001); // no change

        assertEquals(stateOnly.getCoeffFor(339), 0.99 * (3.0 - 1.0), 0.001);
        assertEquals(stateOnly.getCoeffFor(1469), 0.99 * -1.0, 0.001);
        assertEquals(stateOnly.getCoeffFor(2587), 0.99 * -1.0, 0.001);
        assertEquals(stateOnly.getCoeffFor(941), 0.0, 0.001);

        assertEquals(lookahead.getCoeffFor(339), 0.99 * 0.25, 0.001);
        assertEquals(lookahead.getCoeffFor(1469), 0.99 * 0.25, 0.001);
        assertEquals(lookahead.getCoeffFor(2587), 0.99 * (0.5 + 0.25), 0.001);
        assertEquals(lookahead.getCoeffFor(941), 0.0, 0.001);

        assertEquals(independent.value(sso), 0.99 * 1.5 + 2 * -0.495, 0.001);
        assertEquals(stateOnly.value(sso), 0.99 * (2.0 - 1.0 - 1.0), 0.001);
        assertEquals(lookahead.value(sso), 0.99 * (0.75 + 0.25 + 0.25), 0.001);
    }

    @Test
    public void actionStateValuationAfterLearning() {
        // set some value-only coefficients
        independent.setCoeffFor(0, 339, 2.0);   // used
        independent.setCoeffFor(1, 339, 1.0);   // ACTION_LEFT
        independent.setCoeffFor(2, 339, -1.0);   // ACTION_ESCAPE
        assertEquals(independent.value(sso), 2.0, 0.001);
        assertEquals(independent.value(sso, ACTIONS.ACTION_LEFT), 1.0, 0.001);
        assertEquals(independent.value(sso, ACTIONS.ACTION_NIL), 0.0, 0.001);
        assertEquals(independent.value(sso, ACTIONS.ACTION_ESCAPE), -1.0, 0.001);

        SARTuple trainingInstance = new SARTuple(sso, nextSSO, ACTIONS.ACTION_ESCAPE, allActions, allActions, 1.0);
        trainingInstance.target = 1.0;

        independent.learnFrom(trainingInstance, dummy);

        /*
        alpha = 0.5, lambda = 0.01, and the following features are on in sso:
                339, 1469, 2587
                In each case, alpha will increase the parameter by 0.5 * target
                   and then lambda will reduce it to 99% of final value

                 ESCAPE target = 2.0
         */
        assertEquals(independent.getCoeffFor(0, 339), 2.0, 0.001); // no change
        assertEquals(independent.getCoeffFor(1, 339), 1.0, 0.001); // no change
        assertEquals(independent.getCoeffFor(2, 339), 0.99  * (-1.0 + 1.0), 0.001); //changed

        assertEquals(independent.value(sso), 2.0, 0.001);
        assertEquals(independent.value(sso, ACTIONS.ACTION_LEFT), 1.0, 0.001);
        assertEquals(independent.value(sso, ACTIONS.ACTION_NIL), 0.0, 0.001);
        assertEquals(independent.value(sso, ACTIONS.ACTION_ESCAPE), 0.0 + 0.99 * 2.0, 0.001);
    }
}
