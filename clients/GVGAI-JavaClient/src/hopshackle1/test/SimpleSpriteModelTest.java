package hopshackle1.test;

import org.javatuples.*;
import org.junit.*;

import static org.junit.Assert.*;
import hopshackle1.models.*;
import serialization.*;

import java.util.*;

public class SimpleSpriteModelTest {

    SimpleSpriteModel model;

    @Before
    public void setup() {
        model = new SimpleSpriteModel(5);
        model.associateWithSprite(4);
    }

    @Test
    public void correctStatisticsBeforeAnyData() {
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 45, 50));
        List<Pair<Double, Vector2d>> pdf = model.nextMovePdf(6, null);
        assertEquals(pdf.size(), 1);
        assertEquals(pdf.get(0).getValue0(), 1.00, 0.001);
        assertEquals(pdf.get(0).getValue1().x, 45.0, 0.001);
        assertEquals(pdf.get(0).getValue1().y, 50.0, 0.001);

        Vector2d move = model.nextMoveMAP(6, null);
        assertEquals(move.x, 45.0, 0.001);
        assertEquals(move.y, 50.0, 0.001);

        move = model.nextMoveRandom(6, null);
        assertEquals(move.x, 45.0, 0.001);
        assertEquals(move.y, 50.0, 0.001);
    }

    @Test
    public void correctStatisticsAfterSomeMoves() {
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 45, 50));
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 45, 55));
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 48, 58));
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 48, 58));
        // that should be S (4), SE (3), Stationary (-)
        // direction changes of 0, 7, Static

        List<Pair<Double, Vector2d>> pdf = model.nextMovePdf(6, null);
        assertEquals(pdf.size(), 3);        // static, move forward, turn half-left
        assertEquals(pdf.get(0).getValue0(), 6.0 / 8.0, 0.001); // stasis
        assertEquals(pdf.get(0).getValue1().x, 48.0, 0.001);
        assertEquals(pdf.get(0).getValue1().y, 58.0, 0.001);

        assertEquals(pdf.get(1).getValue0(), 1.0 / 8.0, 0.001); // forward (0)
        assertEquals(pdf.get(1).getValue1().x, 53.0, 0.001);
        assertEquals(pdf.get(1).getValue1().y, 63.0, 0.001);

        assertEquals(pdf.get(2).getValue0(), 1.0 / 8.0, 0.001); // half-left (7)
        assertEquals(pdf.get(2).getValue1().x, 53.0, 0.001);
        assertEquals(pdf.get(2).getValue1().y, 58.0, 0.001);

        Vector2d mapMove = model.nextMoveMAP(6, null);
        assertEquals(mapMove.x, 48.0, 0.001);
        assertEquals(mapMove.y, 58.0, 0.001);
    }

    @Test
    public void correctRandomMoves() {
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 45, 50));
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 45, 55));
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 48, 58));
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 48, 58));
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 53, 58));
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 50, 62));
        // that should be S (4), SE (3), Stationary (-), E (2), SW (5)
        // direction changes of 0, 7, Static, 7, 3

        List<Pair<Double, Vector2d>> pdf = model.nextMovePdf(6, null);
        assertEquals(pdf.size(), 4);        // static, straightOn, hardRight, softLeft
        assertEquals(pdf.get(0).getValue0(), 0.6, 0.001); // stasis
        assertEquals(pdf.get(0).getValue1().x, 50.0, 0.001);
        assertEquals(pdf.get(0).getValue1().y, 62.0, 0.001);

        assertEquals(pdf.get(1).getValue0(), 0.1, 0.001); // forward (SW)
        assertEquals(pdf.get(1).getValue1().x, 45.0, 0.001);
        assertEquals(pdf.get(1).getValue1().y, 67.0, 0.001);

        assertEquals(pdf.get(2).getValue0(), 0.1, 0.001); // hardRight (N)
        assertEquals(pdf.get(2).getValue1().x, 50.0, 0.001);
        assertEquals(pdf.get(2).getValue1().y, 57.0, 0.001);

        assertEquals(pdf.get(3).getValue0(), 0.2, 0.001); // softLeft (S)
        assertEquals(pdf.get(3).getValue1().x, 50.0, 0.001);
        assertEquals(pdf.get(3).getValue1().y, 67.0, 0.001);

        int stationary = 0, hardRight = 0, softLeft = 0, straightOn = 0;
        for (int i = 0; i < 1000; i++) {
            Vector2d m = model.nextMoveRandom(6, null);
            double x = m.x;
            double y = m.y;
            if (x < 50.1 && x > 49.9 && y < 62.1 && y > 61.9) stationary++;
            if (x < 50.1 && x > 49.9 && y < 57.1 && y > 56.9) hardRight++; // new direction is N
            if (x < 50.1 && x > 49.9 && y < 67.1 && y > 66.9) softLeft++; // new direction is S
            if (x < 45.1 && x > 44.9 && y < 67.1 && y > 66.9) straightOn++; // new direction is SW
        }

        int total = stationary + hardRight + softLeft + straightOn;
        assertEquals(total, 1000);
        assertEquals(stationary, 600, 47);      // delta is set to 3 sd
        assertEquals(softLeft, 200, 38);
        assertEquals(hardRight, 100, 29);
        assertEquals(straightOn, 100, 29);
    }

    @Test
    public void checkAllDirections() {
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 45, 50));
        Vector2d pos = model.getCurrentPosition(6);
        assertEquals(pos.x, 45.0, 0.001);
        assertEquals(pos.y, 50.0, 0.001);
        assertEquals(model.getDirection(6), 0);

        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 50, 50));
        pos = model.getCurrentPosition(6);
        assertEquals(pos.x, 50.0, 0.001);
        assertEquals(pos.y, 50.0, 0.001);
        assertEquals(model.getDirection(6), 2);

        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 48, 47));
        assertEquals(model.getDirection(6), 8);      // x movement is below threshold
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 43, 47));
        assertEquals(model.getDirection(6), 6);
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 47, 42));
        assertEquals(model.getDirection(6), 1);
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 43, 47));
        assertEquals(model.getDirection(6), 5);
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 47, 50));
        assertEquals(model.getDirection(6), 3);
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 47, 55));
        assertEquals(model.getDirection(6), 4);
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 43, 51));
        assertEquals(model.getDirection(6), 7);
        model.updateStatisticsFrom(createNPCObservation(6, 6, 4, 43, 51));
        assertEquals(model.getDirection(6), 7); // no change
    }

    @Test
    public void applyBehaviourModelToSSO() {
        SerializableStateObservation sso = SSOModifier.constructEmptySSO();
        sso.NPCPositions = new Observation[2][1];
        sso.NPCPositions[0][0] = createNPCObservation(8, 6, 4, 50, 50);
        sso.NPCPositions[1][0] = createNPCObservation(11, 6, 4, 25, 20);
        sso.movablePositions = new Observation[1][1];
        sso.movablePositions[0][0] = createNPCObservation(20, 6, 2, 20, 20);
        sso.fromAvatarSpritesPositions = new Observation[1][1];
        sso.fromAvatarSpritesPositions[0][0] = createNPCObservation(60, 6, 6, 0, 20);

        model = new SimpleSpriteModel(10, new int[]{5, 0, 0, 0, 0, 0, 0, 0},0);
        BehaviourModel turnRight = new SimpleSpriteModel(10, new int[]{0, 500, 0, 0, 0, 0, 0, 0}, 0);
        BehaviourModel stayStill = new SimpleSpriteModel(10, new int[]{0, 0, 0, 0, 0, 0, 0, 0}, 500);

        model.associateWithSprite(4);
        turnRight.associateWithSprite(6);
        stayStill.associateWithSprite(2);

        // model will always move forward
        model.updateModelStatistics(sso);
        turnRight.updateModelStatistics(sso);
        stayStill.updateModelStatistics(sso);
        // no direction yet established
        model.apply(sso, null); // should be no change
        assertEquals(sso.NPCPositions[0][0].position.x, 50.0, 0.001);
        assertEquals(sso.NPCPositions[0][0].position.y, 50.0, 0.001);
        assertEquals(sso.NPCPositions[1][0].position.x, 25.0, 0.001);
        assertEquals(sso.NPCPositions[1][0].position.y, 20.0, 0.001);

        sso.NPCPositions[0][0] = createNPCObservation(8, 6, 4, 50, 60);
        sso.NPCPositions[1][0] = createNPCObservation(11, 6, 4, 15, 10);
        sso.movablePositions[0][0] = createNPCObservation(20, 6,2, 10, 20);
        sso.fromAvatarSpritesPositions[0][0] = createNPCObservation(60, 6, 6, 0, 10);
        model.updateModelStatistics(sso);
        turnRight.updateModelStatistics(sso);
        stayStill.updateModelStatistics(sso);

        model.apply(sso, null); // should now continue in same direction
        turnRight.apply(sso, null);
        stayStill.apply(sso, null);
        assertEquals(sso.NPCPositions[0][0].position.x, 50.0, 0.001);
        assertEquals(sso.NPCPositions[0][0].position.y, 70.0, 0.001);
        assertEquals(sso.NPCPositions[1][0].position.x, 5.0, 0.001);
        assertEquals(sso.NPCPositions[1][0].position.y, 0.0, 0.001);
        assertEquals(sso.movablePositions[0][0].position.x, 10.0, 0.001);
        assertEquals(sso.movablePositions[0][0].position.y, 20.0, 0.001);
        assertEquals(sso.fromAvatarSpritesPositions[0][0].position.x, 10.0, 0.001);
        assertEquals(sso.fromAvatarSpritesPositions[0][0].position.y, 0.0, 0.001);

        SSOModifier.constructGrid(sso);
        // should put sprites in correct place
        assertEquals(sso.observationGrid[0][0][0].obsID, 11);
        assertEquals(sso.observationGrid[0][0][0].position.x, 5.0, 0.1);
        assertEquals(sso.observationGrid[0][0][0].position.y, 0.0, 0.1);
        assertEquals(sso.observationGrid[0][0].length, 1);
        assertEquals(sso.observationGrid[1][0].length, 2);
        assertEquals(sso.observationGrid[1][0][0].obsID, 11);
        assertEquals(sso.observationGrid[1][0][0].position.x, 5.0, 0.1);
        assertEquals(sso.observationGrid[1][0][0].position.y, 0.0, 0.1);
        assertEquals(sso.observationGrid[1][0][1].obsID, 60);
        assertEquals(sso.observationGrid[1][0][1].position.x, 10.0, 0.1);
        assertEquals(sso.observationGrid[1][0][1].position.y, 0.0, 0.1);
        assertEquals(sso.observationGrid[0][1].length, 0);
        assertEquals(sso.observationGrid[1][1].length, 0);
        assertEquals(sso.observationGrid[5][7][0].obsID, 8);
        assertEquals(sso.observationGrid[5][7][0].position.x, 50.0, 0.1);
        assertEquals(sso.observationGrid[5][7][0].position.y, 70.0, 0.1);
        // and check the surrounding 8 spaces are empty
        assertEquals(sso.observationGrid[4][6].length, 0);
        assertEquals(sso.observationGrid[4][7].length, 0);
        assertEquals(sso.observationGrid[4][8].length, 0);
        assertEquals(sso.observationGrid[5][6].length, 0);
        assertEquals(sso.observationGrid[5][8].length, 0);
        assertEquals(sso.observationGrid[6][6].length, 0);
        assertEquals(sso.observationGrid[6][7].length, 0);
        assertEquals(sso.observationGrid[6][8].length, 0);
    }

    private Observation createNPCObservation(int id, int category, int type, double x, double y) {
        return SSOModifier.createNPCObservation(id, category, type, x, y);
    }

}