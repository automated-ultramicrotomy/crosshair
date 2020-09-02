package de.embl.schwab.crosshair.bdv;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.popup.BdvPopupMenus;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.PlaneManager;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;
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
        } else if (planeManager.getTrackPlane() != 0) {
            IJ.log("Can't change points when tracking a plane");
        } else if (!planeManager.checkNamedPlaneExists("block")) {
            IJ.log("Block plane doesn't exist - vertices must lie on this plane!");
        } else {
            planeManager.addRemoveCurrentPositionBlockVertices();
        }
    }

    private void addPointBehaviour() {
        if (microtomeManager.isMicrotomeModeActive()) {
            IJ.log("Can't change points when in microtome mode");
        } else if (planeManager.getTrackPlane() != 0) {
            IJ.log("Can't change points when tracking a plane");
        } else {
            planeManager.addRemoveCurrentPositionPointsToFitPlane();
        }
    }

    private void addFitToPointsBehaviour() {
        if (microtomeManager.isMicrotomeModeActive()) {
            IJ.log("Can't fit to points when in microtome mode");
        } else if (planeManager.getTrackPlane() == 2) {
            IJ.log("Can't fit to points when tracking block plane");
        } else {
            if (planeManager.getPointsToFitPlane().size() >= 3) {
                if (planeManager.getBlockVertices().size() > 0) {
                    int result = JOptionPane.showConfirmDialog(null, "If you fit the block plane to points, you will lose all current vertex points. Continue?", "Are you sure?",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (result == JOptionPane.YES_OPTION) {
                        planeManager.removeAllBlockVertices();
                        ArrayList<Vector3d> planeDefinition = GeometryUtils.fitPlaneToPoints(planeManager.getPointsToFitPlane());
                        planeManager.updatePlane(planeDefinition.get(0), planeDefinition.get(1), "block");
                    }
                } else {
                    ArrayList<Vector3d> planeDefinition = GeometryUtils.fitPlaneToPoints(planeManager.getPointsToFitPlane());
                    planeManager.updatePlane(planeDefinition.get(0), planeDefinition.get(1), "block");
                }
            } else {
                IJ.log ("Need at least 3 points to fit plane");
            }
        }
    }

    private void installBehaviours() {
        final Behaviours behaviours = new Behaviours(new InputTriggerConfig());
        behaviours.install(bdvHandle.getTriggerbindings(), "target");

        bdvHandle.getViewerPanel().addTransformListener(new TransformListener<AffineTransform3D>() {
            @Override
            public void transformChanged(AffineTransform3D affineTransform3D) {
                if ( planeManager.getTrackPlane() == 1 )
                {
                    planeManager.updatePlaneOnTransformChange(affineTransform3D, "target");
                } else if (planeManager.getTrackPlane() == 2) {
                    planeManager.updatePlaneOnTransformChange(affineTransform3D, "block");
                }
            }
        });

        behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
            if (planeManager.getPointMode() == 0 & planeManager.getVertexMode() == 0) {
                planeManager.toggleSelectedVertexCurrentPosition();
            } else if (planeManager.getPointMode() == 1) {
                addPointBehaviour();
            } else if (planeManager.getVertexMode() == 1) {
                addVertexBehaviour();
            }
        }, "Left Click behaviours", "button1" );

        BdvPopupMenus.addAction(bdvHandle, "Toggle Point Mode", ( x, y ) ->
        {
            if (!microtomeManager.isMicrotomeModeActive()) {
                if (planeManager.getPointMode() == 0) {
                    if (planeManager.getVertexMode() == 1) {
                        planeManager.setVertexMode(0);
                    }
                    planeManager.setPointMode(1);
                } else {
                    planeManager.setPointMode(0);
                }
            } else {
                IJ.log("Can't change points while in microtome mode");
            }
        });

        BdvPopupMenus.addAction(bdvHandle, "Toggle Vertex Mode", ( x, y ) ->
        {
            if (!microtomeManager.isMicrotomeModeActive()) {
                if (planeManager.getVertexMode() == 0) {
                    if (planeManager.getPointMode() == 1) {
                        planeManager.setPointMode(0);
                    }
                    planeManager.setVertexMode(1);
                } else {
                    planeManager.setVertexMode(0);
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
