package hopshackle1.test;

import org.javatuples.*;
import org.junit.*;

import static org.junit.Assert.*;

import hopshackle1.models.*;
import serialization.*;

import java.util.*;


public class GameStatusTrackerTest {

    SerializableStateObservation sso;
    GameStatusTrackerWithHistory gst;

    @Before
    public void setup() {
        sso = SSOModifier.constructEmptySSO();
        gst = new GameStatusTrackerWithHistory();
    }


    @Test
    public void noCollisions() {
        SSOModifier.addSprite(6, 6, 4, 45, 50, sso);
        updateGST();
        Set<Pair<Integer, Integer>> collisions = SSOModifier.detectCollisions(gst.getCurrentSSO());
        assertTrue(collisions.isEmpty());

        SSOModifier.addSprite(7, 6, 4, 60, 20, sso);
        updateGST();
        collisions = SSOModifier.detectCollisions(gst.getCurrentSSO());
        assertTrue(collisions.isEmpty());
    }

    @Test
    public void simpleCollision() {
        SSOModifier.addSprite(6, 6, 4, 45, 50, sso);
        updateGST();
        Set<Pair<Integer, Integer>> collisions = SSOModifier.detectCollisions(gst.getCurrentSSO());
        assertTrue(collisions.isEmpty());

        SSOModifier.addSprite(7, 3, 4, 38, 50, sso);
        updateGST();
        collisions = SSOModifier.detectCollisions(gst.getCurrentSSO());
        assertEquals(collisions.size(), 1);
        assertTrue(collisions.contains(new Pair(6, 7)));
    }

    @Test
    public void collisionAfterMove() {
        SSOModifier.addSprite(6, 6, 4, 40, 50, sso);
        updateGST();
        Set<Pair<Integer, Integer>> collisions = SSOModifier.detectCollisions(gst.getCurrentSSO());
        assertTrue(collisions.isEmpty());

        SSOModifier.addSprite(7, 3, 4, 30, 50, sso);
        updateGST();
        collisions = SSOModifier.detectCollisions(gst.getCurrentSSO());
        assertTrue(collisions.isEmpty());

        SSOModifier.moveSprite(7, 3, 35, 50, sso);
        updateGST();
        collisions = SSOModifier.detectCollisions(gst.getCurrentSSO());
        assertEquals(collisions.size(), 1);
        assertTrue(collisions.contains(new Pair(6, 7)));
    }

    @Test
    public void multipleCollisions() {
        SSOModifier.addSprite(6, 6, 4, 45, 50, sso);
        SSOModifier.addSprite(0, 0, 0, 45, 50, sso);
        SSOModifier.addSprite(8, 5, 4, 30, 55, sso);
        SSOModifier.addSprite(9, 6, 4, 40, 45, sso);
        SSOModifier.addSprite(10, 2, 1, 35, 60, sso);
        updateGST();
        Set<Pair<Integer, Integer>> collisions = SSOModifier.detectCollisions(gst.getCurrentSSO());
        assertEquals(collisions.size(), 4);
        assertTrue(collisions.contains(new Pair(10, 8)));
        assertTrue(collisions.contains(new Pair(6, 0)));
        assertTrue(collisions.contains(new Pair(6, 9)));
        assertTrue(collisions.contains(new Pair(9, 0)));
    }

    private void updateGST() {
        SSOModifier.constructGrid(sso);
        gst.update(sso);
        sso = SSOModifier.copy(sso);
        sso.gameTick++;
    }
}
