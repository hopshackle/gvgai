package hopshackle1;

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
    public State last_state_;
    public Types.ACTIONS last_action_;
    public double last_score_;
    public Random rdm_;
    public GameSelector game_selector_;
    public int current_level_;
    public int game_plays_;

    private FeatureSet featureSet = new AvatarMeshFeatureSet();
    private List<LinkedList<SARTuple>> historicTrajectories = new ArrayList<>();
    private LinkedList<SARTuple> currentTrajectory;

    private Policy policy = new Policy();

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
        ArrayList<Types.ACTIONS> actions = sso.getAvailableActions();
        currentTrajectory = new LinkedList<>();
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
        State state = extractFeature(sso);
        double new_score = sso.gameScore;
        double reward = calculateReward(sso);

        if (last_state_ != null) {
            currentTrajectory.add(new SARTuple(last_state_, last_action_, reward));
        }

        Types.ACTIONS action = applyPolicy(sso, state);

        last_score_ = new_score;
        last_action_ = action;
        last_state_ = state;
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
        double new_score = sso.gameScore;
        double reward = calculateReward(sso);
        currentTrajectory.add(new SARTuple(last_state_, last_action_, reward));

        historicTrajectories.add(currentTrajectory);

        game_plays_++;

        game_selector_.addScore(current_level_, reward);

        doLearnin();

        if (game_plays_ < 3) {
            current_level_++;
            assert (game_plays_ == current_level_);
        } else {
            current_level_ = game_selector_.selectLevel();
        }
        return current_level_;
    }

    /**
     * Extract features and create state
     *
     * @param sso
     */
    public State extractFeature(SerializableStateObservation sso) {
        State state = featureSet.describeObservation(sso);
        return state;
    }

    public double calculateReward(SerializableStateObservation sso) {
        double new_score = sso.gameScore;
        if (sso.isGameOver) {
            if (sso.gameWinner == Types.WINNER.PLAYER_WINS) {
                new_score += 1000;
            } else if (sso.gameWinner == Types.WINNER.PLAYER_LOSES) {
                new_score -= 1000;
            }
        }
        return new_score - last_score_;
    }


    public void doLearnin() {
        // TODO: do tha learnin stuff
    }

    public Types.ACTIONS applyPolicy(SerializableStateObservation sso, State state) {
        return policy.chooseAction(sso.getAvailableActions(), state);
    }
}
