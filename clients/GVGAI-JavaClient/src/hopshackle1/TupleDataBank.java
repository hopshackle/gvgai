package hopshackle1;

import hopshackle1.RL.*;

import java.util.*;

import org.javatuples.*;

public class TupleDataBank {

    protected List<SARTuple> data = new ArrayList();
    private final int tupleLimit;
    private Random rnd = new Random();
    protected boolean debug = false;
    protected double PRIORITISATION_THRESHOLD;
    protected EntityLog logFile;

    public TupleDataBank(int limit, double threshold) {
        if (debug) logFile = new EntityLog("TupleDataBank");
        tupleLimit = limit;
        PRIORITISATION_THRESHOLD = threshold;
    }

    public void addData(List<SARTuple> newData) {
        if (debug) {
            logFile.log("\nStarting new trajectory: ");
            for (SARTuple tuple : newData) {
                logFile.log(tuple.toString());
            }
        }
        int historicTuplesToUse = Math.max(tupleLimit - newData.size(), 0);
        if (data.size() > historicTuplesToUse) {
            data = HopshackleUtilities.cloneList(data.subList(data.size() - historicTuplesToUse, data.size()));
        }
        data.addAll(newData);
    }

    public List<SARTuple> getAllData() {
        return data;
    }

    protected SARTuple getTuple() {
        int roll = rnd.nextInt(data.size());
        return data.get(roll);
    }

    protected void updateTuple(SARTuple tuple, double delta) {
        if (data.size() > 100 && Math.abs(delta) < PRIORITISATION_THRESHOLD) {
            data.remove(tuple);
        }
    }

    protected int getDataSize() {
        return data.size();
    }

    public int teach(Trainable fa, int milliseconds, ReinforcementLearningAlgorithm rl) {
        int startingTuples = getDataSize();
        if (startingTuples == 0) return 0;
        int tuplesUsed = 0;
        long startTime = System.currentTimeMillis();
        do {
            tuplesUsed++;
            SARTuple tuple = getTuple();
            double delta = fa.learnFrom(tuple, rl);
            tuple.process();
            updateTuple(tuple, delta);

        } while (System.currentTimeMillis() < startTime + milliseconds && getDataSize() > 0);

        int tuplesFiltered = startingTuples - getDataSize();
        if (debug) logFile.log(String.format("%d tuples of %d used in training in %d ms (%d removed)",
                tuplesUsed, getDataSize(), System.currentTimeMillis() - startTime, tuplesFiltered));
        return tuplesUsed;
    }

}
