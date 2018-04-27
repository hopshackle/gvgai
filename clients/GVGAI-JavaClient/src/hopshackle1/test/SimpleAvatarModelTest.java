package hopshackle1.test;

import org.javatuples.*;
import org.junit.*;
import static org.junit.Assert.*;
import hopshackle1.models.*;
import serialization.*;
import serialization.Types.*;
import java.util.*;

public class SimpleAvatarModelTest {

    SerializableStateObservation baseSSO;
    SimpleAvatarModel model = new SimpleAvatarModel(10, 100, 100);
    GameStatusTrackerWithHistory gst;


    @Before
    public void setup() {
        baseSSO = SSOModifier.constructEmptySSO();
        baseSSO.avatarPosition = new double[] {35, 40};
        SSOModifier.constructGrid(baseSSO);
        gst = new GameStatusTrackerWithHistory();
        gst.update(baseSSO);
    }

    @Test
    public void defaultChangeForEachAction() {
        // apply to an sso for each ACTION, and confirm the result is as expected before observing data
        assertEquals(gst.getCurrentPosition(0).x, 35.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 40.0, 0.001);
        gst.rollForward(model, ACTIONS.ACTION_LEFT);
        assertEquals(gst.getCurrentPosition(0).x, 25.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 40.0, 0.001);
        gst.rollForward(model, ACTIONS.ACTION_NIL);
        assertEquals(gst.getCurrentPosition(0).x, 25.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 40.0, 0.001);
        gst.rollForward(model, ACTIONS.ACTION_ESCAPE);
        assertEquals(gst.getCurrentPosition(0).x, 25.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 40.0, 0.001);
        gst.rollForward(model, ACTIONS.ACTION_RIGHT);
        assertEquals(gst.getCurrentPosition(0).x, 35.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 40.0, 0.001);
        gst.rollForward(model, ACTIONS.ACTION_DOWN);
        assertEquals(gst.getCurrentPosition(0).x, 35.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 50.0, 0.001);
        gst.rollForward(model, ACTIONS.ACTION_UP);
        assertEquals(gst.getCurrentPosition(0).x, 35.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 40.0, 0.001);
        gst.rollForward(model, ACTIONS.ACTION_USE);
        assertEquals(gst.getCurrentPosition(0).x, 35.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 40.0, 0.001);
    }

    @Test
    public void defaultUpdatedWithData() {
        SerializableStateObservation newSSO = SSOModifier.copy(baseSSO);
        for (int i = 0; i < 200; i++) {
            newSSO.gameTick++;
            newSSO.avatarLastAction = ACTIONS.ACTION_LEFT;
            newSSO.avatarPosition[1] = (newSSO.avatarPosition[1] + 10) % 100; // move down instead of left, and then loop
            SSOModifier.constructGrid(newSSO);
            gst.update(newSSO);
        }
        model.updateModelStatistics(gst);
        // only item that has changed below is for ACTION_LEFT compared to defaultChangeForEachAction()
        assertEquals(gst.getCurrentPosition(0).x, 35.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 40.0, 0.001);
        gst.rollForward(model, ACTIONS.ACTION_LEFT);
        assertEquals(gst.getCurrentPosition(0).x, 35.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 50.0, 0.001);
        gst.rollForward(model, ACTIONS.ACTION_NIL);
        assertEquals(gst.getCurrentPosition(0).x, 35.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 50.0, 0.001);
        gst.rollForward(model, ACTIONS.ACTION_ESCAPE);
        assertEquals(gst.getCurrentPosition(0).x, 35.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 50.0, 0.001);
        gst.rollForward(model, ACTIONS.ACTION_RIGHT);
        assertEquals(gst.getCurrentPosition(0).x, 45.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 50.0, 0.001);
        gst.rollForward(model, ACTIONS.ACTION_DOWN);
        assertEquals(gst.getCurrentPosition(0).x, 45.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 60.0, 0.001);
        gst.rollForward(model, ACTIONS.ACTION_UP);
        assertEquals(gst.getCurrentPosition(0).x, 45.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 50.0, 0.001);
        gst.rollForward(model, ACTIONS.ACTION_USE);
        assertEquals(gst.getCurrentPosition(0).x, 45.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 50.0, 0.001);
    }

    @Test
    public void testOffScreen() {
        baseSSO = SSOModifier.constructEmptySSO();
        baseSSO.avatarPosition = new double[] {90, 40};
        SSOModifier.constructGrid(baseSSO);
        gst = new GameStatusTrackerWithHistory();
        gst.update(baseSSO);

        gst.rollForward(model, ACTIONS.ACTION_RIGHT);
        assertEquals(gst.getCurrentPosition(0).x, 90.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 40.0, 0.001);

        model.updateModelStatistics(gst);

        SerializableStateObservation newSSO = SSOModifier.copy(baseSSO);
        newSSO.avatarPosition = new double[] {70, 40};
        SSOModifier.constructGrid(newSSO);
        gst = new GameStatusTrackerWithHistory();
        gst.update(newSSO);

        // TODO: Then check the actual p(NO MOVE | RIGHT) is updated as expected (i.e. no change to base case)
        List<Pair<Double, Vector2d>> basePdf = model.nextMovePdfWithoutPassability(gst, ACTIONS.ACTION_RIGHT);
        List<Pair<Double, Vector2d>> pdf = model.nextMovePdf(gst, 0, ACTIONS.ACTION_RIGHT);
        assertEquals(basePdf.get(0).getValue0(), 1.00, 0.001);
        assertEquals(pdf.get(0).getValue0(), 1.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 80.0, 0.01);
        assertEquals(pdf.get(0).getValue1().y, 40.0, 0.01);

        // then check that we still expect to move correctly if not at the edge of the screen
        gst.rollForward(model, ACTIONS.ACTION_RIGHT);
        assertEquals(gst.getCurrentPosition(0).x, 80.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 40.0, 0.001);




    }

