package hopshackle1;

import java.util.*;

public class TupleDataBankWithPrioritisedSweeping extends TupleDataBank {

    private Map<SARTuple, Double> data = new HashMap();
    private PriorityQueue<SARTuple> queue = new PriorityQueue(500, new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            double v1 = data.getOrDefault(o1, Double.MIN_VALUE);
            double v2 = data.getOrDefault(o2, Double.MIN_VALUE);
            if (v1 < v2) return 1;
            if (v2 < v1) return -1;
            return 0;
            // PriorityQueue returns the smallest element at the head, and we want this one
            // to have the highest priority. Hence we say large v1 is in the correct order
        }
    });


    /*
    Unlike TupleDataBank, we do not have a limit on the Tuples we store
     */
    public TupleDataBankWithPrioritisedSweeping(double threshold) {
        super(Integer.MAX_VALUE, threshold);
    }

    @Override
    public void addData(List<SARTuple> newData) {
        // when adding a new tuple, we default priority to very high
        for (SARTuple d : newData) {
            data.put(d, 100.0);
            queue.add(d);
        }
    }

@Override
    protected SARTuple getTuple() {
        return queue.poll();
    }

    @Override
    protected void updateTuple(SARTuple tuple, double delta) {
        if (Math.abs(delta) > PRIORITISATION_THRESHOLD) {
            SARTuple predecessor = tuple.getPredecessor();
            if (predecessor != null && data.containsKey(predecessor)) {
                double currentPredecessorPriority = data.get(predecessor);
                queue.remove(predecessor);
                data.put(predecessor, Math.max(delta, currentPredecessorPriority));
                queue.add(predecessor);
            }
            data.put(tuple, delta);
            queue.add(tuple);
        } else {
            // remove from databank
            data.remove(tuple);
        }
    }

    @Override
    protected int getDataSize() {
        return queue.size();
    }

    @Override
    public List<SARTuple> getAllData() {
        return HopshackleUtilities.convertSetToList(data.keySet());
    }

}
