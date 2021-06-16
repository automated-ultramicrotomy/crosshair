package de.embl.schwab.crosshair.microtome;

import de.embl.schwab.crosshair.PlaneManager;
import de.embl.schwab.crosshair.io.SettingsToSave;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import net.imglib2.RealPoint;
import org.scijava.vecmath.Vector3d;

import java.util.ArrayList;
import java.util.Map;

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

    Solutions( Microtome microtome ) {
        this.microtome = microtome;
        this.planeManager = microtome.getPlaneManager();
        solutionFirstTouch = new Vector3d();
        validSolution = false;
    }

    Solutions() {
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

    void setSolutionFromRotation( double solutionRotation, double initialTiltAngle, double initialKnifeAngle,
                                 SettingsToSave settings ) {
        TargetOffsetAndTilt targetOffsetAndTilt = new TargetOffsetAndTilt( settings.getNamedVertices(),
                settings.getPlaneNormals() );
        calculateRotations( solutionRotation, initialTiltAngle, initialKnifeAngle,
                targetOffsetAndTilt.targetOffset, targetOffsetAndTilt.targetTilt );
        calculateDistance( settings.getNamedVertices(), settings.getPlaneNormals(), settings.getPlanePoints(),
                solutionKnife );
        checkSolutionValid();
    }

    void setSolutionFromRotation (double solutionRotation) {
        calculateRotations( solutionRotation, microtome.getInitialTiltAngle(), microtome.getInitialKnifeAngle(),
                microtome.getInitialTargetOffset(), microtome.getInitialTargetTilt() );
        calculateDistance( planeManager.getNamedVertices(), planeManager.getPlaneNormals(), planeManager.getPlanePoints(),
                solutionKnife );
        checkSolutionValid();
    }

    private void checkSolutionValid () {
        if (solutionTilt < -20 | solutionTilt > 20 | solutionKnife < -30 | solutionKnife > 30) {
            validSolution = false;
        } else {
            validSolution = true;
        }
    }

    private void calculateRotations( double solutionRotation, double initialTiltAngle, double initialKnifeAngle,
                                     double initialTargetOffset, double initialTargetTilt ) {
        this.solutionRotation = solutionRotation;
        double rot = GeometryUtils.convertToRadians(solutionRotation);
        double iTilt = GeometryUtils.convertToRadians( initialTiltAngle );
        double iKnife = GeometryUtils.convertToRadians( initialKnifeAngle );
        double tOffset = GeometryUtils.convertToRadians( initialTargetOffset );
        double tRotation = GeometryUtils.convertToRadians( initialTargetTilt );

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
        this.solutionTilt = GeometryUtils.convertToDegrees(solTilt);

        double solKnife = atan((A*I + H)*(E*cos(rot) + (A*F - G)*sin(rot))/(sqrt(pow(A*I + H, 2) + pow(E*sin(rot) + (-A*F + G)*cos(rot), 2))*abs(A*I + H)));
        this.solutionKnife = GeometryUtils.convertToDegrees(solKnife);
    }

    private void calculateDistance( Map<String, RealPoint> namedVertices, Map<String, Vector3d> planeNormals,
                                    Map<String, Vector3d> planePoints, double knifeAngle )  {
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

        // Calculate first point touched on block face
        // Originally I did this by calculating perpendicular distance from target to each point (unsigned),
        // and returning that with the largest. This fails for edge case where the target plane intersects the block
        // plane within teh bounds of teh block face. e.g. you're chipping a corner off the block, or not going particularly deep
        // so some of the block face remains. In this case, some vertices can be in front of the target plane and some behind,
        // so absolute distance no longer works.
        // To get around this, we calculate the signed distance (+ve in direction of normal pointing out of block face), and
        // return the maximum.

        // all points as vectors
        targetNormal.normalize();
        Vector3d topLeftV = new Vector3d(topLeft);
        Vector3d topRightV = new Vector3d(topRight);
        Vector3d bottomLeftV = new Vector3d(bottomLeft);
        Vector3d bottomRightV = new Vector3d(bottomRight);

        // Normal pointing out of block face
        Vector3d edgeVector = new Vector3d();
        edgeVector.sub(bottomRightV, bottomLeftV);

        Vector3d upVector = new Vector3d();
        upVector.sub(topLeftV, bottomLeftV);

        Vector3d normalOutBlock = new Vector3d();
        normalOutBlock.cross(edgeVector, upVector);

        // Signed distance
        ArrayList<Vector3d> allVertices = new ArrayList<>();
        allVertices.add(topLeftV);
        allVertices.add(topRightV);
        allVertices.add(bottomLeftV);
        allVertices.add(bottomRightV);
        int maxDistanceIndex = GeometryUtils.indexSignedMinMaxPointsToPlane(targetPoint, targetNormal, allVertices, normalOutBlock, "max");

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
        double NSDist = perpDist / cos( GeometryUtils.convertToRadians( knifeAngle ));
        distanceToCut = NSDist;
    }
}
