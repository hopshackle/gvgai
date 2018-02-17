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
    BehaviouralLookaheadFunction bmf = new BehaviouralLookaheadFunction();

    @Before
    public void setup() {
        SSOSequence = new SerializableStateObservation[10];
        SerializableStateObservation baseSSO = SSOModifier.constructEmptySSO();
        SSOModifier.addSprite(1, SSOModifier.TYPE_STATIC, 1, 60, 60, baseSSO);
        SSOModifier.addSprite(2, SSOModifier.TYPE_STATIC, 1, 35, 50, baseSSO);
        SSOModifier.addSprite(0, SSOModifier.TYPE_AVATAR, 2, 45, 50, baseSSO);
        SSOModifier.addSprite(4, SSOModifier.TYPE_NPC, 3, 95, 50, baseSSO);
        SSOModifier.constructGrid(baseSSO);

        SSOSequence[0] = baseSSO;

        SSOSequence[1] = SSOModifier.copy(baseSSO);
        SSOModifier.moveSprite(4, SSOModifier.TYPE_NPC, 85, 50, SSOSequence[1]);
        SSOSequence[1].avatarLastAction = ACTIONS.ACTION_NIL;

        SSOSequence[2] = SSOModifier.copy(SSOSequence[1]);
        SSOModifier.moveSprite(0, SSOModifier.TYPE_AVATAR, 35, 50, SSOSequence[2]);
        SSOModifier.moveSprite(4, SSOModifier.TYPE_NPC, 75, 50, SSOSequence[2]);
        SSOSequence[2].avatarLastAction = ACTIONS.ACTION_LEFT;

        SSOSequence[3] = SSOModifier.copy(SSOSequence[2]);
        SSOModifier.moveSprite(0, SSOModifier.TYPE_AVATAR, 45, 50, SSOSequence[3]);
        SSOModifier.moveSprite(4, SSOModifier.TYPE_NPC, 65, 50, SSOSequence[3]);
        SSOSequence[3].avatarLastAction = ACTIONS.ACTION_RIGHT;
    }

    @Test
    public void applyMovesInSequenceAndCheckLeftRightPDFs() {
        bmf.updateModelStatistics(SSOSequence[0]);
        // NPC
        List<Pair<Double, Vector2d>>  pdf = bmf.nextMovePdf(4, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 95.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        // Avatar
        pdf = bmf.nextMovePdf(0, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 35.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        pdf = bmf.nextMovePdf(0, ACTIONS.ACTION_RIGHT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 55.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);

        bmf.updateModelStatistics(SSOSequence[1]);
        // NPC
        pdf = bmf.nextMovePdf(4, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 2);
        assertEquals(pdf.get(0).getValue0(), 5.0/6.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 85.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        assertEquals(pdf.get(1).getValue0(), 1.0/6.0, 0.0001);
        assertEquals(pdf.get(1).getValue1().x, 75.00, 0.0001);
        assertEquals(pdf.get(1).getValue1().y, 50.00, 0.0001);
        // Avatar
        pdf = bmf.nextMovePdf(0, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 35.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        pdf = bmf.nextMovePdf(0, ACTIONS.ACTION_RIGHT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 55.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);

        bmf.updateModelStatistics(SSOSequence[2]);
        // NPC
        pdf = bmf.nextMovePdf(4, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 2);
        assertEquals(pdf.get(0).getValue0(), 5.0/7.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 75.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        assertEquals(pdf.get(1).getValue0(), 2.0/7.0, 0.0001);
        assertEquals(pdf.get(1).getValue1().x, 65.00, 0.0001);
        assertEquals(pdf.get(1).getValue1().y, 50.00, 0.0001);
        // Avatar
        pdf = bmf.nextMovePdf(0, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 25.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        pdf = bmf.nextMovePdf(0, ACTIONS.ACTION_RIGHT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 45.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);

        bmf.updateModelStatistics(SSOSequence[3]);
        pdf = bmf.nextMovePdf(4, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 2);
        assertEquals(pdf.get(0).getValue0(), 5.0/8.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 65.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        assertEquals(pdf.get(1).getValue0(), 3.0/8.0, 0.0001);
        assertEquals(pdf.get(1).getValue1().x, 55.00, 0.0001);
        assertEquals(pdf.get(1).getValue1().y, 50.00, 0.0001);
        // Avatar
        pdf = bmf.nextMovePdf(0, ACTIONS.ACTION_LEFT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 35.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        pdf = bmf.nextMovePdf(0, ACTIONS.ACTION_RIGHT);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 55.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
        pdf = bmf.nextMovePdf(0, ACTIONS.ACTION_NIL);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.0, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 45.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().y, 50.00, 0.0001);
    }

    @Test
    public void applyMovesInSequenceAndCheckLeftRightFeatures() {
        fail("Not yet implemented");
    }
}
