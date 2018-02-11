package hopshackle1.Policies;

import java.util.*;

import hopshackle1.*;
import serialization.Types.*;
import utils.ElapsedCpuTimer;

public class PolicyReinforce implements Policy {

    private Random rdm = new Random(45);
    private PolicyCoeffCoreByAction coeffs;
    private double alpha, lambda;
    private double epsilon = 0.05;
    private boolean debug = true;
    private boolean fullDebug = false;      // includes detail of pi gradient updates
    private boolean valueLeastSquares = true;
    private boolean valueReporting = false;
    private boolean usePreviousEpochsForValueTraining = false;
    private boolean meanRewardAsBaseline = true;
    private boolean useValueFunctionAsBaseline = false;
    private int featureCountForValueFunction= 100;
    private double meanReward;
    private double temperature = 1.0;
    private String winnowFeatures = "ENTROPY";
    private TupleDataBank databank = new TupleDataBank(1000);

    private EntityLog logFile = new EntityLog("Policy");
    private EntityLog pdfFile = new EntityLog("ActionChoice");

    public PolicyReinforce(double learningRate, double regularisation) {
        alpha = learningRate;
        lambda = regularisation;
        coeffs = new PolicyCoeffCoreByAction("REINFORCE", debug);
    }

    public ACTIONS chooseAction(List<ACTIONS> actions, State state) {
        double[] pdf = coeffs.getProbabilityDistributionOverActions(actions, state, temperature);
        double roll = rdm.nextDouble();
        if (debug) {
            pdfFile.log("PDF for actions is:");
            for (int i = 0; i < pdf.length; i++) {
                pdfFile.log(String.format("\t%.3f\t%s", pdf[i], actions.get(i)));
            }
        }
        double cdf = 0.0;
        for (int i = 0; i < pdf.length; i++) {
            cdf += pdf[i];
            if (roll <= cdf) {
                if (debug) pdfFile.log("Chooses action " + actions.get(i));
                return actions.get(i);
            }
        }
        throw new AssertionError("Should not be able to get here in theory. " + HopshackleUtilities.formatArray(pdf, ", ", "%.5f"));
    }

