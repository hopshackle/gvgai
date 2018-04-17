package hopshackle1.test;

import hopshackle1.*;

import java.util.*;

import hopshackle1.FeatureSets.*;
import hopshackle1.RL.*;
import hopshackle1.models.*;
import org.javatuples.Pair;
import org.junit.*;
import serialization.*;
import serialization.Types.*;

import static org.junit.Assert.*;

public class BehaviouralLookaheadFunctionTest {

    SerializableStateObservation[] SSOSequence;
    GameStatusTrackerWithHistory gst;
    BehaviouralLookaheadFunction bmf;

    @Before
    public void setup() {
        bmf = new BehaviouralLookaheadFunction();
        bmf.setAllPassable(true);
        SSOSequence = new SerializableStateObservation[10];
        SerializableStateObservation baseSSO = SSOModifier.constructEmptySSO();
        SSOModifier.addSprite(1, SSOModifier.TYPE_STATIC, 1, 60, 60, baseSSO);
        SSOModifier.addSprite(2, SSOModifier.TYPE_STATIC, 1, 35, 50, baseSSO);
        SSOModifier.addSprite(0, SSOModifier.TYPE_AVATAR, 2, 45, 50, baseSSO);
        // So Avatar is one block to the right of the second obstacle. But due to bothg being off-grid,
        // they actually overlap in the (40, 50) block
        SSOModifier.addSprite(4, SSOModifier.TYPE_NPC, 3, 95, 50, baseSSO);
        SSOModifier.constructGrid(baseSSO);

        SSOSequence[0] = baseSSO;
        gst = new GameStatusTrackerWithHistory();
        gst.update(baseSSO);

        SSOSequence[1] = SSOModifier.copy(baseSSO);
        SSOSequence[1].gameTick = 1;
        SSOModifier.moveSprite(4, SSOModifier.TYPE_NPC, 85, 50, SSOSequence[1]);
        SSOSequence[1].avatarLastAction = ACTIONS.ACTION_NIL;
        SSOModifier.constructGrid(SSOSequence[1]);

        SSOSequence[2] = SSOModifier.copy(SSOSequence[1]);
        SSOModifier.moveSprite(0, SSOModifier.TYPE_AVATAR, 35, 50, SSOSequence[2]);
        SSOModifier.moveSprite(4, SSOModifier.TYPE_NPC, 75, 50, SSOSequence[2]);
        SSOSequence[2].avatarLastAction = ACTIONS.ACTION_LEFT;
        SSOSequence[2].gameTick = 2;
        SSOModifier.constructGrid(SSOSequence[2]);

        SSOSequence[3] = SSOModifier.copy(SSOSequence[2]);
        SSOModifier.moveSprite(0, SSOModifier.TYPE_AVATAR, 45, 50, SSOSequence[3]);
        SSOModifier.moveSprite(4, SSOModifier.TYPE_NPC, 65, 50, SSOSequence[3]);
        SSOSequence[3].avatarLastAction = ACTIONS.ACTION_RIGHT;
        SSOSequence[3].gameTick = 3;
        SSOModifier.constructGrid(SSOSequence[3]);

    }

