package hopshackle1;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import org.javatuples.*;
import serialization.*;

public class HopshackleUtilities {

    protected static Logger logger = Logger.getLogger("hopshackle.simulation");
    public static String newline = System.getProperty("line.separator");

    public static <T> List<T> cloneList(Collection<T> listToClone) {
        List<T> retValue = new ArrayList<T>();
        if (listToClone == null) return retValue;
        for (T item : listToClone) {
            retValue.add(item);
        }
        return retValue;
    }

    public static <T, V> Map<T, V> cloneMap(Map<T, V> mapToClone) {
        Map<T, V> retValue = new HashMap<T, V>();
        for (T key : mapToClone.keySet()) {
            retValue.put(key, mapToClone.get(key));
        }
        return retValue;
    }

    public static <T> T[][] cloneAndAddRows(T[][] arrayToClone, int rowsToAdd) {
        T[][] retValue = Arrays.copyOf(arrayToClone, arrayToClone.length + rowsToAdd);
        for (int i = 0; i < arrayToClone.length; i++) {
            if (arrayToClone[i] != null)
                retValue[i] = Arrays.copyOf(arrayToClone[i], arrayToClone[i].length);
        }
        return retValue;
    }

    public static <T> T[][] cloneArray(T[][] arrayToClone) {
        if (arrayToClone == null) return null;
        T[][] retValue = Arrays.copyOf(arrayToClone, arrayToClone.length);
        for (int i = 0; i < arrayToClone.length; i++) {
            if (arrayToClone[i] != null)
                retValue[i] = Arrays.copyOf(arrayToClone[i], arrayToClone[i].length);
        }
        return retValue;
    }

    public static <T> T[][][] cloneArray(T[][][] arrayToClone) {
        if (arrayToClone == null) return null;
        T[][][] retValue = Arrays.copyOf(arrayToClone, arrayToClone.length);
        for (int i = 0; i < arrayToClone.length; i++) {
            if (arrayToClone[i] != null) {
                retValue[i] = Arrays.copyOf(arrayToClone[i], arrayToClone[i].length);
                for (int j = 0; j < arrayToClone[i].length; j++) {
                    if (arrayToClone[i][j] != null)
                        retValue[i][j] = Arrays.copyOf(arrayToClone[i][j], arrayToClone[i][j].length);
                }
            }
        }
        return retValue;
    }

    public static <T> List<T> listFromInstance(T instance) {
        List<T> retList = new ArrayList<T>();
        if (instance != null) retList.add(instance);
        return retList;
    }

    @SafeVarargs
    public static <T> List<T> listFromInstances(T... instances) {
        List<T> retList = new ArrayList<T>();
        for (T i : instances) {
            if (i != null && !retList.contains(i)) retList.add(i);
        }
        return retList;
    }

    public static List<String> createListFromFile(File f) {
        List<String> retList = new ArrayList<String>();
        FileInputStream fis;
        BufferedReader br;
        try {
            fis = new FileInputStream(f);
            InputStreamReader isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);

            String nStr;
            nStr = br.readLine();
            while (nStr != null) {
                retList.add(nStr);
                nStr = br.readLine();
            }

            br.close();
            fis.close();
        } catch (FileNotFoundException e) {
            logger.severe(e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            logger.severe(e.toString());
            e.printStackTrace();
        }

        return retList;
    }

    public static List<Integer> convertToIntegers(List<String> integersAsStrings) {
        List<Integer> retValue = new ArrayList<Integer>();
        for (String s : integersAsStrings) {
            if (s.equals("")) continue;
            Integer i = Integer.valueOf(s);
            retValue.add(i);
        }
        return retValue;
    }

    public static <T> List<T> convertArrayToList(T[] array) {
        List<T> retValue = new ArrayList<>();
        for (T item : array) {
            retValue.add(item);
        }
        return retValue;
    }

