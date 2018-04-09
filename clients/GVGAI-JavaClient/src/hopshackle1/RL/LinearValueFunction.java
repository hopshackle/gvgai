package hopshackle1.RL;

import hopshackle1.*;
import hopshackle1.FeatureSets.FeatureSet;
import hopshackle1.models.GameStatusTracker;
import serialization.*;
import serialization.Types.*;
import java.util.*;

public class LinearValueFunction implements ActionValueFunctionApproximator, Trainable {

    private IndependentLinearActionValue underlying;
    private String winnowFeatures;
    private int featureCountForValueFunction;
    private boolean debug;
    private EntityLog logFile = new EntityLog("LinearValueFunction");

    public LinearValueFunction(List<FeatureSet> features, String winnow, int featureCount, double discountRate, boolean debug) {
        underlying = new IndependentLinearActionValue(features, discountRate, debug);
        winnowFeatures = winnow;
        featureCountForValueFunction = featureCount;
        this.debug = debug;
    }

    private LinearValueFunction() {
    }

    @Override
    public LinearValueFunction copy() {
        LinearValueFunction retValue = new LinearValueFunction();
        retValue.underlying = underlying.copy();
        retValue.winnowFeatures = winnowFeatures;
        retValue.featureCountForValueFunction = featureCountForValueFunction;
        retValue.debug = debug;
        return retValue;
    }

    @Override
    public State calculateState(SerializableStateObservation sso) {
        return underlying.calculateState(sso);
    }

    @Override
    public double value(GameStatusTracker gst, ACTIONS a) {
        throw new AssertionError("No point asking for an Action Value from a State Value function");
    }

    @Override
    public double value(SerializableStateObservation sso) {
        return underlying.value(sso);
    }

    @Override
    public ActionValue valueOfBestAction(GameStatusTracker gst, List<ACTIONS> actions) {
        throw new AssertionError("No point asking for an Action Value from a State Value function");
    }

    @Override
    public double learnFrom(SARTuple tuple, ReinforcementLearningAlgorithm rl) {
        return underlying.learnValueFrom(tuple, rl);
    }


    public void regressValueFrom(List<SARTuple> trajectory) {
        // firstly we need to create an array of the data
        double[][] X = new double[trajectory.size()][State.getHighestIndex() + 1];
        double[] Y = new double[trajectory.size()];
        double[] nonZeroCounts = new double[X[0].length];
        double[] entropy = new double[X[0].length];
        List<Integer> featuresInOrder = new ArrayList(State.getHighestIndex() + 1);
        for (int i = 0; i < Y.length; i++) {
            SARTuple tuple = trajectory.get(i);
            State state = calculateState(tuple.startGST.getCurrentSSO());
            List<Integer> features = HopshackleUtilities.convertSetToList(state.features.keySet());
            for (int j = 0; j < features.size(); j++) {
                int feature = features.get(j);
                double featureValue = state.features.get(feature);
                X[i][State.featureToIndexMap.get(feature)] = featureValue;
                nonZeroCounts[State.featureToIndexMap.get(feature)] += 1.0; // we only have non-zero features in the Map!
            }
            Y[i] = tuple.reward;
        }

        boolean winnow = false;
        boolean reverseWinnowOrder = true;
        boolean winnowFeaturesWithEntropy = false;
        String winnowSwitch = winnowFeatures;
        if (winnowFeatures.startsWith("-")) {
            winnowSwitch = winnowFeatures.substring(1);
            reverseWinnowOrder = false;
        }
        switch (winnowSwitch) {
            case "NONE":
                break;
            case "ENTROPY":
                winnowFeaturesWithEntropy = true;
            case "DENSITY":
                winnow = true;
                break;
            default:
                throw new AssertionError("Invalid value for winnowFeatures: " + winnowFeatures);
        }
        if (winnow) {
            // nonZeroCounts now holds the number of each feature that are non-zero
            double T = trajectory.size();
            for (int i = 0; i < nonZeroCounts.length; i++) {
                double n = nonZeroCounts[i];
                if (n == T || n == 0) {
                    entropy[i] = 0;
                } else {
                    entropy[i] = -(n / T) * Math.log(n / T) - (1.0 - n / T) * Math.log(1.0 - n / T);
                }
            }
            // large negative values for entropy indicate 50:50 split (ish) between populated and non-populated
            // we either use entropy, or density (opposite of sparseness) for winnowing features

            double[] arrayForWinnowing = winnowFeaturesWithEntropy ? entropy : nonZeroCounts;
            if (reverseWinnowOrder) {
                for (int i = 0; i < arrayForWinnowing.length; i++) {
                    arrayForWinnowing[i] = -arrayForWinnowing[i];
                }
            }
            double kthValue = HopshackleUtilities.findKthValueIn(arrayForWinnowing, featureCountForValueFunction);
            featuresInOrder = new ArrayList(featureCountForValueFunction);
            int numberOfFeatures = 0;
            int numberOfSeenFeatures = 0;
            for (int i = 0; i < arrayForWinnowing.length; i++) {
                if (nonZeroCounts[i] == 0) {
                    // in this case, we have not seen the feature at all in the training set, so we should ignore it
                    continue;
                }
                numberOfSeenFeatures++;
                if (arrayForWinnowing[i] < kthValue) {
                    featuresInOrder.add(State.indexToFeatureMap.get(i));
                    numberOfFeatures++;
                }
            }

            double[][] reducedX = new double[X.length][numberOfFeatures];
            for (int i = 0; i < reducedX.length; i++) {
                for (int j = 0; j < numberOfFeatures; j++) {
                    int feature = featuresInOrder.get(j);
                    reducedX[i][j] = X[i][State.featureToIndexMap.get(feature)];
                }
            }
            if (debug) {
                logFile.log(String.format("Regressing using %d of %d features with %s %s than %.2f",
                        numberOfFeatures, numberOfSeenFeatures, winnowFeatures,
                        reverseWinnowOrder ? "greater" : "less",
                        reverseWinnowOrder ? -kthValue : kthValue));
                logFile.flush();
            }
            X = reducedX;
        } else {
            for (int i = 0; i <= State.getHighestIndex(); i++) {
                featuresInOrder.add(State.indexToFeatureMap.get(i));
            }
            // X, Y are otherwise fine.
        }
        if (X[0].length == 0) {
            if (debug) logFile.log("No features meet criteria, keeping previous value function");
            return;
        }
        LinearRegression regressor = new LinearRegression(X, Y);
        // Now need to translate weights back
        // first blank out previous estimator (TODO: calculate this as an exponential moving average)
        double[] regressedWeights = regressor.getWeights();
        underlying.clearCoeffs(0);
        // then run through each weight from regression, and insert it in correct place
        for (int i = 0; i < regressedWeights.length; i++) {
            int feature = featuresInOrder.get(i);
            underlying.setCoeffFor(0, feature, regressedWeights[i]);
        }
    }

    public void setCoeffFor(int feature, double weight) {
        underlying.setCoeffFor(0, feature, weight);
    }
    public double getCoeffFor(int feature) {
        return underlying.getCoeffFor(0, feature);
    }
}