    private void regressValueFrom(List<SARTuple> trajectory) {
        // firstly we need to create an array of the data
        double[][] X = new double[trajectory.size()][State.getHighestIndex() + 1];
        double[] Y = new double[trajectory.size()];
        double[] nonZeroCounts = new double[X[0].length];
        double[] entropy = new double[X[0].length];
        List<Integer> featuresInOrder = new ArrayList(State.getHighestIndex() + 1);
        for (int i = 0; i < Y.length; i++) {
            SARTuple tuple = trajectory.get(i);
            List<Integer> features = HopshackleUtilities.convertSetToList(tuple.state.features.keySet());
            for (int j = 0; j < features.size(); j++) {
                int feature = features.get(j);
                double featureValue = tuple.state.features.get(feature);
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
                    entropy[i] = - (n / T) * Math.log(n / T) - (1.0 - n / T) * Math.log(1.0 - n / T);
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
        coeffs.clearCoeffs(0);
        // then run through each weight from regression, and insert it in correct place
        for (int i = 0; i < regressedWeights.length; i++) {
            int feature = featuresInOrder.get(i);
            coeffs.setCoeffFor(0, feature, regressedWeights[i]);
        }
    }

    /*
    Only pass in *new* trajectory data. Policy will maintain old trajectory data that it thinks might be useful.
     */
    @Override
    public void learnFrom(List<SARTuple> trajectory) {
        List<SARTuple> trajectoriesForValueTraining = new ArrayList();
        if (usePreviousEpochsForValueTraining) {
            trajectoriesForValueTraining = databank.getAllData();
        } // before we truncate, store the previous data for value training

        databank.addData(trajectory);

        List<SARTuple> trainingData = databank.getAllData();

        if (!usePreviousEpochsForValueTraining)
            trajectoriesForValueTraining = trainingData;

        meanReward = 0.0;
        if (meanRewardAsBaseline) {
            for (SARTuple tuple : trainingData) {
                meanReward += tuple.reward;
            }
        }
        meanReward /= (double) trainingData.size();
        if (debug) {
            logFile.log(String.format("Starting learning cycle with %d experience tuples and %d total features. Mean R = %.2f",
                    trainingData.size(), State.getHighestIndex() + 1, meanReward));
            logFile.flush();
        }
        long start = System.currentTimeMillis();
        double previousError = Double.POSITIVE_INFINITY;
        double newError = Double.POSITIVE_INFINITY;
        int count = 0;
        if (useValueFunctionAsBaseline && trajectoriesForValueTraining.size() > 100) {
            if (valueLeastSquares) {
                regressValueFrom(trajectoriesForValueTraining);
                reportValueDelta(trajectoriesForValueTraining);
                long end = System.currentTimeMillis();
                if (debug) logFile.log(String.format("Regression took %dms", (end - start)));
            } else {
                boolean baseDebug = debug;
                debug = false;
                do {
                    count++;
                    previousError = newError;
                    newError = 0.0;
                    if (valueReporting) {
                        reportValueDelta(trajectoriesForValueTraining);
                    }

                    for (SARTuple tuple : trajectoriesForValueTraining) {
                        double delta = learnValueFrom(tuple);
                        newError += Math.pow(delta, 2);
                    }
                    newError /= trajectoriesForValueTraining.size();
                } while (count < 100 && newError + epsilon < previousError);

                long end = System.currentTimeMillis();
                logFile.log(String.format("Value training converges after %d iterations to MSE of %.2f in %dms", count, newError, (end - start)));
                logFile.flush();

                debug = baseDebug;
            }
        }
        // now update the policy
        for (SARTuple tuple : trainingData) {
            learnPolicyFrom(tuple);
        }
    }

    private void reportValueDelta(List<SARTuple> trajectory) {
        double totalValueError = 0.0;
        double totalSquaredValueError = 0.0;
        for (SARTuple sarTuple : trajectory) {
            double value = coeffs.valueState(sarTuple.state);
            double reward = sarTuple.reward;
            totalValueError += Math.abs(value - reward);
            totalSquaredValueError += Math.pow(value - reward, 2);
        }
        logFile.log(String.format("Error on Critic value approximation MAE: %.2f MSE %.2f", totalValueError / trajectory.size(), totalSquaredValueError / trajectory.size()));
        logFile.flush();
    }

    private double learnValueFrom(SARTuple tuple) {
        return learnFrom(tuple, true, false);
    }

    private void learnPolicyFrom(SARTuple tuple) {
        learnFrom(tuple, false, true);
    }

    public void learnFrom(SARTuple tuple) {
        learnFrom(tuple, true, true);
    }

    private double learnFrom(SARTuple tuple, boolean trainValue, boolean trainPolicy) {
        int[] indicesToCoefficientsForAvailableActions = new int[tuple.availableStartActions.size()];
        for (int i = 0; i < indicesToCoefficientsForAvailableActions.length; i++) {
            indicesToCoefficientsForAvailableActions[i] = coeffs.getIndexFor(tuple.availableStartActions.get(i));
        }
        int indexToPDFForAction = tuple.availableStartActions.indexOf(tuple.action);
        double[] actionPDF = coeffs.getProbabilityDistributionOverActions(tuple.availableStartActions, tuple.state, temperature);
        double baseline = useValueFunctionAsBaseline ? coeffs.valueState(tuple.state) : meanReward;
        double target = tuple.reward - baseline;

        if (fullDebug) {
            StringBuilder logMessage = new StringBuilder(String.format("R = %.2f\tV(S) = %.2f\tTupleRef: %d\tActions: ", tuple.reward, baseline, tuple.ref));
            for (int i = 0; i < actionPDF.length; i++) {
                logMessage.append(String.format("\t%s %.3f", tuple.availableStartActions.get(i).toString(), actionPDF[i]));
            }
            logFile.log(logMessage.toString() + "\n");
        }

        for (int f : tuple.state.features.keySet()) {
            double featureValue = tuple.state.features.get(f);
            double[] oldCoeff = new double[actionPDF.length + 1];
            double[] delLogPolicy = new double[oldCoeff.length];

            if (fullDebug) {
                oldCoeff[0] = coeffs.getCoeffFor(0, f);
                for (int i : indicesToCoefficientsForAvailableActions) {
                    oldCoeff[i] = coeffs.getCoeffFor(i, f);
                }
            }

            for (int i = 0; i < delLogPolicy.length; i++) {
                delLogPolicy[i] = (i == 0 || indexToPDFForAction == (i - 1)) ? featureValue : 0.0;
                // use feature value for V(s) and Q(a) where a was chosen
                if (trainPolicy && i != 0) {
                    delLogPolicy[i] -= actionPDF[i - 1] * featureValue;
                    modifyCoeff(f, indicesToCoefficientsForAvailableActions[i - 1], alpha * delLogPolicy[i], target);
                    // always subtract probability of choice
                } else if (trainValue && i == 0) {
                    modifyCoeff(f, 0, alpha * delLogPolicy[i], target);
                }
            }

            if (fullDebug) {
                StringBuilder featureDetails = new StringBuilder(String.format("Feature %d\t Phi = %.3f VTheta = %.3f dLoss = %.3f VTheta' = %.3f\n",
                        f, tuple.state.getFeature(f), oldCoeff[0], delLogPolicy[0], coeffs.getCoeffFor(0, f)));
                if (trainPolicy) {
                    for (int i = 1; i < delLogPolicy.length; i++) {
                        featureDetails.append(String.format("\tp(a) = %.3f Theta = %.3f dLogPi = %.3f Theta' = %.3f\n",
                                actionPDF[i - 1], oldCoeff[i], delLogPolicy[i], coeffs.getCoeffFor(i, f)));
                    }
                }
                logFile.log(featureDetails.toString());
            }
        }
        if (fullDebug) logFile.flush();
        return target;
    }

    private void modifyCoeff(int f, int actionIndex, double rate, double target) {
        double currentValue = coeffs.getCoeffFor(actionIndex, f);
        double newValue = (1.0 - lambda) * (currentValue + rate * target);
        coeffs.setCoeffFor(actionIndex, f, newValue);
    }

    @Override
    public void learnUntil(ElapsedCpuTimer cpuTimer, int milliSeconds) {
        return;
    }
}
