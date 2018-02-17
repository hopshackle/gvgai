package hopshackle1.test;


import hopshackle1.HopshackleUtilities;
import hopshackle1.models.SSOModifier;
import org.junit.*;

import static org.junit.Assert.*;

import serialization.*;

public class ArrayCloneTest {

    @Test
    public void clone2DArray() {
        Observation[][] startArray = new Observation[2][3];
        startArray[0][0] = SSOModifier.createObservation(1, 3, 3, 4.5, 6.7);
        startArray[0][1] = SSOModifier.createObservation(2, 3, 3, 4.5, 6.7);
        startArray[0][2] = SSOModifier.createObservation(3, 3, 3, 4.5, 6.7);
        startArray[1][0] = SSOModifier.createObservation(4, 3, 3, 4.5, 6.7);
        startArray[1][1] = SSOModifier.createObservation(5, 3, 3, 4.5, 6.7);
        startArray[1][2] = SSOModifier.createObservation(6, 3, 3, 4.5, 6.7);

        Observation[][] newArray = HopshackleUtilities.cloneArray(startArray);
        assertFalse(newArray[0] == startArray[0]);
        assertFalse(newArray[1] == startArray[1]);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                assertTrue(newArray[i][j] == startArray[i][j]);
            }
        }
    }

    @Test
    public void cloneSparse3DArray() {
        Observation[][][] startArray = new Observation[2][3][];
        startArray[0][0] = new Observation[2];
        startArray[0][1] = new Observation[1];
        startArray[0][2] = new Observation[1];
        startArray[1][1] = new Observation[3];
        startArray[1][2] = new Observation[1];

        startArray[0][0][1] = SSOModifier.createObservation(1, 2, 3, 4, 4);
        startArray[1][2][0] = SSOModifier.createObservation(2, 2, 3, 4, 4);

        Observation[][][] newArray = HopshackleUtilities.cloneArray(startArray);
        assertFalse(newArray[0] == startArray[0]);
        assertFalse(newArray[1] == startArray[1]);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                assertTrue(startArray[i][j] == null || newArray[i][j] != startArray[i][j]);
            }
        }

        assertTrue(startArray[0][0][1] == newArray[0][0][1]);
        assertTrue(startArray[1][2][0] == newArray[1][2][0]);
        assertTrue(newArray[0][0][0] == null);
    }
}
