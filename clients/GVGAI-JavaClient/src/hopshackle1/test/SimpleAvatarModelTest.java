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
        gst = new GameStatusTrackerWithHistory();
        gst.update(baseSSO);

        gst.rollForward(model, ACTIONS.ACTION_RIGHT);
        assertEquals(gst.getCurrentPosition(0).x, 90.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 40.0, 0.001);

        SerializableStateObservation newSSO = SSOModifier.copy(baseSSO);
        for (int i = 0; i < 20; i++) {
            newSSO.gameTick++;
            newSSO.avatarLastAction = ACTIONS.ACTION_RIGHT;
            // stay static
            gst.update(newSSO);
        }
        model.updateModelStatistics(gst);

        gst.rollForward(model, ACTIONS.ACTION_RIGHT);
        assertEquals(gst.getCurrentPosition(0).x, 90.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 40.0, 0.001);

        newSSO = SSOModifier.copy(baseSSO);
        newSSO.avatarPosition = new double[] {70, 40};
        gst = new GameStatusTrackerWithHistory();
        gst.update(newSSO);

        gst.rollForward(model, ACTIONS.ACTION_RIGHT);
        assertEquals(gst.getCurrentPosition(0).x, 80.0, 0.001);
        assertEquals(gst.getCurrentPosition(0).y, 40.0, 0.001);
    }
}
