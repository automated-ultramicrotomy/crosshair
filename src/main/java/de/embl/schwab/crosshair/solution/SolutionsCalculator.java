package de.embl.schwab.crosshair.solution;

import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.microtome.Microtome;
import de.embl.schwab.crosshair.plane.Plane;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.points.VertexPoint;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import net.imglib2.RealPoint;
import org.scijava.vecmath.Vector3d;

import java.util.ArrayList;
import java.util.Map;

import static java.lang.Math.*;

/**
 * Class to calculate Crosshair solutions from microtome/plane values.
 */
public class SolutionsCalculator {

    private Microtome microtome;
    private PlaneManager planeManager;

    private double solutionRotation;
    private double solutionKnife;
    private double solutionTilt;

    private boolean validSolution;

    private VertexPoint solutionFirstTouchVertexPoint;
    private Vector3d solutionFirstTouchPoint;
    private double distanceToCut;

    /**
     * Create a solutions calculator
     * @param microtome microtome
     */
    public SolutionsCalculator( Microtome microtome ) {
        this.microtome = microtome;
        this.planeManager = microtome.getPlaneManager();
        solutionFirstTouchPoint = new Vector3d();
        validSolution = false;
    }

    public boolean isValidSolution() {
        return validSolution;
    }

    public double getSolutionTilt() {
        return solutionTilt;
    }

    public double getSolutionKnife() {
        return solutionKnife;
    }

    public double getSolutionRotation() { return solutionRotation; }

    public double getDistanceToCut() {
        return distanceToCut;
    }

    public VertexPoint getSolutionFirstTouchVertexPoint() {
        return solutionFirstTouchVertexPoint;
    }

    /**
     * Get current solution, with nicely formatted values for saving to file. Note - this rounds values to 4dp for
     * nicer formatting when saving, don't use these values directly for calculations! Use the values from the getters
     * e.g. getSolutionKnife() for full precision.
     * @param unit distance unit
     * @return current solution (with rounding)
     */
    public Solution getSolution( String unit ) {
        return new Solution( microtome.getInitialKnifeAngle(),
                microtome.getInitialTiltAngle(), solutionKnife, solutionTilt,
                solutionRotation, solutionFirstTouchVertexPoint, distanceToCut, unit);
    }

    /**
     * Update current solution from the given rotation value
     * @param solutionRotation rotation value for new solution
     */
    public void setSolutionFromRotation( double solutionRotation ) {
        calculateRotations( solutionRotation, microtome.getInitialTiltAngle(), microtome.getInitialKnifeAngle(),
                microtome.getInitialTargetOffset(), microtome.getInitialTargetTilt() );
        Plane targetPlane = planeManager.getPlane( Crosshair.target );
        calculateDistance( planeManager.getVertexDisplay( Crosshair.block ).getAssignedVertices(),
                targetPlane.getNormal(), targetPlane.getPoint(), solutionKnife );
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

    private void calculateDistance( Map<VertexPoint, RealPoint> assignedVertices, Vector3d targetNormal,
                                   Vector3d targetPoint, double knifeAngle )  {

        double[] topLeft = new double[3];
        double[] topRight = new double[3];
        double[] bottomLeft = new double[3];
        double[] bottomRight = new double[3];
        assignedVertices.get( VertexPoint.TopLeft ).localize(topLeft);
        assignedVertices.get( VertexPoint.TopRight ).localize(topRight);
        assignedVertices.get( VertexPoint.BottomLeft ).localize(bottomLeft);
        assignedVertices.get( VertexPoint.BottomRight ).localize(bottomRight);

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
            solutionFirstTouchPoint.set(topLeft);
            solutionFirstTouchVertexPoint = VertexPoint.TopLeft;
        } else if (maxDistanceIndex == 1) {
            solutionFirstTouchPoint.set(topRight);
            solutionFirstTouchVertexPoint = VertexPoint.TopRight;
        } else if (maxDistanceIndex == 2) {
            solutionFirstTouchPoint.set(bottomLeft);
            solutionFirstTouchVertexPoint = VertexPoint.BottomLeft;
        } else if (maxDistanceIndex == 3) {
            solutionFirstTouchPoint.set(bottomRight);
            solutionFirstTouchVertexPoint = VertexPoint.BottomRight;
        }

        // Calculate perpendicular distance to target
        Vector3d firstTouchToTarget = new Vector3d();
        firstTouchToTarget.sub(targetPoint, solutionFirstTouchPoint);
        double perpDist = abs(firstTouchToTarget.dot(targetNormal));

        // Compensate for offset between perpendicular distance and true N-S of microtome
        // I believe this is just the angle of the knife in this scenario, as the knife was reset to true 0
        double NSDist = perpDist / cos( GeometryUtils.convertToRadians( knifeAngle ));
        distanceToCut = NSDist;
    }
}
