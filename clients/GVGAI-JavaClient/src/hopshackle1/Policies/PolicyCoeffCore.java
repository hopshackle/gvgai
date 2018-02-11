package hopshackle1.Policies;

import hopshackle1.EntityLog;
import hopshackle1.HopshackleUtilities;
import hopshackle1.State;
import serialization.Types.ACTIONS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolicyCoeffCore {

    private Map<Integer, Double> coefficients;
    private boolean debug;
    private EntityLog logFile;
    private String name;

    public PolicyCoeffCore(String name, boolean debug) {
        this.name = name;
        this.debug = debug;
        if (debug) logFile = new EntityLog(name);
        coefficients = new HashMap();
    }

    public void refreshFrom(PolicyCoeffCore pcc) {
        if(debug) logFile.log("Refreshing from " + pcc.name);
        coefficients = new HashMap();
        for (int i = 0; i < pcc.coefficients.size(); i++) {
            Map<Integer, Double> cloneActionTheta = new HashMap();
            for (Integer f : pcc.coefficients.keySet()) {
                cloneActionTheta.put(f, pcc.coefficients.get(f));
            }
            coefficients = cloneActionTheta;
        }
    }

    public double getCoeffFor(int feature) {
        if (!coefficients.containsKey(feature)) {
            coefficients.put(feature, 0.0);
        }
        return coefficients.get(feature);
    }

    public void setCoeffFor(int feature, double value) {
        coefficients.put(feature, value);
    }

    public void clearCoeffs(int index) {
        coefficients = new HashMap();
    }

    public double valueState(State state) {
        double retValue = 0.0;
        for (int feature : state.features.keySet()) {
            double coeff = getCoeffFor(feature);
            retValue += state.features.get(feature) * coeff;
            if (debug) {
                logFile.log(String.format("\tFeature %d\thas coefficient %.2f for state value", feature, coeff));
            }
        }
        if (debug) {
            logFile.log(String.format("Total value for state is %.2f", retValue));
            logFile.flush();
        }
        return retValue;
    }
}

