package de.embl.schwab.crosshair.targetingaccuracy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.embl.schwab.crosshair.plane.BlockPlane;
import de.embl.schwab.crosshair.solution.Solution;
import de.embl.schwab.crosshair.plane.Plane;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import net.imglib2.RealPoint;
import org.scijava.vecmath.Vector3d;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static de.embl.schwab.crosshair.utils.GeometryUtils.convertToDegrees;
import static java.lang.Math.cos;

public class AccuracyCalculator {

    private transient BlockPlane beforeBlock;
    private transient BlockPlane beforeTarget;
    private transient Plane afterBlock;
    private transient Solution solution;

    private double angleError;
    // i.e. did you cut the distance the solution told you to?
    private double solutionDistanceError;
    // i.e. shortest distance between point at centre of before target and after block plane
    private double targetPointToPlaneDistanceError;
    private String anglesUnit;
    private String distanceUnit;

    public AccuracyCalculator( BlockPlane beforeTarget, BlockPlane beforeBlock, Plane afterBlock, Solution solution ) {
        this.beforeBlock = beforeBlock;
        this.beforeTarget = beforeTarget;
        this.afterBlock = afterBlock;
        this.solution = solution;
        this.distanceUnit = solution.getDistanceUnit();
        this.anglesUnit = "degrees";
    }

    public double calculateAngleError() {
        Vector3d beforeNormal = beforeTarget.getNormal();
        Vector3d afterNormal = afterBlock.getNormal();

        double angle  = convertToDegrees( beforeNormal.angle( afterNormal ) );

        // we want the minimal angle (we don't care about which way the normal is pointing, only its orientation)
        if (angle > 90) {
            angle = 180 - angle;
        }

        angleError = angle;
        return angleError;
    }

    // return actual cut distance - the solution cut distance
    public double calculateSolutionDistanceError() {
        // Given the current orientation of your after block, how far did you cut? (if you started from the predicted
        // first touch point) [note if your angle was so far off that you didn't start cutting from the same first touch
        // point this measure will be off]

        RealPoint firstTouchPoint = beforeBlock.getVertexDisplay().getAssignedVertices().get( solution.getFirstTouch() );
        Vector3d firstTouch = new Vector3d( firstTouchPoint.positionAsDoubleArray() );

        // parallel to after block plane distance
        double distanceFromFirstTouchToAfterBlock = GeometryUtils.distanceFromPointToPlane(
                firstTouch, afterBlock.getNormal(), afterBlock.getPoint() );

        // compensate for knife angle, to get distance in NS cutting direction
        // [note this assumes your actual knife angle was the same as the one from the solution i.e. no knife angle error]
        double NSDist = distanceFromFirstTouchToAfterBlock / cos( GeometryUtils.convertToRadians( solution.getKnife() ));

        solutionDistanceError = NSDist - solution.getDistanceToCut();
        return solutionDistanceError;
    }

    public double calculateTargetPointToPlaneDistanceError() {
        // Calculate shortest distance between target vertex (placed at the centre of the structure of interest on the
        // before target plane) and the after block plane.
        ArrayList<RealPoint> vertices = beforeTarget.getVertexDisplay().getVertices();
        Vector3d targetPoint = new Vector3d( vertices.get(0).positionAsDoubleArray() );

        // shortest distance to after block
        targetPointToPlaneDistanceError = GeometryUtils.distanceFromPointToPlane(
                targetPoint, afterBlock.getNormal(), afterBlock.getPoint() );

        return targetPointToPlaneDistanceError;
    }

    public void saveAccuracy( String jsonPath ) {
        try {
            FileWriter fileWriter = new FileWriter( jsonPath );
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson( this, fileWriter);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }


}
