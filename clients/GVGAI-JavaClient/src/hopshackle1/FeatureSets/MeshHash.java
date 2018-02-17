package hopshackle1.FeatureSets;

import hopshackle1.Cell;
import hopshackle1.HopshackleUtilities;
import serialization.*;
import java.util.*;

public class MeshHash {

    private int hashCode;
    public Cell[][] mesh;

    public MeshHash(int meshSize, SerializableStateObservation sso, int xOffset, int yOffset) {
        this.mesh = Mesh.extractMeshAroundAvatar(meshSize, sso, xOffset, yOffset);
        this.hashCode = calculateFeatureHash(mesh);
    }

    private int calculateFeatureHash(Cell[][] mesh_) {
        if (mesh_ == null) return 0;
        int retValue = 0;
        for (int i = 0; i < mesh_.length; i++) {
            for (int j = 0; j < mesh_.length; j++) {
                // sort observations in order
                if (mesh_[i][j] == null || mesh_[i][j].getNbObservations() == 0) {
                    // no impact on hash, which is zero for a completely empty mesh
                } else {
                    List<int[]> cloned_list = HopshackleUtilities.cloneList(mesh_[i][j].getObservations());
                    cloned_list.sort(sortByObjType);
                    for (int k = 0; k < cloned_list.size(); k++) {
                        retValue += (i * 19) + (j * 167) + (cloned_list.get(k)[1] * 1319);
                    }
                }
            }
        }
        return retValue;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object object_to_compare) {
        // not same Class
        if (!(object_to_compare instanceof MeshHash)) {
            return false;
        }

        return ((MeshHash) object_to_compare).hashCode == this.hashCode;
    }

    private static Comparator sortByObjType = new Comparator<int[]>() {
        @Override
        public int compare(int[] a, int[] b) {
            // just by type (0 is category, 1 is type)
            return (a[1] - b[1]);
        }
    };

    @Override
    public String toString() {
        StringBuilder retValue = new StringBuilder(String.format("Hash: %d, Mesh Size: %d by %d\n", hashCode, mesh.length, mesh[0].length));
        for (int i = 0; i < mesh.length; i++) {
            for (int j = 0; j < mesh[i].length; j++) {
                String cellContents = "empty";
                if (mesh[i][j] != null && mesh[i][j].getNbObservations() != 0) {
                    mesh[i][j].getObservations();
                    cellContents = "";
                    for (int[] detail : mesh[i][j].getObservations()) {
                        cellContents += String.format("category=%d, itype=%d", detail[0], detail[1]);
                    }

                }
                retValue.append(String.format("\t%d:%d %s\n", i, j, cellContents));
            }
        }
        return retValue.toString();
    }
}
