package hopshackle1;

import hopshackle1.FeatureSets.*;
import hopshackle1.Policies.*;
import hopshackle1.RL.*;
import hopshackle1.models.*;
import org.javatuples.*;
import serialization.*;
import serialization.Types;
import utils.ElapsedCpuTimer;

import java.util.*;

/**
 * This class has been built with a simple design in mind.
 * It is to be used to store player agent information,
 * to be later used by the client to send and receive information
 * to and from the server.
 */
public class Agent extends utils.AbstractPlayer {
    public int iter_;
    public SerializableStateObservation last_state_;
    public Types.ACTIONS last_action_;
    public List<Types.ACTIONS> lastAvailableActions;
    public double last_score_;
    public Random rdm_;
    public GameSelector game_selector_;
    public int current_level_;
    public int game_plays_;
    public double gamma = 0.95;
    public double alpha = 0.01;
    public double lambda = 0.0001;
    private double gameWinBonus = 100.0;
    private double gameLossMalus = 10.0;
    public static EntityLog logFile;
    private boolean debug = false;
    private boolean detailedPredictionDebug = false;
    private int switchOnDebugAtGame = 200;
    private ActionValueFunctionApproximator QFunction;
    private boolean firstGame = true;
    private BehaviourModel model;
    private GameStatusTrackerWithHistory gst;
    private Map<Integer, Pair<Integer, Double>> accuracyTracker;
    private Map<Integer, Integer> frequencyCount;
    private RLTargetCalculator rewardCalculator;
    private ReinforcementLearningAlgorithm rl;
    private List<FeatureSet> featureSets = new ArrayList();
    private LinkedList<SARTuple> currentTrajectory;
    private Policy policy, initialPolicy;
    private TupleDataBank databank = new TupleDataBank(500, 0.5);
    private Map<Integer, List<Pair<Double, Vector2d>>> predictions = new HashMap();
    private Map<Integer, Integer> featureSeenCount = new HashMap();
    private double rewardPerNewFeature = 10.0;
    private double tickPenalty = 0.0;
    private double defaultCoefficient = 1.0;
    private double temperature = 1.0;
    private int newFeaturesInEpisode;
    private long totalTime;
    private int tickOfLastReward, noRewardLimit = 50;

    /**
     * Public method to be called at the start of the communication. No game has been initialized yet.
     * Perform one-time setup here.
     */
    public Agent() {
        lastSsoType = Types.LEARNING_SSO_TYPE.JSON;
        this.iter_ = 0;
        this.last_state_ = null;
        this.last_action_ = null;
        this.last_score_ = 0;
        this.rdm_ = new Random();
        this.game_selector_ = new GameSelector();
        this.current_level_ = 0;
        this.game_plays_ = 0;
        logFile = new EntityLog("Hopshackle1");

        featureSets.add(new AvatarMeshWidthOneFeatureSet(2));
        //       featureSets.add(new AvatarMeshWidthThreeFeatureSet(1));
        //     featureSets.add(new GlobalPopulationFeatureSet());
        featureSets.add(new CollisionFeatures());

        //QFunction = new IndependentLinearActionValue(featureSets, gamma, debug);
        model = new BehaviouralLookaheadFunction();
        LookaheadLinearActionValue qf = new LookaheadLinearActionValue(featureSets, gamma, debug, (LookaheadFunction) model);
        qf.setDefaultFeatureCoefficient(defaultCoefficient);
        QFunction = qf;
        rewardCalculator = new QLearning(1, alpha, gamma, lambda, QFunction);
        //    rewardCalculator = new MonteCarloReward(alpha, gamma, lambda);
        initialPolicy = new BoltzmannPolicy(QFunction, temperature);
        policy = initialPolicy;
        //     policy = new MCTSPolicy(model, qf, 3.0);
        policy = new MCTSMaxPolicy(model, QFunction, 3.0, 1.0, temperature);
        rl = (QLearning) rewardCalculator;
        //   rl = new QLearning(1, alpha, gamma, lambda, QFunction);
    }


    /**
     * Public method to be called at the start of every level of a game.
     * Perform any level-entry initialization here.
     *
     * @param sso          Phase Observation of the current game.
     * @param elapsedTimer Timer (1s)
     */
    @Override
    public void init(SerializableStateObservation sso, ElapsedCpuTimer elapsedTimer) {
        currentTrajectory = new LinkedList<>();
        if (!debug && game_plays_ >= switchOnDebugAtGame) {
            debug = true;
            detailedPredictionDebug = true;
        }
        if (debug) logFile.log("Initialising new trajectory for game " + game_plays_);
        accuracyTracker = new HashMap();
        newFeaturesInEpisode = 0;
        gst = new GameStatusTrackerWithHistory();
        gst.update(sso);
        totalTime = 0;
        tickOfLastReward = 0;
        frequencyCount = new HashMap();
    }


