package hopshackle1.FeatureSets;

import hopshackle1.Cell;
import serialization.*;

public class Mesh {

    public int meshSize;
    private Cell[][] mesh_;

    public Mesh(int meshSize, SerializableStateObservation sso, int xOffset, int yOffset) {
        this.meshSize = meshSize;
        this.mesh_ = extractMeshAroundAvatar(meshSize, sso, xOffset, yOffset); // null 2-d array
    }

    public static Cell[][] extractMeshAroundAvatar(int meshSize, SerializableStateObservation sso, int xOffset, int yOffset) {
        Observation[][][] observations_grid = sso.getObservationGrid();
        Cell[][] retValue = new Cell[meshSize][meshSize];
        // note that Cell just records ctaegory and type of every sprite
        // This drops the other data (position, obsID, distance) that are in Observation

        if (observations_grid == null || observations_grid.length == 0) {
            return null;
        }

        double[] avatar_pos = sso.getAvatarPosition();
        int x = (int) (avatar_pos[0]) / sso.blockSize + xOffset;
        int y = (int) (avatar_pos[1]) / sso.blockSize + yOffset;

        double max_width = observations_grid.length;
        double max_hight = (observations_grid[0].length);
        // update observation
        for (int i = 0; i < meshSize; i++) {
            int pos_i = x - (meshSize / 2) + i;
            if (pos_i >= 0 && pos_i < max_width) {
                for (int j = 0; j < meshSize; j++) {
                    int pos_j = y - (meshSize / 2) + j;
                    if (pos_j >= 0 && pos_j < max_hight) {
                        // no observation
                        if (observations_grid[pos_i][pos_j] == null) {
                            retValue[i][j] = null;
                        } else {
                            Observation[] observations_array = observations_grid[pos_i][pos_j];
                            // no observation
                            if (observations_array.length == 0) {
                                retValue[i][j] = null;
                            } else { // has observation
                                Cell cell = new Cell(observations_array);
                                retValue[i][j] = cell;
                            }
                        }
                    }
                }
            }
        }

        return retValue;
    }

    @Override
    public boolean equals(Object comparison) {
        if (!(comparison instanceof Mesh)) {
            return false;
        }

        Mesh other = (Mesh) comparison;

        // check mesh
        for (int i = 0; i < meshSize; i++) {
            for (int j = 1; j < meshSize; j++) {
                if (mesh_[i][j] != other.mesh_[i][j]) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder retValue = new StringBuilder("Mesh Size = " + meshSize + "\n");
        for (int i = 0; i < mesh_.length; i++) {
            for (int j = 0; j < mesh_[i].length; j++) {
                String cellContents = "empty";
                if (mesh_[i][j].getNbObservations() != 0) {
                    mesh_[i][j].getObservations();
                    cellContents = "";
                    for (int[] detail : mesh_[i][j].getObservations()) {
                        cellContents += String.format("category=%d, itype=%d", detail[0], detail[1]);
                    }

                }
                retValue.append(String.format("%d%/%d %s\n", i, j, cellContents));
            }
        }
        return retValue.toString();
    }
}
