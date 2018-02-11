package hopshackle1.Policies;

import hopshackle1.*;
import serialization.*;
import utils.*;
import java.util.*;

public class LookaheadQLearning implements Policy {

    private double gamma, alpha, lambda, epsilon;
    private Random rnd = new Random(56);
    private boolean debug = false;
    private double temperature = 3.0;
    private int updateCount = 0;
    private boolean boltzmannDistribution = true;
    private boolean freezeThetaForValuation = false;
    private int updatesForThetaMinus = 10000;
    private boolean monteCarloReward = false;
    private boolean nStepReward = true;
    private PolicyCoeffCoreByAction theta = new PolicyCoeffCoreByAction("QTheta", debug);
    private PolicyCoeffCoreByAction thetaMinus = new PolicyCoeffCoreByAction("QThetaMinus", debug);
    private EntityLog logFile;
    private TupleDataBank databank = new TupleDataBank(1000);

    @Override
    public void learnFrom(List<SARTuple> trajectories) {

    }

    @Override
    public void learnFrom(SARTuple tuple) {

    }

    @Override
    public Types.ACTIONS chooseAction(List<Types.ACTIONS> actions, State state) {
        return null;
    }

    @Override
    public void learnUntil(ElapsedCpuTimer cpuTimer, int milliSeconds) {

    }
}
