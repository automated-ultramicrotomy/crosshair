package de.embl.schwab.crosshair.targetingaccuracy;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.popup.BdvPopupMenus;
import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.plane.BlockPlane;
import de.embl.schwab.crosshair.plane.Plane;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.points.VertexPoint;
import ij.IJ;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.vecmath.Vector3d;

import java.util.Map;

import static de.embl.schwab.crosshair.utils.BdvUtils.flipCurrentView;
import static de.embl.schwab.crosshair.utils.BdvUtils.shiftCurrentView;

public class AccuracyBdvBehaviours {

    private final BdvHandle bdvHandle;
    private final PlaneManager planeManager;

    public AccuracyBdvBehaviours( BdvHandle bdvHandle, PlaneManager planeManager ) {
        this.bdvHandle = bdvHandle;
        this.planeManager = planeManager;

        installBehaviours();
    }

    private void addPointBehaviour() {
        if ( planeManager.isTrackingPlane() ) {
            IJ.log("Can't change points when tracking a plane");
        } else {
            if ( !planeManager.checkNamedPlaneExists( TargetingAccuracy.afterBlock ) ) {
                planeManager.addPlane( TargetingAccuracy.afterBlock );
            }
            planeManager.getPointsToFitPlaneDisplay( TargetingAccuracy.afterBlock ).addOrRemoveCurrentPositionFromPointsToFitPlane();
        }
    }

    private void addVertexBehaviour() {
        if ( planeManager.isTrackingPlane() ) {
            IJ.log("Can't change points when tracking a plane");
        } else if ( !planeManager.checkNamedPlaneExistsAndOrientationIsSet( TargetingAccuracy.beforeTarget )) {
            IJ.log("Before target plane doesn't exist - vertices must lie on this plane!");
        } else {
            Plane blockPlane = planeManager.getPlane( TargetingAccuracy.beforeTarget );
            planeManager.getVertexDisplay( TargetingAccuracy.beforeTarget ).addOrRemoveCurrentPositionFromVertices( blockPlane );
        }
    }

    private void addFitToPointsBehaviour() {
        if ( planeManager.isTrackingPlane() && planeManager.getTrackedPlaneName().equals( TargetingAccuracy.afterBlock ) ) {
            IJ.log("Can't fit to points when tracking after block plane");
        } else {
            if ( planeManager.checkNamedPlaneExists( TargetingAccuracy.afterBlock ) ) {
                planeManager.fitToPoints(TargetingAccuracy.afterBlock);
            }
        }
    }

    private void addFlipViewBehaviour() {
        if ( planeManager.isTrackingPlane() ) {
            IJ.log("Can't flip view while tracking a plane.");
        } else {
            flipCurrentView( bdvHandle );
        }
    }

    private void addShiftViewBehaviour() {
        if ( planeManager.isTrackingPlane() ) {
            IJ.log("Can't shift view while tracking a plane.");
        } else {
            BlockPlane blockPlane = (BlockPlane) planeManager.getPlane( TargetingAccuracy.beforeBlock );
            double[] topLeft = new double[3];
            double[] bottomLeft = new double[3];
            double[] bottomRight = new double[3];
            Map<VertexPoint, RealPoint> assignedVertices = blockPlane.getVertexDisplay().getAssignedVertices();
            assignedVertices.get( VertexPoint.TopLeft ).localize(topLeft);
            assignedVertices.get( VertexPoint.BottomLeft ).localize(bottomLeft);
            assignedVertices.get( VertexPoint.BottomRight ).localize(bottomRight);

            // all points as vectors
            Vector3d topLeftV = new Vector3d(topLeft);
            Vector3d bottomLeftV = new Vector3d(bottomLeft);
            Vector3d bottomRightV = new Vector3d(bottomRight);

            // Normal pointing out of block face
            Vector3d edgeVector = new Vector3d();
            edgeVector.sub(bottomRightV, bottomLeftV);

            Vector3d upVector = new Vector3d();
            upVector.sub(topLeftV, bottomLeftV);

            Vector3d normalInBlock = new Vector3d();
            normalInBlock.cross(upVector, edgeVector);
            normalInBlock.normalize();

            shiftCurrentView( bdvHandle, planeManager.getPlane(TargetingAccuracy.afterBlock), normalInBlock );
        }
    }

    private void installBehaviours() {
        final Behaviours behaviours = new Behaviours(new InputTriggerConfig());
        behaviours.install( bdvHandle.getTriggerbindings(), "accuracy" );

        bdvHandle.getViewerPanel().addTransformListener(new bdv.viewer.TransformListener<AffineTransform3D>() {
            @Override
            public void transformChanged(AffineTransform3D affineTransform3D) {
                if ( planeManager.isTrackingPlane() ) {
                    planeManager.updatePlaneOnTransformChange( affineTransform3D, planeManager.getTrackedPlaneName() );
                }
            }
        });

        behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
            if ( planeManager.isInPointMode() ) {
                addPointBehaviour();
            } else if ( planeManager.isInVertexMode() ) {
                addVertexBehaviour();
            }
        }, "Left Click behaviours", "button1" );

        BdvPopupMenus.addAction(bdvHandle, "Toggle Point Mode", ( x, y ) ->
        {
            if ( !planeManager.isInPointMode() ) {
                if ( planeManager.isInVertexMode() ) {
                    planeManager.setVertexMode( false );
                }
                planeManager.setPointMode( true );
            } else {
                planeManager.setPointMode( false );
            }
        });

        BdvPopupMenus.addAction(bdvHandle, "Toggle Target Vertex Mode", ( x, y ) ->
        {
            if ( !planeManager.isInVertexMode() ) {
                if ( planeManager.isInPointMode() ) {
                    planeManager.setPointMode( false );
                }
                planeManager.setVertexMode( true );
            } else {
                planeManager.setVertexMode( false );
            }
        });

        BdvPopupMenus.addAction(bdvHandle, "Fit To Points", ( x, y ) ->
        {
                addFitToPointsBehaviour();
        });

        BdvPopupMenus.addAction(bdvHandle, "Flip view", ( x, y ) ->
        {
            addFlipViewBehaviour();
        });

        BdvPopupMenus.addAction(bdvHandle, "Shift view", ( x, y ) ->
        {
            addShiftViewBehaviour();
        });

    }
}
