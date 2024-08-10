package de.embl.schwab.crosshair.solution;


import de.embl.schwab.crosshair.points.VertexPoint;
import org.apache.commons.math3.util.Precision;

/**
 * Class to store information about a Crosshair solution, ready for saving to a json file.
 */
public class Solution {

    private double initialKnifeAngle;
    private double initialTiltAngle;
    private double knife;
    private double tilt;
    private double rotation;
    private VertexPoint firstTouch;
    private double distanceToCut;
    private String anglesUnit;
    private String distanceUnit;
    private int saveDecimalPlaces;

    /**
     * Create a solution. Note - this class is intended to store nicely formatted solution values for saving to file.
     * Values will be rounded to 4dp, so don't use these directly for calculations! Use SolutionsCalculator instead.
     * @param initialKnifeAngle initial knife angle in degrees
     * @param initialTiltAngle initial tilt angle in degrees
     * @param knife knife angle in degrees
     * @param tilt tilt angle in degrees
     * @param rotation rotation angle in degrees
     * @param firstTouch block face vertex that will be touched first by the knife
     * @param distanceToCut distance to cut
     * @param unit distance unit
     */
    public Solution(double initialKnifeAngle, double initialTiltAngle, double knife, double tilt,
                    double rotation, VertexPoint firstTouch, double distanceToCut, String unit) {
        saveDecimalPlaces = 4;
        this.initialKnifeAngle = Precision.round(initialKnifeAngle, saveDecimalPlaces);
        this.initialTiltAngle = Precision.round(initialTiltAngle, saveDecimalPlaces);
        this.knife = Precision.round(knife,saveDecimalPlaces);
        this.tilt = Precision.round(tilt, saveDecimalPlaces);
        this.rotation = Precision.round(rotation, saveDecimalPlaces);
        this.firstTouch = firstTouch;
        this.distanceToCut = Precision.round(distanceToCut, saveDecimalPlaces);
        this.anglesUnit = "degrees";
        this.distanceUnit = unit;
    }

    public double getInitialKnifeAngle() {
        return initialKnifeAngle;
    }

    public double getInitialTiltAngle() {
        return initialTiltAngle;
    }

    public double getKnife() {
        return knife;
    }

    public double getTilt() {
        return tilt;
    }

    public double getRotation() {
        return rotation;
    }

    public VertexPoint getFirstTouch() {
        return firstTouch;
    }

    public double getDistanceToCut() {
        return distanceToCut;
    }

    public String getAnglesUnit() {
        return anglesUnit;
    }

    public String getDistanceUnit() {
        return distanceUnit;
    }
}
