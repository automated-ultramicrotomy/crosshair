package de.embl.cba.crosshair.microtome;

import de.embl.cba.crosshair.PlaneManager;
import net.imglib2.RealPoint;
import org.scijava.vecmath.Vector3d;

import java.util.ArrayList;
import java.util.Map;

import static de.embl.cba.crosshair.utils.GeometryUtils.*;
import static de.embl.cba.crosshair.utils.GeometryUtils.convertToRadians;
import static java.lang.Math.*;

class Solutions {

    private Microtome microtome;
    private PlaneManager planeManager;

    private double solutionRotation;
    private double solutionKnife;
    private double solutionTilt;

    private boolean validSolution;

    private String solutionFirstTouchName;
    private Vector3d solutionFirstTouch;
    private double distanceToCut;

    Solutions (Microtome microtome) {
        this.microtome = microtome;
        this.planeManager = microtome.getPlaneManager();
        solutionFirstTouch = new Vector3d();
        validSolution = false;
    }

    boolean isValidSolution() {
        return validSolution;
    }

    double getSolutionTilt() {
        return solutionTilt;
    }

    double getSolutionKnife() {
        return solutionKnife;
    }

    double getSolutionRotation() { return solutionRotation; }

    double getDistanceToCut() {
        return distanceToCut;
    }

    String getSolutionFirstTouchName() {
        return solutionFirstTouchName;
    }

    void setSolutionFromRotation (double solutionRotation) {
        this.solutionRotation = solutionRotation;
        double rot = convertToRadians(solutionRotation);
        double iTilt = convertToRadians( microtome.getInitialTiltAngle() );
        double iKnife = convertToRadians( microtome.getInitialKnifeAngle() );
        double tOffset = convertToRadians( microtome.getInitialTargetOffset() );
        double tRotation = convertToRadians( microtome.getInitialTargetTilt() );

        double A = cos(iKnife + tOffset);
        double B =  sin(tRotation)*sin(iKnife + tOffset);
        double C = sin(iTilt)*sin(iKnife+tOffset);
        double D = cos(iTilt)*sin(iKnife+tOffset);
        double E = cos(tRotation)*sin(iKnife+tOffset);
        double F = sin(iTilt)*cos(tRotation);
        double G = sin(tRotation)*cos(iTilt);
        double H = sin(iTilt)*sin(tRotation);
        double I = cos(iTilt)*cos(tRotation);

        double solTilt = atan(((-A*F + G)/(-A*I -H))*cos(rot) + ((E/(-A*I - H))*sin(rot)));
        this.solutionTilt = convertToDegrees(solTilt);

        double solKnife = atan((A*I + H)*(E*cos(rot) + (A*F - G)*sin(rot))/(sqrt(pow(A*I + H, 2) + pow(E*sin(rot) + (-A*F + G)*cos(rot), 2))*abs(A*I + H)));
        this.solutionKnife = convertToDegrees(solKnife);

        calculateDistance();
        checkSolutionValid();
    }

    private void checkSolutionValid () {
        if (solutionTilt < -20 | solutionTilt > 20 | solutionKnife < -30 | solutionKnife > 30) {
            validSolution = false;
        } else {
            validSolution = true;
        }
    }

    private void calculateDistance () {
        //TODO - calculate once then only update for the knife angle interactively???
        Map<String, RealPoint> namedVertices = planeManager.getNamedVertices();
        Map<String, Vector3d> planeNormals = planeManager.getPlaneNormals();
        Map<String, Vector3d> planePoints = planeManager.getPlanePoints();
        Vector3d targetNormal = new Vector3d(planeNormals.get("target"));
        Vector3d targetPoint = new Vector3d(planePoints.get("target"));

        double[] topLeft = new double[3];
        double[] topRight = new double[3];
        double[] bottomLeft = new double[3];
        double[] bottomRight = new double[3];
        namedVertices.get("Top Left").localize(topLeft);
        namedVertices.get("Top Right").localize(topRight);
        namedVertices.get("Bottom Left").localize(bottomLeft);
        namedVertices.get("Bottom Right").localize(bottomRight);

        // Calculate first point touched on block face, done by calculating perpendicular distance
        // from target to each point, and returning that with the largest.

        // Calculate perpendicular distance to each point
        targetNormal.normalize();
        ArrayList<Vector3d> allVertices = new ArrayList<>();
        allVertices.add(new Vector3d(topLeft));
        allVertices.add(new Vector3d(topRight));
        allVertices.add(new Vector3d(bottomLeft));
        allVertices.add( new Vector3d(bottomRight));

        int maxDistanceIndex = indexMinMaxPointsToPlane(targetPoint, targetNormal, allVertices, "max");


        //  Assign first touch to point with maximum distance
        if (maxDistanceIndex == 0) {
            solutionFirstTouch.set(topLeft);
            solutionFirstTouchName = "Top Left";
        } else if (maxDistanceIndex == 1) {
            solutionFirstTouch.set(topRight);
            solutionFirstTouchName = "Top Right";
        } else if (maxDistanceIndex == 2) {
            solutionFirstTouch.set(bottomLeft);
            solutionFirstTouchName = "Bottom Left";
        } else if (maxDistanceIndex == 3) {
            solutionFirstTouch.set(bottomRight);
            solutionFirstTouchName = "Bottom Right";
        }

        // Calculate perpendicular distance to target
        Vector3d firstTouchToTarget = new Vector3d();
        firstTouchToTarget.sub(targetPoint, solutionFirstTouch);
        double perpDist = abs(firstTouchToTarget.dot(targetNormal));

        // Compensate for offset between perpendicular distance and true N-S of microtome
        // I believe this is just the angle of the knife in this scenario, as the knife was reset to true 0
        double NSDist = perpDist / cos(convertToRadians( microtome.getKnife() ));
        distanceToCut = NSDist;
    }
}
