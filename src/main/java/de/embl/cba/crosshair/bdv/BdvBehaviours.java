package de.embl.cba.crosshair.bdv;

import bdv.util.BdvHandle;
import de.embl.cba.crosshair.microtome.MicrotomeManager;
import de.embl.cba.crosshair.PlaneManager;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.vecmath.Vector3d;

import javax.swing.*;
import java.util.ArrayList;

import static de.embl.cba.crosshair.utils.GeometryUtils.fitPlaneToPoints;

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
            if (!microtomeManager.isMicrotomeModeActive() & planeManager.getTrackPlane() == 0) {
                planeManager.addRemoveCurrentPositionPoints();
            } else {
                IJ.log("Microtome mode must be inactive, and not tracking plane, to change points");
            }
        }, "add point", "P" );

        behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
            if (planeManager.getTrackPlane() == 0 & !microtomeManager.isMicrotomeModeActive()) {
                if (planeManager.getBlockVertices().size() > 0) {
                    int result = JOptionPane.showConfirmDialog(null, "If you fit the block plane to points, you will lose all current vertex points", "Are you sure?",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (result == JOptionPane.YES_OPTION) {
                        planeManager.removeAllBlockVertices();
                        ArrayList<Vector3d> planeDefinition = fitPlaneToPoints(planeManager.getPoints());
                        planeManager.updatePlane(planeDefinition.get(0), planeDefinition.get(1), "block");
                    }
                } else {
                    ArrayList<Vector3d> planeDefinition = fitPlaneToPoints(planeManager.getPoints());
                    planeManager.updatePlane(planeDefinition.get(0), planeDefinition.get(1), "block");
                }
            } else {
                IJ.log("Can only fit to points, when not tracking a plane and microtome mode is inactive");
            }
        }, "fit to points", "K" );

        behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
            if (!microtomeManager.isMicrotomeModeActive() & planeManager.getTrackPlane() == 0 & planeManager.checkNamedPlaneExists("block")) {
                planeManager.addRemoveCurrentPositionBlockVertices();
            } else {
                IJ.log("Microtome mode must be inactive, block plane must exit, and not tracking plane, to change points");
            }
        }, "add block vertex", "V" );

        behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
            planeManager.toggleSelectedVertexCurrentPosition();
        }, "select point", "button1" );

    }
}
