package hopshackle1.RL;

import hopshackle1.*;

public interface Trainable {

    public void learnFrom(SARTuple sarTuple, ReinforcementLearningAlgorithm rl);
}
