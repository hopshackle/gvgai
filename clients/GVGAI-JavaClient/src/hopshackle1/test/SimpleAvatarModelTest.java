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
    SimpleAvatarModel model = new SimpleAvatarModel(10);


    @Before
    public void setup() {
        baseSSO = SSOModifier.constructEmptySSO();
        baseSSO.avatarPosition = new double[] {35, 40};
        model.updateModelStatistics(baseSSO);
    }

    @Test
    public void defaultChangeForEachAction() {
        // apply to an sso for each ACTION, and confirm the result is as expected before observing data
        assertEquals(baseSSO.avatarPosition[0], 35.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 40.0, 0.001);
        model.apply(baseSSO, ACTIONS.ACTION_LEFT);
        assertEquals(baseSSO.avatarPosition[0], 25.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 40.0, 0.001);
        model.apply(baseSSO, ACTIONS.ACTION_NIL);
        assertEquals(baseSSO.avatarPosition[0], 35.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 40.0, 0.001);
        model.apply(baseSSO, ACTIONS.ACTION_ESCAPE);
        assertEquals(baseSSO.avatarPosition[0], 35.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 40.0, 0.001);
        model.apply(baseSSO, ACTIONS.ACTION_RIGHT);
        assertEquals(baseSSO.avatarPosition[0], 45.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 40.0, 0.001);
        model.apply(baseSSO, ACTIONS.ACTION_DOWN);
        assertEquals(baseSSO.avatarPosition[0], 35.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 50.0, 0.001);
        model.apply(baseSSO, ACTIONS.ACTION_UP);
        assertEquals(baseSSO.avatarPosition[0], 35.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 30.0, 0.001);
        model.apply(baseSSO, ACTIONS.ACTION_USE);
        assertEquals(baseSSO.avatarPosition[0], 35.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 40.0, 0.001);
    }

    @Test
    public void defaultUpdatedWithData() {
        SerializableStateObservation newSSO = SSOModifier.copy(baseSSO);
        for (int i = 0; i < 200; i++) {
            newSSO.avatarLastAction = ACTIONS.ACTION_LEFT;
            newSSO.avatarPosition[1] = (newSSO.avatarPosition[1] + 10) % 100; // move down instead of left, and then loop
            model.updateModelStatistics(newSSO);
        }
        // only item that has changed below is for ACTION_LEFT compared to defaultChangeForEachAction()
        assertEquals(baseSSO.avatarPosition[0], 35.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 40.0, 0.001);
        model.apply(baseSSO, ACTIONS.ACTION_LEFT);
        assertEquals(baseSSO.avatarPosition[0], 35.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 50.0, 0.001);
        model.apply(baseSSO, ACTIONS.ACTION_NIL);
        assertEquals(baseSSO.avatarPosition[0], 35.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 40.0, 0.001);
        model.apply(baseSSO, ACTIONS.ACTION_ESCAPE);
        assertEquals(baseSSO.avatarPosition[0], 35.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 40.0, 0.001);
        model.apply(baseSSO, ACTIONS.ACTION_RIGHT);
        assertEquals(baseSSO.avatarPosition[0], 45.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 40.0, 0.001);
        model.apply(baseSSO, ACTIONS.ACTION_DOWN);
        assertEquals(baseSSO.avatarPosition[0], 35.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 50.0, 0.001);
        model.apply(baseSSO, ACTIONS.ACTION_UP);
        assertEquals(baseSSO.avatarPosition[0], 35.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 30.0, 0.001);
        model.apply(baseSSO, ACTIONS.ACTION_USE);
        assertEquals(baseSSO.avatarPosition[0], 35.0, 0.001);
        assertEquals(baseSSO.avatarPosition[1], 40.0, 0.001);
    }
}