    @Test
    public void testPassabilityOneMove() {
        // we have the avatar at one position, with a sprite just below it
        // we then move DOWN, without changing position, and confirm the passability update is correct
        // and also that the update to expected movement on DOWN is correct
        // we then repeat...passability up, and expected movement down also up, but by less.
        baseSSO = SSOModifier.constructEmptySSO();
        baseSSO.avatarPosition = new double[] {50, 50};
        SSOModifier.addSprite(65, SSOModifier.TYPE_STATIC, 4, 50, 60, baseSSO);
        SSOModifier.constructGrid(baseSSO);
        gst = new GameStatusTrackerWithHistory();
        gst.update(baseSSO);

        SerializableStateObservation nextSSO = SSOModifier.copy(baseSSO);
        nextSSO.gameTick++;
        nextSSO.avatarLastAction = ACTIONS.ACTION_DOWN;
        gst.update(nextSSO);

        List<Pair<Double, Vector2d>> basePdf = model.nextMovePdfWithoutPassability(gst, ACTIONS.ACTION_DOWN);
        List<Pair<Double, Vector2d>> pdf = model.nextMovePdf(gst, 0, ACTIONS.ACTION_DOWN);
        assertEquals(basePdf.get(0).getValue0(), 1.00, 0.0001);
        assertEquals(pdf.get(0).getValue0(), 1.00, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 50.0, 0.01);
        assertEquals(pdf.get(0).getValue1().y, 60.0, 0.01);

        model.updateModelStatistics(gst);
        basePdf = model.nextMovePdfWithoutPassability(gst, ACTIONS.ACTION_DOWN);
        pdf = model.nextMovePdf(gst, 0, ACTIONS.ACTION_DOWN);
        assertEquals(basePdf.get(0).getValue0(), 5.0/6.0, 0.0001);
        double prediction = (5.0 / 6.0 * 1.0 / 3.0) / (5.0 / 6.0 * 1.0 / 3.0 + 1.0 / 6.0);
        // this should equal 0.625
        assertEquals(pdf.get(0).getValue0(), prediction, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 50.0, 0.01);
        assertEquals(pdf.get(0).getValue1().y, 60.0, 0.01);
        assertEquals(pdf.get(1).getValue0(), 1.0 - prediction, 0.0001);
        assertEquals(pdf.get(1).getValue1().x, 50.0, 0.01);
        assertEquals(pdf.get(1).getValue1().y, 50.0, 0.01);
    }

    @Test
    public void testPassabilityTwoMoves() {
        baseSSO = SSOModifier.constructEmptySSO();
        baseSSO.avatarPosition = new double[] {50, 50};
        SSOModifier.addSprite(65, SSOModifier.TYPE_STATIC, 4, 50, 60, baseSSO);
        SSOModifier.constructGrid(baseSSO);
        gst = new GameStatusTrackerWithHistory();
        gst.update(baseSSO);

        SerializableStateObservation nextSSO = SSOModifier.copy(baseSSO);
        nextSSO.gameTick++;
        nextSSO.avatarLastAction = ACTIONS.ACTION_DOWN;
        gst.update(nextSSO);

        nextSSO = SSOModifier.copy(nextSSO);
        nextSSO.gameTick++;
        gst.update(nextSSO);

        model.updateModelStatistics(gst);

        List<Pair<Double, Vector2d>> basePdf = model.nextMovePdfWithoutPassability(gst, ACTIONS.ACTION_DOWN);
        List<Pair<Double, Vector2d>> pdf = model.nextMovePdf(gst, 0, ACTIONS.ACTION_DOWN);

        // the first move adds 1.0 to the notMove count; the second move adds 0.1667 as the p(not moving) and 0.8333 / 3.0 as the p(tried to move, and would have succeeded)
        // with the rest of the 1.0 added to move count
        double move = 5.0 + 0.8333 * 2.0 / 3.0;
        double notMove = 1.0 + 0.16667 + 0.833333 * 1.0 / 3.0;
        assertEquals(basePdf.get(0).getValue0(), move / (move + notMove), 0.001);
        // we update the denominator of the passability of the sprite by 0.8333, the raw p(move there)
        double moveWithPassability = move / (move + notMove) * (1.0 / (3.0 + 0.833333));
        double prediction = moveWithPassability / (moveWithPassability + notMove / (move + notMove));
        assertEquals(pdf.get(0).getValue0(), prediction, 0.0001);
        assertEquals(pdf.get(0).getValue1().x, 50.0, 0.01);
        assertEquals(pdf.get(0).getValue1().y, 60.0, 0.01);
        assertEquals(pdf.get(1).getValue0(), 1.0 - prediction, 0.001);
        assertEquals(pdf.get(1).getValue1().x, 50.0, 0.01);
        assertEquals(pdf.get(1).getValue1().y, 50.0, 0.01);
    }
}
