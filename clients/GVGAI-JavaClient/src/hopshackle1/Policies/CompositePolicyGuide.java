package hopshackle1.Policies;

import serialization.*;
import java.util.*;

public class CompositePolicyGuide implements PolicyGuide {

    private Set<PolicyGuide> allGuides = new HashSet();

    public void add(PolicyGuide newGuide) {
        allGuides.add(newGuide);
    }
    public void remove(PolicyGuide oldGuide) {
        allGuides.remove(oldGuide);
    }
    public void clear() {
        allGuides = new HashSet();
    }

    @Override
    public double locationBonus(Vector2d location) {
        double retValue = 0.0;
        for (PolicyGuide guide : allGuides) {
            retValue += guide.locationBonus(location);
        }
        return retValue;
    }
}
