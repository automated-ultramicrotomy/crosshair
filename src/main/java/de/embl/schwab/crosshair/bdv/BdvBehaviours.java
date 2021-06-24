package de.embl.schwab.crosshair.bdv;

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

public class BdvBehaviours {

    private BdvHandle bdvHandle;
    private PlaneManager planeManager;
    private MicrotomeManager microtomeManager;

    public BdvBehaviours (BdvHandle bdvHandle, PlaneManager planeManager, MicrotomeManager microtomeManager) {
        this.bdvHandle = bdvHandle;
        this.planeManager = planeManager;
        this.microtomeManager = microtomeManager;

        installBehaviours();
    }

    private void addVertexBehaviour() {
        if (microtomeManager.isMicrotomeModeActive()) {
            IJ.log("Can't change points when in microtome mode");
        } else if ( planeManager.isTrackingPlane() ) {
            IJ.log("Can't change points when tracking a plane");
        } else if ( !planeManager.checkNamedPlaneExistsAndOrientationIsSet( Crosshair.block )) {
            IJ.log("Block plane doesn't exist - vertices must lie on this plane!");
        } else {
            Plane blockPlane = planeManager.getPlane( Crosshair.block );
            planeManager.getVertexDisplay( Crosshair.block ).addOrRemoveCurrentPositionFromVertices( blockPlane );
        }
    }

    private void addPointBehaviour() {
        if (microtomeManager.isMicrotomeModeActive()) {
            IJ.log("Can't change points when in microtome mode");
        } else if ( planeManager.isTrackingPlane() ) {
            IJ.log("Can't change points when tracking a plane");
        } else {
            if ( !planeManager.checkNamedPlaneExists( Crosshair.block ) ) {
                planeManager.addBlockPlane( Crosshair.block );
            }
            planeManager.getPointsToFitPlaneDisplay( Crosshair.block ).addOrRemoveCurrentPositionFromPointsToFitPlane();
        }
    }

    private void addFitToPointsBehaviour() {
        if ( microtomeManager.isMicrotomeModeActive() ) {
            IJ.log("Can't fit to points when in microtome mode");
        } else if ( planeManager.isTrackingPlane() && planeManager.getTrackedPlaneName().equals( Crosshair.block ) ) {
            IJ.log("Can't fit to points when tracking block plane");
        } else if ( !planeManager.checkNamedPlaneExists( Crosshair.block ) ) {
            IJ.log("Block plane doesn't exist" );
        } else {
            BlockPlane plane = planeManager.getBlockPlane( Crosshair.block );
            if ( plane.getVertexDisplay().getVertices().size() > 0 ) {
                int result = JOptionPane.showConfirmDialog(null, "If you fit the block plane to points, you will lose all current vertex points. Continue?", "Are you sure?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    plane.getVertexDisplay().removeAllVertices();
                    planeManager.fitToPoints( Crosshair.block );
                }
            } else {
                planeManager.fitToPoints( Crosshair.block );
            }
        }
    }

    private void installBehaviours() {
        final Behaviours behaviours = new Behaviours(new InputTriggerConfig());
        behaviours.install( bdvHandle.getTriggerbindings(), Crosshair.target );

        bdvHandle.getViewerPanel().addTransformListener(new bdv.viewer.TransformListener<AffineTransform3D>() {
            @Override
            public void transformChanged(AffineTransform3D affineTransform3D) {
                if ( planeManager.isTrackingPlane() ) {
                    planeManager.updatePlaneOnTransformChange( affineTransform3D, planeManager.getTrackedPlaneName() );
                }
            }
        });

        behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
            if ( !planeManager.isInPointMode() & !planeManager.isInVertexMode() ) {
                if ( planeManager.checkNamedPlaneExists( Crosshair.block ) ) {
                    planeManager.getVertexDisplay( Crosshair.block ).toggleSelectedVertexCurrentPosition();
                }
            } else if ( planeManager.isInPointMode() ) {
                addPointBehaviour();
            } else if ( planeManager.isInVertexMode() ) {
                addVertexBehaviour();
            }
        }, "Left Click behaviours", "button1" );

        BdvPopupMenus.addAction(bdvHandle, "Toggle Point Mode", ( x, y ) ->
        {
            if ( !microtomeManager.isMicrotomeModeActive() ) {
                if ( !planeManager.isInPointMode() ) {
                    if ( planeManager.isInVertexMode() ) {
                        planeManager.setVertexMode( false );
                    }
                    planeManager.setPointMode( true );
                } else {
                    planeManager.setPointMode( false );
                }
            } else {
                IJ.log("Can't change points while in microtome mode");
            }
        });

        BdvPopupMenus.addAction(bdvHandle, "Toggle Vertex Mode", ( x, y ) ->
        {
            if (!microtomeManager.isMicrotomeModeActive()) {
                if ( !planeManager.isInVertexMode() ) {
                    if ( planeManager.isInPointMode() ) {
                        planeManager.setPointMode( false );
                    }
                    planeManager.setVertexMode( true );
                } else {
                    planeManager.setVertexMode( false );
                }
            } else {
                IJ.log("Can't change vertices while in microtome mode");
            }
        });

        BdvPopupMenus.addAction(bdvHandle, "Fit To Points", ( x, y ) ->
        {
                addFitToPointsBehaviour();
        });

    }
}
