package hopshackle1.Policies;

import java.util.*;

import hopshackle1.*;
import hopshackle1.FeatureSets.FeatureSet;
import hopshackle1.RL.*;
import hopshackle1.models.GameStatusTracker;
import serialization.*;
import serialization.Types.*;

public class Reinforce implements Policy, Trainable {

    private Random rdm = new Random(45);
    private IndependentLinearActionValue coeffs;
    private double alpha, lambda, gamma;
    private boolean debug = true;
    private boolean fullDebug = false;      // includes detail of pi gradient updates
    private boolean useValueFunctionAsBaseline = false;
    private boolean trainValue = false; // TODO: This should really be split out as a separate Value function
    private double temperature = 1.0;
    private BoltzmannPolicy choicePolicy;
    private EntityLog logFile = new EntityLog("REINFORCE");

    public Reinforce(double learningRate, double regularisation, double discountRate, List<FeatureSet> featureSets) {
        alpha = learningRate;
        lambda = regularisation;
        gamma = discountRate;
        coeffs = new IndependentLinearActionValue(featureSets, gamma, debug);
        choicePolicy = new BoltzmannPolicy(coeffs, temperature);
    }

    @Override
    public double[] pdfOver(List<ACTIONS> availableActions, GameStatusTracker gst) {
        return choicePolicy.pdfOver(availableActions, gst);
    }

    @Override
    public ACTIONS chooseAction(List<ACTIONS> availableActions, GameStatusTracker gst, int timeBudget) {
        return choicePolicy.chooseAction(availableActions, gst, timeBudget);
    }

    @Override
    public double learnFrom(SARTuple tuple, ReinforcementLearningAlgorithm rl) {
        double[] actionPDF = choicePolicy.pdfOver(tuple.availableStartActions, tuple.startGST);
        State startState = coeffs.calculateState(tuple.startGST.getCurrentSSO());

        int[] indicesToCoefficientsForAvailableActions = new int[tuple.availableStartActions.size()];
        for (int i = 0; i < indicesToCoefficientsForAvailableActions.length; i++) {
            indicesToCoefficientsForAvailableActions[i] = coeffs.getIndexFor(tuple.availableStartActions.get(i));
        }
        int indexToPDFForAction = tuple.availableStartActions.indexOf(tuple.action);
        double baseline = useValueFunctionAsBaseline ? coeffs.value(tuple.startGST.getCurrentSSO()) : 0.00;
        double target = tuple.rewardToEnd - baseline;       // Assumes we always have crystallised a Monte Carlo Reward

        if (fullDebug) {
            StringBuilder logMessage = new StringBuilder(String.format("R = %.2f\tV(S) = %.2f\tTupleRef: %d\tActions: ", tuple.reward, baseline, tuple.ref));
            for (int i = 0; i < actionPDF.length; i++) {
                logMessage.append(String.format("\t%s %.3f", tuple.availableStartActions.get(i).toString(), actionPDF[i]));
            }
            logFile.log(logMessage.toString() + "\n");
        }

        for (int f : startState.features.keySet()) {
            double featureValue = startState.features.get(f);
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
                if (i != 0) {
                    delLogPolicy[i] -= actionPDF[i - 1] * featureValue;
                    modifyCoeff(f, indicesToCoefficientsForAvailableActions[i - 1], alpha * delLogPolicy[i], target);
                    // always subtract probability of choice
                } else if (trainValue && i == 0) {
                    modifyCoeff(f, 0, alpha * delLogPolicy[i], target);
                }
            }

            if (fullDebug) {
                StringBuilder featureDetails = new StringBuilder(String.format("Feature %d\t Phi = %.3f VTheta = %.3f dLoss = %.3f VTheta' = %.3f\n",
                        f, startState.getFeature(f), oldCoeff[0], delLogPolicy[0], coeffs.getCoeffFor(0, f)));
                for (int i = 1; i < delLogPolicy.length; i++) {
                    featureDetails.append(String.format("\tp(a) = %.3f Theta = %.3f dLogPi = %.3f Theta' = %.3f\n",
                            actionPDF[i - 1], oldCoeff[i], delLogPolicy[i], coeffs.getCoeffFor(i, f)));
                }
                logFile.log(featureDetails.toString());
            }
        }
        if (fullDebug) logFile.flush();
        tuple.incrementCount();
        return target;
    }

    private void modifyCoeff(int f, int actionIndex, double rate, double target) {
        double currentValue = coeffs.getCoeffFor(actionIndex, f);
        double newValue = (1.0 - lambda) * (currentValue + rate * target);
        coeffs.setCoeffFor(actionIndex, f, newValue);
    }
}
