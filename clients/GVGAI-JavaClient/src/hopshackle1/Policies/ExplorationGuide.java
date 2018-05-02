package hopshackle1.Policies;

import hopshackle1.models.GameStatusTracker;
import serialization.*;
import java.util.*;

public class ExplorationGuide implements PolicyGuide {

    private List<Vector2d> locations = new ArrayList();
    private double blockSize;

    public ExplorationGuide(int targetType, GameStatusTracker gst) {
        List<Integer> targets = gst.getAllSpritesOfType(targetType);
        for (int id : targets) {
            locations.add(gst.getCurrentPosition(id));
        }
        blockSize = gst.getBlockSize();
    }

    @Override
    public double locationBonus(Vector2d location) {
        if (locations.isEmpty()) return 0.0;
        double closest = Double.MAX_VALUE;
        for (Vector2d pos : locations) {
            double d = pos.dist(location);
            if (d < closest)
                closest = d;
        }
        return -closest / blockSize;
    }
}
