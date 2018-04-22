package hopshackle1.models;

import hopshackle1.HopshackleUtilities;
import serialization.*;
import org.javatuples.*;

import java.util.*;

public class SSOModifier {

    public static SerializableStateObservation constructEmptySSO() {
        SerializableStateObservation sso = new SerializableStateObservation();
        sso.blockSize = 10;
        sso.isValidation = false;
        sso.avatarLastAction = Types.ACTIONS.ACTION_NIL;
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
        addAllObsToList(sso.getMovablePositions(), allExtantObs);
        addAllObsToList(sso.getNPCPositions(), allExtantObs);
        addAllObsToList(sso.getPortalsPositions(), allExtantObs);
        addAllObsToList(sso.getFromAvatarSpritesPositions(), allExtantObs);
        Observation avatarObs = createObservation(0, 0, sso.avatarType, sso.avatarPosition[0], sso.avatarPosition[1]);
        allExtantObs.add(avatarObs);

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
        Observation[][][] newObservationGrid = new Observation[wipObsGrid.length][wipObsGrid[0].length][maxRow + 1];
        // we add one on to maxRow to enable avatar to be moved about outside of needing to rebuild the whole grid
        for (int i = 0; i < wipObsGrid.length; i++) {
            for (int j = 0; j < wipObsGrid[i].length; j++) {
                newObservationGrid[i][j] = wipObsGrid[i][j].toArray(obsArray);
            }
        }
        sso.observationGrid = newObservationGrid;
    }

