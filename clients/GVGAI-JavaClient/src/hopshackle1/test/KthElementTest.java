package hopshackle1.test;

import hopshackle1.HopshackleUtilities;
import org.junit.*;
import static org.junit.Assert.*;
import org.javatuples.*;
import serialization.*;

public class KthElementTest {

    @Test
    public void kthElementCorrectOnDouble() {
        double[] testArray = {0.01, 4.5, 2934.9, 3.0, 3.0, 3, 4, 5, 2, 98.0, 3.274733, 3.27, 1e10, -3e-9};
        assertEquals(HopshackleUtilities.findKthValueIn(testArray, 1), -3e-9, 1e-10);
        assertEquals(HopshackleUtilities.findKthValueIn(testArray, 2), 0.01, 1e-11);
        assertEquals(HopshackleUtilities.findKthValueIn(testArray, 3), 2, 1e-4);
        assertEquals(HopshackleUtilities.findKthValueIn(testArray, 4), 3, 1e-4);
        assertEquals(HopshackleUtilities.findKthValueIn(testArray, 5), 3, 1e-4);
        assertEquals(HopshackleUtilities.findKthValueIn(testArray, 8), 3.274733, 1e-6);

        assertEquals(testArray[0], 0.01, 1e-10);
        assertEquals(testArray[1], 4.5, 1e-11);
        assertEquals(testArray[2], 2934.9, 1e-4);
    }

    @Test
    public void directionOfTest() {
        Pair<Double, Integer> result = HopshackleUtilities.directionOf(new Vector2d(0, -10));
        assertEquals(result.getValue1(), 0, 0);
        assertEquals(result.getValue0(), 0.0, 0.001);
        result = HopshackleUtilities.directionOf(new Vector2d(10, -10));
        assertEquals(result.getValue1(), 1, 0);
        assertEquals(result.getValue0(), Math.PI / 4.0, 0.001);
        result = HopshackleUtilities.directionOf(new Vector2d(10, 0));
        assertEquals(result.getValue1(), 2, 0);
        assertEquals(result.getValue0(), Math.PI / 2.0, 0.001);
        result = HopshackleUtilities.directionOf(new Vector2d(10, 10));
        assertEquals(result.getValue1(), 3, 0);
        assertEquals(result.getValue0(), 3.0 * Math.PI / 4.0, 0.001);
        result = HopshackleUtilities.directionOf(new Vector2d(0, 10));
        assertEquals(result.getValue1(), 4, 0);
        assertEquals(result.getValue0(), Math.PI, 0.001);
        result = HopshackleUtilities.directionOf(new Vector2d(-10, 10));
        assertEquals(result.getValue1(), 5, 0);
        assertEquals(result.getValue0(), 5.0 * Math.PI / 4.0, 0.001);
        result = HopshackleUtilities.directionOf(new Vector2d(-10, 0));
        assertEquals(result.getValue1(), 6, 0);
        assertEquals(result.getValue0(), 6.0 * Math.PI / 4.0, 0.001);
        result = HopshackleUtilities.directionOf(new Vector2d(-10, -10));
        assertEquals(result.getValue1(), 7, 0);
        assertEquals(result.getValue0(), 7.0 * Math.PI / 4.0, 0.001);
        result = HopshackleUtilities.directionOf(new Vector2d(-1, -10));
        assertEquals(result.getValue1(), 0, 0);
        assertEquals(result.getValue0(), 2.0 * Math.PI, 0.1);
    }
}