    @Test
    public void applyMovesInSequenceAndCheckLeftRightPDFs() {
        bmf.updateModelStatistics(gst);
        // NPC
        List<Pair<Double, Vector2d>> pdf = bmf.nextMovePdf(gst, 4, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 95.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        // Avatar
        pdf = bmf.nextMovePdf(gst, 0, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 35.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        pdf = bmf.nextMovePdf(gst, 0, ACTIONS.ACTION_RIGHT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 55.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);

        bmf = new BehaviouralLookaheadFunction();
        gst.update(SSOSequence[1]);
        bmf.updateModelStatistics(gst);
        // NPC
        pdf = bmf.nextMovePdf(gst, 4, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 2);
        assertEquals(pdf.get(0).getValue0(), 5.0 / 6.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 85.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        assertEquals(pdf.get(1).getValue0(), 1.0 / 6.0, 0.0001);
        assertEquals(pdf.get(1).getValue1().x, 75.00, 0.0001);
        assertEquals(pdf.get(1).getValue1().y, 50.00, 0.0001);
        // Avatar
        pdf = bmf.nextMovePdf(gst, 0, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 35.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        pdf = bmf.nextMovePdf(gst, 0, ACTIONS.ACTION_RIGHT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 55.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);

        bmf = new BehaviouralLookaheadFunction();
        gst.update(SSOSequence[2]);
        bmf.updateModelStatistics(gst);
        // NPC
        pdf = bmf.nextMovePdf(gst, 4, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 2);
        assertEquals(pdf.get(0).getValue0(), 5.0 / 7.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 75.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        assertEquals(pdf.get(1).getValue0(), 2.0 / 7.0, 0.0001);
        assertEquals(pdf.get(1).getValue1().x, 65.00, 0.0001);
        assertEquals(pdf.get(1).getValue1().y, 50.00, 0.0001);
        // Avatar
        pdf = bmf.nextMovePdf(gst, 0, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 25.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        pdf = bmf.nextMovePdf(gst, 0, ACTIONS.ACTION_RIGHT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 45.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);

        bmf = new BehaviouralLookaheadFunction();
        bmf.setAllPassable(true);
        gst.update(SSOSequence[3]);
        bmf.updateModelStatistics(gst);

        pdf = bmf.nextMovePdf(gst, 4, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 2);
        assertEquals(pdf.get(0).getValue0(), 5.0 / 8.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 65.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        assertEquals(pdf.get(1).getValue0(), 3.0 / 8.0, 0.0001);
        assertEquals(pdf.get(1).getValue1().x, 55.00, 0.0001);
        assertEquals(pdf.get(1).getValue1().y, 50.00, 0.0001);
        // Avatar
        pdf = bmf.nextMovePdf(gst, 0, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 35.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        pdf = bmf.nextMovePdf(gst, 0, ACTIONS.ACTION_RIGHT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 55.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        pdf = bmf.nextMovePdf(gst, 0, ACTIONS.ACTION_NIL);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 45.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
    }

    @Test
    public void applyMovesInSequenceAndCheckOneWidthFeaturesToDistanceOne() {
        State[] baseStates = new State[4];
        FeatureSet[] fs = new FeatureSet[]{new AvatarMeshWidthOneFeatureSet(1)};
        for (int i = 0; i < 4; i++) {
            baseStates[i] = new State(SSOSequence[i], fs);
        }

        assertTrue(baseStates[0].equals(baseStates[1]));
        assertFalse(baseStates[1].equals(baseStates[2]));
        assertFalse(baseStates[2].equals(baseStates[3]));
        assertTrue(baseStates[0].equals(baseStates[3]));

        assertTrue(baseStates[0].features.containsKey(44741));
        assertTrue(baseStates[0].features.containsKey(39357)); // the avatar + block
        assertTrue(baseStates[0].features.containsKey(30016));

        assertTrue(baseStates[1].features.containsKey(39357)); // the avatar + block
        assertTrue(baseStates[2].features.containsKey(39357)); // the avatar + block
        assertTrue(baseStates[3].features.containsKey(39357)); // the avatar + block

        assertEquals(baseStates[0].features.size(), 3); // avatar, avatar+block1, block1
        assertEquals(baseStates[1].features.size(), 3); // no move
        assertEquals(baseStates[2].features.size(), 2); // avatar+block1, avatar+block1
        assertEquals(baseStates[3].features.size(), 3); // avatar, avatar+block1, block1
    }

    @Test
    public void applyMovesInSequenceAndCheckOneWidthFeaturesToDistanceTwo() {
        State[] baseStates = new State[4];
        FeatureSet[] fs = new FeatureSet[]{new AvatarMeshWidthOneFeatureSet(2)};
        for (int i = 0; i < 4; i++) {
            baseStates[i] = new State(SSOSequence[i], fs);
        }

        assertTrue(baseStates[0].equals(baseStates[1]));
        assertFalse(baseStates[1].equals(baseStates[2]));
        assertFalse(baseStates[2].equals(baseStates[3]));
        assertFalse(baseStates[0].equals(baseStates[3])); // this time the alien has arrived

        assertTrue(baseStates[0].features.containsKey(44741));
        assertTrue(baseStates[0].features.containsKey(39357)); // the avatar
        assertTrue(baseStates[0].features.containsKey(30016));

        assertTrue(baseStates[1].features.containsKey(39357)); // the avatar
        assertTrue(baseStates[2].features.containsKey(39357)); // the avatar
        assertTrue(baseStates[3].features.containsKey(39357)); // the avatar

        assertEquals(baseStates[0].features.size(), 4); // avatar, avatar+block1, block1, block2
        assertEquals(baseStates[1].features.size(), 4); // no move
        assertEquals(baseStates[2].features.size(), 2); // avatar+block1, avatar+block1
        assertEquals(baseStates[3].features.size(), 5); // as [0], but with alien
    }

    @Test
    public void checkRollForwardIsCorrect() {
        State[] baseStates = new State[4];
        FeatureSet[] fs = new FeatureSet[]{new AvatarMeshWidthOneFeatureSet(2)};
        for (int i = 0; i < 4; i++) {
            baseStates[i] = new State(SSOSequence[i], fs);
        }
        for (int i = 1; i < 4; i++)
            gst.update(SSOSequence[i]);

        bmf.updateModelStatistics(gst);
        // the bmf now has a more interesting model for the alien (3/8 will move, 5/8 stay still)
        GameStatusTracker gst2 = new GameStatusTracker();
        gst2.update(SSOSequence[0]);
        SerializableStateObservation rolledForwardSSO = bmf.rollForward(gst2, ACTIONS.ACTION_LEFT);
        // say we had moved left initially, rather than after one turn of doing nothing
        // this should give is the same state exactly as SSOSequence[2]
        State alternateState = new State(rolledForwardSSO, fs);
        assertTrue(alternateState.equals(baseStates[2]));

        int alienMovedLeft = 0;
        int alienStatic = 0;    // because gst2 has no direction for aliens yet, we will predict that they remain static
        for (int i = 0; i < 100; i++) {
            // gst2 should be unchanged
            SerializableStateObservation tempSSO = bmf.rollForward(gst2, ACTIONS.ACTION_DOWN);
            assertEquals(tempSSO.getAvatarPosition()[0], 45.0, 0.001);
            assertEquals(tempSSO.getAvatarPosition()[1], 60.0, 0.001);
            if (tempSSO.NPCPositions[0][0].position.x == 85.0)
                alienMovedLeft++;
            if (tempSSO.NPCPositions[0][0].position.x == 95.0)
                alienStatic++;
        }
        assertEquals(alienMovedLeft + alienStatic, 100);
        assertEquals(alienStatic, 100, 0);

        gst2.update(SSOSequence[1]);
        gst2.update(SSOSequence[2]);
        // this will now mean we have a direction for the aliens
        alienStatic = 0;
        for (int i = 0; i < 100; i++) {
            SerializableStateObservation tempSSO = bmf.rollForward(gst2, ACTIONS.ACTION_DOWN);
            assertEquals(tempSSO.getAvatarPosition()[0], 35.0, 0.001);
            assertEquals(tempSSO.getAvatarPosition()[1], 60.0, 0.001);
            if (tempSSO.NPCPositions[0][0].position.x == 65.0)
                alienMovedLeft++;
            if (tempSSO.NPCPositions[0][0].position.x == 75.0)
                alienStatic++;
        }
        assertEquals(alienMovedLeft + alienStatic, 100);
        assertEquals(alienMovedLeft, 50, 15);
    }
}
