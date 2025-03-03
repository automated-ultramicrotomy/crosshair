package de.embl.schwab.crosshair.microtome;

import de.embl.schwab.crosshair.points.VertexPoint;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import net.imglib2.RealPoint;
import org.jogamp.vecmath.Vector3d;

import java.util.Map;

/**
 * Class to calculate the target offset / tilt for the block.
 */
public class TargetOffsetAndTilt {
    public double targetOffset; // in degrees
    public double targetTilt; // in degrees

    public TargetOffsetAndTilt() { }

    /**
     * Calculate target offset / tilt for given settings (stored in targetOffset and targetTilt)
     * @param assignedVertices the block face's assigned vertices
     * @param blockNormal block face normal
     * @param targetNormal target plane normal
     */
    public TargetOffsetAndTilt(
            Map<VertexPoint, RealPoint> assignedVertices, Vector3d blockNormal, Vector3d targetNormal ) {

        double[] topLeft = new double[3];
        double[] bottomLeft = new double[3];
        double[] bottomRight = new double[3];
        assignedVertices.get( VertexPoint.TopLeft ).localize(topLeft);
        assignedVertices.get( VertexPoint.BottomLeft ).localize(bottomLeft);
        assignedVertices.get( VertexPoint.BottomRight ).localize(bottomRight);

        // Vector along the bottom edge of block, left to right
        Vector3d bottomEdgeVector = new Vector3d();
        bottomEdgeVector.sub(new Vector3d(bottomRight), new Vector3d(bottomLeft));

        // Vector pointing 'up' along left edge of block. Bear in mind may not be exactly perpendicular to the
        // edge_vector due to not perfectly rectangular block shape. I correct for this later
        Vector3d upLeftSideVector = new Vector3d();
        upLeftSideVector.sub(new Vector3d(topLeft), new Vector3d(bottomLeft));

        // Calculate line perpendicular to x (edge vector), in plane of block face
        Vector3d vertical = new Vector3d();
        vertical.cross(blockNormal, bottomEdgeVector);

        // But depending on orientation of block normal, this could be 'up' or 'down' relative to user
        // to force this to be 'up' do:
        if (upLeftSideVector.dot(vertical) < 0) {
            vertical.negate();
        }

        this.targetOffset = GeometryUtils.rotationPlaneToPlane(vertical, targetNormal, blockNormal);

        // Calculate initial target tilt
        // Calculate tilt (about the new axis of rotation, i.e. about line of intersection from previous calc)
        // new axis of rotation, unknown orientation
        Vector3d axisRotation = new Vector3d();
        axisRotation.cross(targetNormal, vertical);
        // I force this to be pointing in the general direction of the edge vector to give consistent clockwise vs anticlockwise
        // i.e. I look down the right side of the vector
        if (axisRotation.dot(bottomEdgeVector) < 0) {
            axisRotation.negate();
        }

        Vector3d intersectionNormal = new Vector3d();
        intersectionNormal.cross(axisRotation, vertical);
        this.targetTilt = GeometryUtils.rotationPlaneToPlane(axisRotation, targetNormal, intersectionNormal);
    }
}
