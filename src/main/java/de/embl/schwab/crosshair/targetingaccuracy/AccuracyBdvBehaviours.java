package de.embl.schwab.crosshair.targetingaccuracy;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.popup.BdvPopupMenus;
import de.embl.schwab.crosshair.plane.PlaneManager;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import static de.embl.schwab.crosshair.utils.BdvUtils.flipCurrentView;

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

        BdvPopupMenus.addAction(bdvHandle, "Fit To Points", ( x, y ) ->
        {
                addFitToPointsBehaviour();
        });

        BdvPopupMenus.addAction(bdvHandle, "Flip view", ( x, y ) ->
        {
            addFlipViewBehaviour();
        });

    }
}
