package hopshackle1.RL;

import hopshackle1.*;

public interface Trainable {

    public double learnFrom(SARTuple sarTuple, ReinforcementLearningAlgorithm rl);
}
