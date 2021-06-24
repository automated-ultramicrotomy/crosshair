package de.embl.schwab.crosshair.targetingaccuracy;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.popup.BdvPopupMenus;
import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.plane.BlockPlane;
import de.embl.schwab.crosshair.plane.Plane;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.vecmath.Vector3d;

import javax.swing.*;
import java.util.ArrayList;

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
        } else if ( !planeManager.checkNamedPlaneExists( TargetingAccuracy.afterBlock )) {
            IJ.log("After block plane doesn't exist" );
        } else {
            planeManager.getPlane( TargetingAccuracy.afterBlock ).addOrRemoveCurrentPositionFromPointsToFitPlane();
        }
    }

    private void fitToPoints( Plane plane ) {
        ArrayList<Vector3d> planeDefinition = GeometryUtils.fitPlaneToPoints( plane.getPointsToFitPlane() );
        planeManager.updatePlane( planeDefinition.get(0), planeDefinition.get(1), Crosshair.block );
        plane.setVisible( true );
    }

    private void addFitToPointsBehaviour() {
        if ( planeManager.isTrackingPlane() && planeManager.getTrackedPlaneName().equals( Crosshair.block ) ) {
            IJ.log("Can't fit to points when tracking block plane");
        } else if ( !planeManager.checkNamedPlaneExists( TargetingAccuracy.afterBlock ) ) {
            IJ.log("After block plane doesn't exist" );
        } else {
            Plane plane = planeManager.getPlane( TargetingAccuracy.afterBlock );
            if ( plane.getPointsToFitPlane().size() >= 3 ) {
                    fitToPoints( plane );
            } else {
                IJ.log ("Need at least 3 points to fit plane");
            }
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

        BdvPopupMenus.addAction(bdvHandle, "Toggle Vertex Mode", ( x, y ) ->
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

    }
}
