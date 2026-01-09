package de.embl.schwab.crosshair.targetingaccuracy;

import bdv.util.BdvHandle;
import de.embl.schwab.crosshair.plane.Plane;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.utils.BdvPopupMenus;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import static de.embl.schwab.crosshair.utils.BdvUtils.flipCurrentView;

/**
 * Class to control custom interactions with the BigDataViewer window during the targeting accuracy workflow
 * For example, clicking to add points or fitting a plane to points
 */
public class AccuracyBdvBehaviours {

    private final BdvHandle bdvHandle;
    private final PlaneManager planeManager;

    /**
     * Adds custom behaviours to the BigDataViewer window referenced by bdvHandle
     * @param bdvHandle bdvHandle of the BigDataViewer window
     * @param planeManager Crosshair plane manager
     */
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

    private void installBehaviours() {
        final Behaviours behaviours = new Behaviours(new InputTriggerConfig());
        behaviours.install( bdvHandle.getTriggerbindings(), "accuracy" );

        bdvHandle.getViewerPanel().transformListeners().add( new bdv.viewer.TransformListener<AffineTransform3D>() {
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

    }
}
