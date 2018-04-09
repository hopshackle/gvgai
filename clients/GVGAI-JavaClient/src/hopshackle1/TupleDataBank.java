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

    public TupleDataBank(int limit, double threshold) {
        tupleLimit = limit;
        PRIORITISATION_THRESHOLD = threshold;
    }

    public void addData(List<SARTuple> newData) {
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

    public void teach(Trainable fa, int milliseconds, ReinforcementLearningAlgorithm rl) {
        if (data.isEmpty()) return;
        int tuplesUsed = 0, tuplesFiltered = 0;
        long startTime = System.currentTimeMillis();
        do {
            tuplesUsed++;
            Pair<Integer, SARTuple> tupleData = getTuple();
            double delta = fa.learnFrom(tupleData.getValue1(), rl);
            if (Math.abs(delta) < PRIORITISATION_THRESHOLD) {
                data.remove((int) tupleData.getValue0());
                tuplesFiltered++;
            }
        } while (System.currentTimeMillis() < startTime + milliseconds);

        if (debug) System.out.println(String.format("%d tuples of %d used in training in %d ms (%d removed)",
                tuplesUsed, data.size(), System.currentTimeMillis() - startTime, tuplesFiltered));
    }

}