    /**
     * Method used to determine the next move to be performed by the agent.
     * This method can be used to identify the current state of the game and all
     * relevant details, then to choose the desired course of action.
     *
     * @param sso          Observation of the current state of the game to be used in deciding
     *                     the next action to be taken by the agent.
     * @param elapsedTimer Timer (40ms)
     * @return The action to be performed by the agent.
     */
    @Override
    public Types.ACTIONS act(SerializableStateObservation sso, ElapsedCpuTimer elapsedTimer) {

        long start = System.currentTimeMillis();
        double new_score = sso.gameScore;
        updateFrequencyCount(sso.avatarPosition);
        double reward = calculateReward(sso);

        if (last_state_ != null) {
            SARTuple tuple = new SARTuple(gst, sso, last_action_, lastAvailableActions, sso.availableActions, reward);
            currentTrajectory.add(tuple);
            if (debug) {
                logFile.log(String.format("TupleRef: %d Action %s gives reward %.2f", tuple.ref, last_action_, reward));
            }
        }
        gst.update(sso);

        Policy policyToUse = policy;
        if (game_plays_ < 5) policyToUse = initialPolicy;
        QFunction.injectPolicyGuide(new RetraceDisincentive(frequencyCount));
        Types.ACTIONS action = policyToUse.chooseAction(sso.getAvailableActions(), gst, 30);
        if (debug) {
            double[] pdf = policyToUse.pdfOver(sso.getAvailableActions(), gst);
            logFile.log(String.format("Action %s taken with Avatar at %.0f/%.0f", action.toString(), sso.avatarPosition[0], sso.avatarPosition[1]));
            logFile.log("With underlying pdf:");
            for (int i = 0; i < pdf.length; i++) {
                logFile.log(String.format("\t%s\t%.2f", sso.getAvailableActions().get(i), pdf[i]));
            }
            // logFile.log(model.toString());
        }
        QFunction.injectPolicyGuide(null);

        if (!firstGame && model != null) {
            // score the result of our predictions
            if (!predictions.isEmpty()) {
                Map<Integer, Pair<Integer, Double>> accuracyBySpriteType = SSOModifier.accuracyOf(predictions, sso);
                for (Integer spriteType : accuracyBySpriteType.keySet()) {
                    Pair<Integer, Double> runningAcc = accuracyTracker.getOrDefault(spriteType, new Pair(0, 0.00));
                    int nSprite = accuracyBySpriteType.get(spriteType).getValue0();
                    double acc = accuracyBySpriteType.get(spriteType).getValue1();
                    accuracyTracker.put(spriteType, new Pair(runningAcc.getValue0() + nSprite, runningAcc.getValue1() + acc * nSprite));
                    if (debug) {
                        logFile.log(String.format("Accuracy %.2f for predictions of %d instances of %d",
                                acc, nSprite, spriteType));
                    }
                }
            }

            // then we track predictions for comparison to actual results
            predictions = new HashMap();
            List<Pair<Integer, Integer>> allSprites = SSOModifier.getAllSprites(sso,
                    new int[]{SSOModifier.TYPE_AVATAR, SSOModifier.TYPE_FROMAVATAR, SSOModifier.TYPE_MOVABLE, SSOModifier.TYPE_NPC});
            for (Pair<Integer, Integer> s : allSprites) {
                int spriteID = s.getValue0();
                Vector2d currentPosition = SSOModifier.positionOf(spriteID, sso);
                List<Pair<Double, Vector2d>> pdf = model.nextMovePdf(gst, spriteID, action);
                predictions.put(spriteID, pdf);
                if (detailedPredictionDebug) {
                    StringBuilder msg = new StringBuilder(String.format("T:%d ID:%d at %s\n", s.getValue1(), spriteID, currentPosition.toString()));
                    for (Pair<Double, Vector2d> option : pdf) {
                        msg.append(String.format("\t%.2f\t%s", option.getValue0(), option.getValue1().toString()));
                    }
                    logFile.log(msg.toString());
                }
            }
        }

        long end = System.currentTimeMillis();
        totalTime += (end - start);

        Trainable thingToTrain = null;
        if (QFunction instanceof Trainable)
            thingToTrain = (Trainable) QFunction;
        else if (policy instanceof Trainable)
            thingToTrain = (Trainable) policy;

        int remainingTime = (int) elapsedTimer.remainingTimeMillis();
        if (thingToTrain != null) {
            databank.teach(thingToTrain, 20, rl);
        } else {
            logFile.log("Nothing to train....");
        }
        if (sso.gameTick - tickOfLastReward > noRewardLimit + game_plays_ * 10)
            action = Types.ACTIONS.ACTION_ESCAPE;

        last_score_ = new_score;
        last_action_ = action;
        lastAvailableActions = sso.availableActions;
        last_state_ = sso;
        iter_++;

        return action;
    }