    private static void addAllObsToList(Observation[][] observations, List<Observation> list) {
        if (observations != null && observations.length > 0)
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
        retValue.isValidation = sso.isValidation;

        retValue.noOfPlayers = sso.noOfPlayers;
        retValue.avatarSpeed = sso.avatarSpeed;
        retValue.avatarOrientation = Arrays.copyOf(sso.avatarOrientation, sso.avatarOrientation.length);
        retValue.avatarPosition = Arrays.copyOf(sso.avatarPosition, sso.avatarPosition.length);
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

    /*
    This adds a sprite to the relevant Observation[][] array, but does not update the ObservationGrid
    To do that call SSOModifier.constructGrid once all required Sprites have been added
    In addition, this function does not check for any duplicates on id - it assumes that the sprite is brand new
     */
    public static void addSprite(int id, int category, int type, double x, double y, SerializableStateObservation sso) {
        if (category == TYPE_AVATAR) {
            sso.avatarPosition[0] = x;
            sso.avatarPosition[1] = y;
            sso.avatarType = type;
        } else {
            Observation[][] obsArray = getObsArrayForCategory(category, sso);
            Observation obs = createObservation(id, category, type, x, y);
            int rowForSpriteType = -1;
            for (int i = 0; i < obsArray.length; i++) {
                if (obsArray[i].length > 0 && obsArray[i][0].itype == type)
                    rowForSpriteType = i;
            }
            if (rowForSpriteType == -1) {
                // need to add one. This also means that we need to update the sso
                Observation[] newRow = new Observation[1];
                Observation[][] newObsArray = HopshackleUtilities.cloneAndAddRows(obsArray, 1);
                newObsArray[obsArray.length] = newRow;
                newRow[0] = obs;
                obsArray = newObsArray;
            } else {
                Observation[] newRow = Arrays.copyOf(obsArray[rowForSpriteType], obsArray[rowForSpriteType].length + 1);
                obsArray[rowForSpriteType] = newRow;
                newRow[newRow.length - 1] = obs;
            }
            setObsArrayForCategory(category, sso, obsArray);
        }
    }

    public static void moveSprite(int id, int category, Vector2d newPos, SerializableStateObservation sso) {
        moveSprite(id, category, newPos.x, newPos.y, sso);
    }

    public static void moveSprite(int id, int category, double x, double y, SerializableStateObservation sso) {
        if (category == TYPE_AVATAR) {
            double oldX = sso.avatarPosition[0];
            double oldY = sso.avatarPosition[1];
            sso.avatarPosition[0] = x;
            sso.avatarPosition[1] = y;
            moveSpriteOnGrid(id, sso, oldX, oldY, x, y);
            return;
        } else {
            Observation[][] obsArray = getObsArrayForCategory(category, sso);
            for (int i = 0; i < obsArray.length; i++) {
                for (int j = 0; j < obsArray[i].length; j++) {
                    if (obsArray[i][j] != null && obsArray[i][j].obsID == id) {
                        Observation currentObs = obsArray[i][j];
                        obsArray[i][j] = createObservation(id, category, currentObs.itype, x, y);
                        return;
                    }
                }
            }
        }
        throw new AssertionError(String.format("Sprite %d of category %d not found", id, category));
    }

    public static void moveSpriteOnGrid(int id, SerializableStateObservation sso, double oldX, double oldY, double newX, double newY) {
        if (sso.observationGrid != null && sso.observationGrid.length != 0) {
            int x = (int) (oldX / sso.blockSize);
            int y = (int) (oldY / sso.blockSize);

            Observation obsToRemove = null;
            if (sso.observationGrid.length <= x || sso.observationGrid[x].length <= y) {
                throw new AssertionError("Whoops");
            }
            for (Observation obs : sso.observationGrid[x][y]) {
                if (obs.obsID == id) {
                    // found it
                    obsToRemove = obs;
                }
            }
            // then we pull it out
            for (int i = x - 1; i < x + 2; i++) {
                for (int j = y - 1; j < y + 2; j++) {
                    if (i < 0 || j < 0 || i >= sso.observationGrid.length || j >= sso.observationGrid[0].length)
                        continue; // off-screen
                    for (int k = 0; k < sso.observationGrid[i][j].length; k++) {
                        if (sso.observationGrid[i][j][k] == obsToRemove) {
                            Observation[] newRow = new Observation[sso.observationGrid[i][j].length - 1];
                            int count = 0;
                            for (int l = 0; l < sso.observationGrid[i][j].length; l++) {
                                if (l != k) {
                                    newRow[count] = sso.observationGrid[i][j][l];
                                    count++;
                                }
                            }
                            sso.observationGrid[i][j] = newRow;
                        }
                    }
                }
            }

            x = (int) (newX / sso.blockSize);
            boolean xPlus = (newX % sso.blockSize) > 0 && (x + 1 < sso.observationGrid.length);
            y = (int) (newY / sso.blockSize);
            boolean yPlus = (newY % sso.blockSize) > 0 && (y + 1 < sso.observationGrid[0].length);

            // and put it back in the right place
            if (x >= 0 && y >= 0 && x < sso.observationGrid.length && y < sso.observationGrid[0].length) {
                addObservationToEndOfRow(x, y, obsToRemove, sso.observationGrid);
                if (xPlus)
                    addObservationToEndOfRow(x + 1, y, obsToRemove, sso.observationGrid);
                if (yPlus)
                    addObservationToEndOfRow(x, y + 1, obsToRemove, sso.observationGrid);
                if (xPlus && yPlus)
                    addObservationToEndOfRow(x + 1, y + 1, obsToRemove, sso.observationGrid);
            }
        }
    }

    private static void addObservationToEndOfRow(int x, int y, Observation obs, Observation[][][] observationGrid) {
        Observation[] newRow = new Observation[observationGrid[x][y].length + 1];
        for (int k = 0; k < observationGrid[x][y].length; k++) {
            newRow[k] = observationGrid[x][y][k];
        }
        newRow[newRow.length - 1] = obs;
        observationGrid[x][y] = newRow;
    }

    public static List<Pair<Integer, Integer>> getAllSprites(SerializableStateObservation sso, int[] categories) {
        List<Pair<Integer, Integer>> retValue = new ArrayList();
        for (int i = 0; i < categories.length; i++) {
            retValue.addAll(getSpritesOfCategory(categories[i], sso));
        }
        return retValue;
    }

    public static List<Pair<Integer, Integer>> getSpritesOfCategory(int category, SerializableStateObservation sso) {
        List<Pair<Integer, Integer>> retValue = new ArrayList();
        Observation[][] observations = getObsArrayForCategory(category, sso);
        if (observations != null) {
            for (Observation[] a : observations) {
                for (Observation o : a) {
                    retValue.add(new Pair(o.obsID, o.itype));
                }
            }
        }
        return retValue;
    }

    public static Map<Integer, Pair<Integer, Double>> accuracyOf(Map<Integer, List<Pair<Double, Vector2d>>> predictions, SerializableStateObservation sso) {
        Map<Integer, Pair<Integer, Double>> retValue = new HashMap();
        List<Pair<Integer, Integer>> currentSprites = getAllSprites(sso, new int[]{1, 2, 3, 4, 5, 6});
        for (Pair<Integer, Integer> sprite : currentSprites) {
            int spriteID = sprite.getValue0();
            if (predictions.containsKey(spriteID)) {
                Vector2d currentPosition = SSOModifier.positionOf(spriteID, sso);
                int type = sprite.getValue1();
                if (!retValue.containsKey(type)) {
                    retValue.put(type, new Pair(0, 0.00));
                }
                double v = 0.00;
                for (Pair<Double, Vector2d> prediction : predictions.get(spriteID)) {
                    Vector2d predictedPosition = prediction.getValue1();
                    double closestSoFar = sso.blockSize / 2.0; // must be within at least this
                    double distanceToActualPosition = predictedPosition.dist(currentPosition);
                    if (distanceToActualPosition < closestSoFar) {
                        // correct
                        v = prediction.getValue0(); // the predicted probability
                        closestSoFar = distanceToActualPosition;
                    }
                }
                int countSoFar = retValue.get(type).getValue0();
                double accuracy = retValue.get(type).getValue1();
                retValue.put(type, new Pair(countSoFar + 1, (accuracy * countSoFar + v) / (countSoFar + 1.0)));
            }
        }
        return retValue;
    }

    public static Vector2d positionOf(int spriteID, SerializableStateObservation sso) {
        if (spriteID == 0) {
            return new Vector2d(sso.avatarPosition[0], sso.avatarPosition[1]);
        }
        for (int i = 1; i <= 6; i++) {
            Observation[][] obs = getObsArrayForCategory(i, sso);
            if (obs != null) {
                for (Observation[] ob : obs) {
                    for (Observation observation : ob) {
                        if (observation != null && observation.obsID == spriteID) {
                            // found it
                            return observation.position;
                        }
                    }
                }
            }
        }
        return new Vector2d(-100.0, -100.0); // not found at all
    }

    public static Observation createObservation(int id, int category, int type, double x, double y) {
        Observation retValue = new Observation();
        retValue.category = category;
        retValue.obsID = id;
        retValue.itype = type;
        Vector2d position = new Vector2d(x, y);
        retValue.position = position;
        return retValue;
    }

    public static Set<Pair<Integer, Integer>> detectCollisions(SerializableStateObservation sso) {

        if (sso.observationGrid == null || sso.observationGrid.length == 0) {
            constructGrid(sso);
        }
        // for the moment we define a collision as two sprites sharing a block
        // we then generate a list of colliding sprites

        // we have the position of each sprite ... do we just generate an Observation Grid, and then look for overlaps?
        // in this case though, we just add the id of the sprites
        Set<Pair<Integer, Integer>> retValue = new HashSet();

        // we can then just run through obsGrid
        for (int i = 0; i < sso.observationGrid.length; i++) {
            for (int j = 0; j < sso.observationGrid[i].length; j++) {
                if (sso.observationGrid[i][j].length > 1) {
                    for (int k = 0; k < sso.observationGrid[i][j].length - 1; k++) {
                        for (int l = k + 1; l < sso.observationGrid[i][j].length; l++) {
                            retValue.add(new Pair(sso.observationGrid[i][j][k].obsID, sso.observationGrid[i][j][l].obsID));
                        }
                    }
                }
            }
        }

        return retValue;
    }

    public static List<Pair<Integer, Vector2d>> newCollisionsOf(int objId, int category, SerializableStateObservation sso, Vector2d newPosition) {
        List<Pair<Integer, Vector2d>> retValue = new ArrayList();
        SerializableStateObservation projectedSSO = SSOModifier.copy(sso);
        moveSprite(objId, category, newPosition, projectedSSO);
        constructGrid(projectedSSO);
        Set<Pair<Integer, Integer>> collisions = newCollisions(sso, projectedSSO);
        for (Pair<Integer, Integer> c : collisions) {
            if (c.getValue0() == objId) {
                retValue.add(new Pair(c.getValue1(), SSOModifier.positionOf(c.getValue1(), sso)));
            }
            if (c.getValue1() == objId) {
                retValue.add(new Pair(c.getValue0(), SSOModifier.positionOf(c.getValue0(), sso)));
            }
        }
        return retValue;
    }

    public static Set<Pair<Integer, Integer>> newCollisions(SerializableStateObservation start, SerializableStateObservation finish) {
        Set<Pair<Integer, Integer>> collisionsBefore = SSOModifier.detectCollisions(start);
        Set<Pair<Integer, Integer>> collisionsAfter = SSOModifier.detectCollisions(finish);
        collisionsAfter.removeAll(collisionsBefore);
        return collisionsAfter;
    }

    public static final int TYPE_AVATAR = 0;
    public static final int TYPE_RESOURCE = 1;
    public static final int TYPE_PORTAL = 2;
    public static final int TYPE_NPC = 3;
    public static final int TYPE_STATIC = 4;
    public static final int TYPE_FROMAVATAR = 5;
    public static final int TYPE_MOVABLE = 6;

    private static Observation[][] getObsArrayForCategory(int category, SerializableStateObservation sso) {
        Observation[][] obsArray = new Observation[1][1];
        switch (category) {
            case TYPE_AVATAR:
                obsArray[0][0] = new Observation();
                obsArray[0][0].position = positionOf(0, sso);
                obsArray[0][0].category = 0;
                obsArray[0][0].itype = sso.avatarType;
                break;
            case TYPE_RESOURCE:
                obsArray = sso.resourcesPositions;
                break;
            case TYPE_PORTAL:
                obsArray = sso.portalsPositions;
                break;
            case TYPE_NPC:
                obsArray = sso.NPCPositions;
                break;
            case TYPE_STATIC:
                obsArray = sso.immovablePositions;
                break;
            case TYPE_FROMAVATAR:
                obsArray = sso.fromAvatarSpritesPositions;
                break;
            case TYPE_MOVABLE:
                obsArray = sso.movablePositions;
                break;
            default:
                throw new AssertionError("Unknown Category " + category);
        }
        return obsArray;
    }

    private static void setObsArrayForCategory(int category, SerializableStateObservation sso, Observation[][] obsArray) {
        switch (category) {
            case TYPE_RESOURCE:
                sso.resourcesPositions = obsArray;
                break;
            case TYPE_PORTAL:
                sso.portalsPositions = obsArray;
                break;
            case TYPE_NPC:
                sso.NPCPositions = obsArray;
                break;
            case TYPE_STATIC:
                sso.immovablePositions = obsArray;
                break;
            case TYPE_FROMAVATAR:
                sso.fromAvatarSpritesPositions = obsArray;
                break;
            case TYPE_MOVABLE:
                sso.movablePositions = obsArray;
                break;
        }
    }
}
