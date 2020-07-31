package de.embl.cba.crosshair.microtome;

import net.imglib2.RealPoint;
import org.scijava.vecmath.Vector3d;

import java.util.ArrayList;
import java.util.Map;

import static de.embl.cba.crosshair.utils.GeometryUtils.*;
import static de.embl.cba.crosshair.utils.GeometryUtils.convertToRadians;
import static java.lang.Math.*;

class Solutions {

    public void setSolutionFromRotation (double solutionRotation) {
        microtomePanel.getRotationAngle().setCurrentValue(solutionRotation);

        double rot = convertToRadians(solutionRotation);
        double iTilt = convertToRadians(initialTiltAngle);
        double iKnife = convertToRadians(initialKnifeAngle);
        double tOffset = convertToRadians(initialTargetOffset);
        double tRotation = convertToRadians(initialTargetTilt);

        double A = cos(iKnife + tOffset);
        double B =  sin(tRotation)*sin(iKnife + tOffset);
        double C = sin(iTilt)*sin(iKnife+tOffset);
        double D = cos(iTilt)*sin(iKnife+tOffset);
        double E = cos(tRotation)*sin(iKnife+tOffset);
        double F = sin(iTilt)*cos(tRotation);
        double G = sin(tRotation)*cos(iTilt);
        double H = sin(iTilt)*sin(tRotation);
        double I = cos(iTilt)*cos(tRotation);

//        solution tilt & rot
        double solTilt = atan(((-A*F + G)/(-A*I -H))*cos(rot) + ((E/(-A*I - H))*sin(rot)));
        double solTiltDegrees = convertToDegrees(solTilt);
        System.out.println(solTiltDegrees);

//    solution knife & rot
        double solKnife = atan((A*I + H)*(E*cos(rot) + (A*F - G)*sin(rot))/(sqrt(pow(A*I + H, 2) + pow(E*sin(rot) + (-A*F + G)*cos(rot), 2))*abs(A*I + H)));
        double solKnifeDegrees = convertToDegrees(solKnife);
        System.out.println(solKnifeDegrees);

//        If solution invalid i.e. does not fit constraints of the angles the microtome can reach
        if (solTiltDegrees < -20 | solTiltDegrees > 20 | solKnifeDegrees < -30 | solKnifeDegrees > 30) {
//            Still set to value, so microtome moves / maxes out limit - makes for a smoother transition
            microtomePanel.getTiltAngle().setCurrentValue(solTiltDegrees);
            microtomePanel.getKnifeAngle().setCurrentValue(solKnifeDegrees);

//            Display first touch as nothing, and distance as 0
            microtomePanel.setFirstTouch("");
            microtomePanel.setDistanceToCut(0);
            microtomePanel.setValidSolution(false);
        } else {
            microtomePanel.getTiltAngle().setCurrentValue(solTiltDegrees);
            microtomePanel.getKnifeAngle().setCurrentValue(solKnifeDegrees);
            calculateDistance();
            microtomePanel.setValidSolution(true);
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

//        Calculate first point touched on block face, done by calculating perpendicular distance
//        from target to each point, and returning that with the largest.

//        Calculate perpendicular distance to each point
        targetNormal.normalize();
        ArrayList<Vector3d> allVertices = new ArrayList<>();
        allVertices.add(new Vector3d(topLeft));
        allVertices.add(new Vector3d(topRight));
        allVertices.add(new Vector3d(bottomLeft));
        allVertices.add( new Vector3d(bottomRight));

        int maxDistanceIndex = indexMinMaxPointsToPlane(targetPoint, targetNormal, allVertices, "max");

        Vector3d firstTouch = new Vector3d();
//        Assign first touch to point with maximum distance
        if (maxDistanceIndex == 0) {
            firstTouch.set(topLeft);
            microtomePanel.setFirstTouch("Top Left");
        } else if (maxDistanceIndex == 1) {
            firstTouch.set(topRight);
            microtomePanel.setFirstTouch("Top Right");
        } else if (maxDistanceIndex == 2) {
            firstTouch.set(bottomLeft);
            microtomePanel.setFirstTouch("Bottom Left");
        } else if (maxDistanceIndex == 3) {
            firstTouch.set(bottomRight);
            microtomePanel.setFirstTouch("Bottom Right");
        }
        firstTouchPointSolution = new Vector3d(firstTouch);

//        Calculate perpendicular distance to target
        Vector3d firstTouchToTarget = new Vector3d();
        firstTouchToTarget.sub(targetPoint, firstTouch);
        double perpDist = abs(firstTouchToTarget.dot(targetNormal));

//        Compensate for offset between perpendicular distance and true N-S of microtome
//        I believe this is just the angle of the knife in this scenario, as the knife was reset to true 0
        double NSDist = perpDist / cos(convertToRadians(knife));

        microtomePanel.setDistanceToCut(NSDist);
    }
}
