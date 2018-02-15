package hopshackle1.models;

import hopshackle1.HopshackleUtilities;
import serialization.*;

import java.util.*;

public class SSOModifier {

    public static SerializableStateObservation constructEmptySSO() {
        SerializableStateObservation sso = new SerializableStateObservation();
        sso.blockSize = 10;
        sso.worldDimension = new double[]{100.0, 100.0};
        sso.NPCPositions = new Observation[0][0];
        sso.movablePositions = new Observation[0][0];
        sso.immovablePositions = new Observation[0][0];
        sso.fromAvatarSpritesPositions = new Observation[0][0];
        sso.resourcesPositions = new Observation[0][0];
        sso.portalsPositions = new Observation[0][0];
        sso.avatarPosition = new double[2];
        sso.avatarOrientation = new double[0];
        sso.avatarResources = new HashMap();

        constructGrid(sso);
        return sso;
    }

    public static void constructGrid(SerializableStateObservation sso) {

        List<Observation> allExtantObs = new ArrayList();
        addAllObsToList(sso.getImmovablePositions(), allExtantObs);
        addAllObsToList(sso.getImmovablePositions(), allExtantObs);
        addAllObsToList(sso.getNPCPositions(), allExtantObs);
        addAllObsToList(sso.getPortalsPositions(), allExtantObs);
        addAllObsToList(sso.getFromAvatarSpritesPositions(), allExtantObs);

        List<Observation>[][] wipObsGrid = new ArrayList[(int) (sso.worldDimension[0] / sso.blockSize)][(int) (sso.worldDimension[1] / sso.blockSize)];
        // we need to construct a temporary grid that we can then update
        for (int i = 0; i < wipObsGrid.length; i++) {
            for (int j = 0; j < wipObsGrid[i].length; j++) {
                wipObsGrid[i][j] = new ArrayList();
            }
        }

        for (Observation obs : allExtantObs) {
            addObservationToGrid(obs, wipObsGrid, sso.blockSize);
        }
        int maxRow = 0;
        for (int i = 0; i < wipObsGrid.length; i++) {
            for (int j = 0; j < wipObsGrid[i].length; j++) {
                if (wipObsGrid[i][j].size() > maxRow)
                    maxRow = wipObsGrid[i][j].size();
            }
        }

        // now convert wipObsGrid into observationGrid[][][]
        Observation[] obsArray = new Observation[0];
        Observation[][][] newObservationGrid = new Observation[wipObsGrid.length][wipObsGrid[0].length][maxRow];
        for (int i = 0; i < wipObsGrid.length; i++) {
            for (int j = 0; j < wipObsGrid[i].length; j++) {
                newObservationGrid[i][j] = wipObsGrid[i][j].toArray(obsArray);
            }
        }
        sso.observationGrid = newObservationGrid;
    }

    private static void addAllObsToList(Observation[][] observations, List<Observation> list) {
        for (Observation[] observation : observations) {
            for (Observation observation1 : observation) {
                list.add(observation1);
            }
        }
    }

    private static void addObservationToGrid(Observation obs, List<Observation>[][] observationGrid, int blockSize) {
        Vector2d position = obs.position;
        int x = (int) position.x / blockSize;
        boolean validX = x >= 0 && x < observationGrid.length;
        boolean xPlus = (position.x % blockSize) > 0 && (x + 1 < observationGrid.length);
        int y = (int) position.y / blockSize;
        boolean validY = y >= 0 && y < observationGrid[0].length;
        boolean yPlus = (position.y % blockSize) > 0 && (y + 1 < observationGrid[0].length);

        if (validX && validY) {
            observationGrid[x][y].add(obs);
            if (xPlus)
                observationGrid[x + 1][y].add(obs);
            if (yPlus)
                observationGrid[x][y + 1].add(obs);
            if (xPlus && yPlus)
                observationGrid[x + 1][y + 1].add(obs);
        }
    }

    public static SerializableStateObservation copy(SerializableStateObservation sso) {
        SerializableStateObservation retValue = new SerializableStateObservation();
        retValue.gameScore = sso.gameScore;
        retValue.gameTick = sso.gameTick;
        retValue.gameWinner = sso.gameWinner;
        retValue.isGameOver = sso.isGameOver;
        retValue.worldDimension = Arrays.copyOf(sso.worldDimension, sso.worldDimension.length);
        retValue.blockSize = sso.blockSize;

        retValue.noOfPlayers = sso.noOfPlayers;
        retValue.avatarSpeed = sso.avatarSpeed;
        retValue.avatarOrientation = Arrays.copyOf(sso.avatarOrientation, sso.avatarOrientation.length);
        retValue.avatarPosition = Arrays.copyOf(sso.avatarPosition, sso.avatarOrientation.length);
        retValue.avatarLastAction = sso.avatarLastAction;
        retValue.avatarType = sso.avatarType;
        retValue.avatarHealthPoints = sso.avatarHealthPoints;
        retValue.avatarMaxHealthPoints = sso.avatarMaxHealthPoints;
        retValue.avatarLimitHealthPoints = sso.avatarLimitHealthPoints;
        retValue.isAvatarAlive = sso.isAvatarAlive;
        retValue.availableActions = (ArrayList) HopshackleUtilities.cloneList(sso.availableActions);
        retValue.avatarResources = (HashMap) HopshackleUtilities.cloneMap(sso.avatarResources);

        retValue.observationGrid = HopshackleUtilities.cloneArray(sso.observationGrid);
        retValue.immovablePositions = HopshackleUtilities.cloneArray(sso.immovablePositions);
        retValue.movablePositions = HopshackleUtilities.cloneArray(sso.movablePositions);
        retValue.resourcesPositions = HopshackleUtilities.cloneArray(sso.resourcesPositions);
        retValue.portalsPositions = HopshackleUtilities.cloneArray(sso.portalsPositions);
        retValue.fromAvatarSpritesPositions = HopshackleUtilities.cloneArray(sso.fromAvatarSpritesPositions);
        retValue.NPCPositions = HopshackleUtilities.cloneArray(sso.NPCPositions);

        return retValue;
    }

    public static Observation createNPCObservation(int id, int category, int type, double x, double y) {
        Observation retValue = new Observation();
        retValue.category = category;
        retValue.obsID = id;
        retValue.itype = type;
        Vector2d position = new Vector2d(x, y);
        retValue.position = position;
        return retValue;
    }
}
