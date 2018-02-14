package hopshackle1;

import hopshackle1.RL.*;

import java.util.*;

public class TupleDataBank {

    private List<SARTuple> data = new ArrayList();
    private int tupleLimit;
    private Random rnd = new Random();
    private boolean debug = true;

    public TupleDataBank(int limit) {
        tupleLimit = limit;
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

    public SARTuple getTuple() {
        int roll = rnd.nextInt(data.size());
        return data.get(roll);
    }

    public void teach(Trainable fa, int milliseconds, ReinforcementLearningAlgorithm rl) {
        int tuplesUsed = 0;
        long startTime = System.currentTimeMillis();
        do {
            tuplesUsed++;
            fa.learnFrom(getTuple(), rl);
        } while (System.currentTimeMillis() < startTime + milliseconds);

        if (debug) System.out.println(String.format("%d tuples used in training in %d ms", tuplesUsed, System.currentTimeMillis() - startTime));
    }

}