    /**
     * Method used to perform actions in case of a game end.
     * This is the last thing called when a level is played (the game is already in a terminal state).
     * Use this for actions such as teardown or process data.
     *
     * @param sso          The current state observation of the game.
     * @param elapsedTimer Timer (up to CompetitionParameters.TOTAL_LEARNING_TIME
     *                     or CompetitionParameters.EXTRA_LEARNING_TIME if current global time is beyond TOTAL_LEARNING_TIME)
     * @return The next level of the current game to be played.
     * The level is bound in the range of [0,2]. If the input is any different, then the level
     * chosen will be ignored, and the game will play a sampleRandom one instead.
     */
    @Override
    public int result(SerializableStateObservation sso, ElapsedCpuTimer elapsedTimer) {

        // add final state to trajectory
        updateFrequencyCount(sso.avatarPosition);
        double reward = calculateReward(sso);
        if (sso.gameTick == last_state_.gameTick) sso.gameTick++;
        SARTuple tuple = new SARTuple(gst, sso, last_action_, lastAvailableActions, new ArrayList(), reward);
        currentTrajectory.add(tuple);
        if (debug) {
            logFile.log(String.format("TupleRef: %d Action %s gives reward %.2f and final score %.2f", tuple.ref, last_action_, reward, sso.gameScore));
            logFile.log(String.format("End of game %s, with processing taking %.2f seconds for %d moves (%.0f ms per move)",
                    game_plays_, totalTime / 1000.0, sso.gameTick, (sso.gameTick > 0) ? totalTime / sso.gameTick : 0.0));
        }

        gst.update(sso);
        if (firstGame) {
            firstGame = false;
        }

        Map<Types.ACTIONS, Integer> actionCounts = new HashMap();
        for (SARTuple exp : currentTrajectory) {
            int oldCount = actionCounts.getOrDefault(exp.action, 0);
            actionCounts.put(exp.action, oldCount + 1);
        }
        StringBuilder msg = new StringBuilder("Total action counts in game:\n");
        for (Types.ACTIONS action : actionCounts.keySet()) {
            msg.append(String.format("\t%s\t%d\t(%.0f%%)\n", action.toString(), actionCounts.get(action),
                    100.0 * (double) actionCounts.get(action) / (double) currentTrajectory.size()));
        }
        logFile.log(msg.toString());
        msg = new StringBuilder("Accuracy of Model over game:\n");
        for (Integer spriteType : accuracyTracker.keySet()) {
            Pair<Integer, Double> results = accuracyTracker.get(spriteType);
            msg.append(String.format("\tSprite Type: %s\t%.0f%%\n", spriteType, 100.0 * results.getValue1() / results.getValue0()));
        }
        logFile.log(msg.toString());

        if (model != null) {
            model.updateModelStatistics(gst);
            if (debug) logFile.log("New model after processing:\n" + model.toString() + "\n");
        }

        logFile.log("Coefficients after game:");
        logFile.log(QFunction.toString());
        logFile.log("Total new features in episode: " + newFeaturesInEpisode);
        logFile.log(String.format("Databank size %d, with %d new tuples to be added\n", databank.getAllData().size(), currentTrajectory.size()));
        logFile.flush();

        last_score_ = 0.0;
        last_state_ = null;
        lastAvailableActions = null;

        game_plays_++;

        game_selector_.addScore(current_level_, reward);

        long start = elapsedTimer.elapsed();

        processNewTrajectory();

        long end = elapsedTimer.elapsed();

        if (debug) logFile.log("Learning takes " + ((end - start) / 1000000) + "ms");

        if (game_plays_ < 3) {
            current_level_++;
            assert (game_plays_ == current_level_);
        } else {
            current_level_ = game_selector_.selectLevel();
        }
        return current_level_;
    }

    private void updateFrequencyCount(double[] avatarPosition) {
        int key = (int)(avatarPosition[0] * 307 + avatarPosition[1]);
        int visits = frequencyCount.getOrDefault(key, 0);
        frequencyCount.put(key, visits + 1);
    }


    public double calculateReward(SerializableStateObservation sso) {
        double explorationReward = 0.0;
        if (rewardPerNewFeature > 0.00) {
            int newFeatures = 0;
            State state = QFunction.calculateState(sso);
            for (Integer f : state.features.keySet()) {
                if (!featureSeenCount.containsKey(f)) {
                    newFeatures++;
                    featureSeenCount.put(f, 1);
                    newFeaturesInEpisode++;
                }
            }
            explorationReward = Math.log(newFeatures + 1.0) * rewardPerNewFeature;
        }
        double new_score = sso.gameScore;
        new_score -= tickPenalty;
        if (sso.isGameOver) {
            if (sso.gameWinner == Types.WINNER.PLAYER_WINS) {
                new_score += gameWinBonus;
            } else if (sso.gameWinner == Types.WINNER.PLAYER_LOSES) {
                new_score -= gameLossMalus;
            }
        }
        if (new_score - last_score_ + explorationReward > 0.00) {
            tickOfLastReward = sso.gameTick;
        }
        return new_score - last_score_ + explorationReward; // - repetitionPenalty;
    }


    public void processNewTrajectory() {
        // execute a simple run of Policy Learning by gradient descent after updating rewards on trajectory
        rewardCalculator.crystalliseRewards(currentTrajectory);
        databank.addData(currentTrajectory);
    }
}
