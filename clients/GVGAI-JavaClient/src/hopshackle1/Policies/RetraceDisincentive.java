package hopshackle1.Policies;

import hopshackle1.models.GameStatusTracker;
import serialization.*;

import java.util.*;

public class RetraceDisincentive implements PolicyGuide {

    private Map<Integer, Integer> footsteps;

    public RetraceDisincentive(Map<Integer, Integer> previousFootsteps) {
        footsteps = previousFootsteps;
    }

    @Override
    public double locationBonus(Vector2d location) {
        int key = (int) (location.x * 307 + location.y);
        int visits = footsteps.getOrDefault(key, 0);
        return -visits;
    }
}
