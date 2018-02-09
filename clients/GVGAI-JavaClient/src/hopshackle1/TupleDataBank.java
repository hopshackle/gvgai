package hopshackle1;

import java.util.*;

public class TupleDataBank {

    private List<SARTuple> data = new ArrayList();
    private int tupleLimit;
    private Random rnd = new Random();

    public TupleDataBank(int limit) {
        tupleLimit = limit;
    }

    public void addData(List<SARTuple> newData) {
        List<SARTuple> shuffledData = HopshackleUtilities.cloneList(newData);
        Collections.shuffle(shuffledData);
        int historicTuplesToUse = Math.max(tupleLimit - shuffledData.size(), 0);
        if (data.size() > historicTuplesToUse) {
            data = HopshackleUtilities.cloneList(data.subList(0, historicTuplesToUse));
        }
        data.addAll(shuffledData);
    }

    public List<SARTuple> getAllData() {
        return data;
    }

    public SARTuple getTuple() {
        int roll = rnd.nextInt(data.size());
        return data.get(roll);
    }

}
