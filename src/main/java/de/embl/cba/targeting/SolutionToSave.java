package de.embl.cba.targeting;

import net.imglib2.RealPoint;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3d;

import java.util.ArrayList;
import java.util.Map;


public class SolutionToSave {

    private double initialKnifeAngle;
    private double initialTiltAngle;
    private double knife;
    private double tilt;
    private double rotation;
    private String firstTouch;
    private double distanceToCut;

    public SolutionToSave(double initialKnifeAngle, double initialTiltAngle, double knife, double tilt,
                          double rotation, String firstTouch, double distanceToCut) {
        this.initialKnifeAngle = initialKnifeAngle;
        this.initialTiltAngle = initialTiltAngle;
        this.knife = knife;
        this.tilt = tilt;
        this.rotation = rotation;
        this.firstTouch = firstTouch;
        this.distanceToCut = distanceToCut;
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

    public String getFirstTouch() {
        return firstTouch;
    }

    public double getDistanceToCut() {
        return distanceToCut;
    }
}