    public static <T> List<T> convertSetToList(Set<T> set) {
        List<T> retValue = new ArrayList<>();
        for (T item : set) {
            retValue.add(item);
        }
        return retValue;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> loadEnums(List<String> classFullNameList) {
        @SuppressWarnings("rawtypes")
        Class aeClass = null;
        List<Object> retList = new ArrayList<Object>();
        for (String aeString : classFullNameList) {
            try {
                aeClass = Class.forName(aeString);
            } catch (ClassNotFoundException e) {
                logger.severe(e.toString() + " in HopshackleUtilities.loadEnums");
            }
            if (aeClass != null)
                for (Object ae : EnumSet.allOf(aeClass)) {
                    retList.add(ae);
                }
        }
        return retList;
    }

    public static String getArgument(String[] args, int index, String defaultValue) {
        if (args == null || args.length <= index)
            return defaultValue;

        if (args[index] == null)
            return defaultValue;

        return args[index];
    }

    public static int getArgument(String[] args, int index, int defaultValue) {
        if (args == null || args.length <= index)
            return defaultValue;

        int retValue = defaultValue;
        try {
            retValue = Integer.valueOf(args[index]);
        } catch (NumberFormatException e) {
            retValue = defaultValue;
        }

        return retValue;
    }

    public static String formatArray(double[] array, String delimiter, String format) {
        StringBuilder retValue = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) retValue.append(delimiter);
            retValue.append(String.format(format, array[i]));
        }
        return retValue.toString();
    }

    public interface Formatter {
        public String format(Object o);
    }

    public static String formatList(List<?> inputList, String delimiter, Formatter prettifier) {
        StringBuilder retValue = new StringBuilder();
        if (inputList != null) {
            boolean firstItemProcessed = false;
            for (Object o : inputList) {
                if (firstItemProcessed) {
                    retValue.append(delimiter);
                } else {
                    firstItemProcessed = true;
                }
                if (prettifier == null) {
                    retValue.append(o.toString());
                } else {
                    retValue.append(prettifier.format(o));
                }
            }
        }
        return retValue.toString();
    }

    @SuppressWarnings("unchecked")
    public static <A, B> List<B> convertList(List<A> input) {
        List<B> retValue = new ArrayList<B>();
        if (input == null) return retValue;
        for (A item : input) {
            retValue.add((B) item);
        }
        return retValue;
    }

    public static double findKthValueIn(double[] inputArray, int k) {
        double[] array = inputArray.clone();
        for (int i = 0; i < k; i++) {
            double minValue = array[i];
            for (int j = i + 1; j < array.length; j++) {
                if (array[j] < minValue) {  // lower than minValue
                    minValue = array[j];
                    array[j] = array[i];
                    array[i] = minValue;
                }
            }
        }
        return array[k - 1];
    }

    public static <A extends Comparable> A findKthValueIn(A[] inputArray, int k) {
        A[] array = inputArray.clone();
        for (int i = 0; i < k; i++) {
            A minValue = array[i];
            for (int j = i + 1; j < array.length; j++) {
                if (array[j].compareTo(minValue) < 0) {  // lower than minValue
                    minValue = array[j];
                    array[j] = array[i];
                    array[i] = minValue;
                }
            }
        }
        return array[k];
    }

    public static double[] expNormalise(double[] input) {
        double[] outputs = new double[input.length];
        double maxValue = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < input.length; i++) {
            if (input[i] > maxValue) maxValue = input[i];
            outputs[i] = input[i];
        }
        for (int i = 0; i < outputs.length; i++) {
            outputs[i] -= maxValue;
            double expVal = Math.exp(outputs[i]);
            if (Double.isNaN(expVal)) {
                outputs[i] = 1.0;
                System.out.println(String.format("%.2f exponentiates to NaN", outputs[i]));
            } else {
                outputs[i] = expVal;
            }
        }
        double total = 0.0;
        for (int i = 0; i < outputs.length; i++) {
            total += outputs[i];
        }
        for (int i = 0; i < outputs.length; i++) {
            outputs[i] /= total;
        }
        if (Double.isNaN(outputs[0])) {
            throw new AssertionError("Invalid pdf");
        }
        return outputs;
    }

    public static Pair<Double, Integer> directionOf(Vector2d v) {
        double theta = (v.theta() + 5.0 * Math.PI / 2.0) ;
        // we now break this into eight Pi/4 chunks, with each centred at N, NE, E .. etc
        // We subtract Pi / 8 so that we centre each quadrant
        int rightHandTurns = ((int) ((theta + 0.125 * Math.PI) / (Math.PI / 4.0))) % 8;
        Pair<Double, Integer> retValue = new Pair(theta % (2.0 * Math.PI), rightHandTurns);
        return retValue;
    }

    public static Vector2d thetaToUnitVector(double theta) {
        double x = Math.cos(theta);
        double y = Math.sin(theta);
        return new Vector2d(x, y);
    }
}
