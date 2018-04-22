package hopshackle1;

import hopshackle1.RL.*;

import java.util.*;

import org.javatuples.*;

public class TupleDataBank {

    private List<SARTuple> data = new ArrayList();
    private int tupleLimit;
    private Random rnd = new Random();
    private boolean debug = false;
    private double PRIORITISATION_THRESHOLD;
    private EntityLog logFile;

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
            data = HopshackleUtilities.cloneList(data.subList(0, historicTuplesToUse));
        }
        data.addAll(newData);
    }

    public List<SARTuple> getAllData() {
        return data;
    }

    private Pair<Integer, SARTuple> getTuple() {
        int roll = rnd.nextInt(data.size());
        return new Pair(roll, data.get(roll));
    }

    public int teach(Trainable fa, int milliseconds, ReinforcementLearningAlgorithm rl) {
        if (data.isEmpty()) return 0;
        int tuplesUsed = 0, tuplesFiltered = 0, startingTuples = data.size();
        long startTime = System.currentTimeMillis();
        do {
            tuplesUsed++;
            Pair<Integer, SARTuple> tupleData = getTuple();
            double delta = fa.learnFrom(tupleData.getValue1(), rl);
            if (startingTuples > 100 && Math.abs(delta) < PRIORITISATION_THRESHOLD) {
                data.remove((int) tupleData.getValue0());
                tuplesFiltered++;
            }
        } while (System.currentTimeMillis() < startTime + milliseconds && !data.isEmpty());

        if (debug) logFile.log(String.format("%d tuples of %d used in training in %d ms (%d removed)",
                tuplesUsed, data.size(), System.currentTimeMillis() - startTime, tuplesFiltered));
        return tuplesUsed;
    }

}
