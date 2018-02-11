package hopshackle1;

import hopshackle1.Policies.*;
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
    public double alpha = 0.001;
    public double lambda = 0.0001;
    public double epsilon = 0.10;
    private int nStepParameter = 1;
    private double gameWinBonus = 100.0;

    private EntityLog logFile;
    private boolean debug = false;

    private FeatureSet[] featureSets = {new AvatarMeshFeatureSet(), new GlobalPopulationFeatureSet()};
    private LinkedList<SARTuple> currentTrajectory;

    private PolicyKernel policyKernel = new PolicyCoeffCoreByAction("QTheta", alpha, gamma, lambda, debug);
    private Policy policy = new BoltzmannPolicy(policyKernel, 0.1);

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
        if (debug) logFile = new EntityLog("Hopshackle1");
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
        if (debug) logFile.log("Initialising new trajectory");
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
        double new_score = sso.gameScore;
        double reward = calculateReward(sso);

        if (last_state_ != null) {
            SARTuple tuple = new SARTuple(last_state_, sso, last_action_, lastAvailableActions, sso.availableActions, reward);
            currentTrajectory.add(tuple);
            if (debug) {
                logFile.log(String.format("TupleRef: %d Action %s gives reward %.2f", tuple.ref, last_action_, reward));
            }
        }

        Types.ACTIONS action = policy.chooseAction(sso.getAvailableActions(), sso);
        if (debug) {
            logFile.log(String.format("Action %s taken with Avatar at %.0f/%.0f", action.toString(), sso.avatarPosition[0], sso.avatarPosition[1]));
        }

        policy.learnUntil(elapsedTimer, 30);

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
        double reward = calculateReward(sso);
        SARTuple tuple = new SARTuple(last_state_, sso, last_action_, lastAvailableActions, new ArrayList(), reward);
        currentTrajectory.add(tuple);
        if (debug) {
            logFile.log(String.format("TupleRef: %d Action %s gives reward %.2f and final score %.2f", tuple.ref, last_action_, reward, sso.gameScore));
            logFile.log("End of game.");
            logFile.flush();
        }

        last_score_ = 0.0;
        last_state_ = null;
        lastAvailableActions = null;

        game_plays_++;

        game_selector_.addScore(current_level_, reward);

        long start = elapsedTimer.elapsed();

        doLearnin();

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

    public double calculateReward(SerializableStateObservation sso) {
        double new_score = sso.gameScore;
        if (sso.isGameOver) {
            if (sso.gameWinner == Types.WINNER.PLAYER_WINS) {
                new_score += gameWinBonus;
            } else if (sso.gameWinner == Types.WINNER.PLAYER_LOSES) {
                new_score -= gameWinBonus;
            }
        }
        return new_score - last_score_;
    }


    public void doLearnin() {
        // execute a simple run of Policy Learning by gradient descent after updating rewards on trajectory
        SARTuple.chainRewardsBackwards(currentTrajectory, nStepParameter, gamma);
        policy.learnFrom(currentTrajectory);
    }
}
