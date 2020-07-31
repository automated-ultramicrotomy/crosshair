package de.embl.cba.crosshair.microtome;

import bdv.util.BdvStackSource;
import customnode.CustomMesh;
import customnode.CustomTriangleMesh;
import customnode.Tube;
import de.embl.cba.crosshair.PlaneManager;
import de.embl.cba.crosshair.io.STLResourceLoader;
import de.embl.cba.crosshair.ui.swing.MicrotomePanel;
import de.embl.cba.crosshair.ui.swing.VertexAssignmentPanel;
import de.embl.cba.crosshair.utils.GeometryUtils;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.RealPoint;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.*;

import javax.swing.*;
import java.util.*;

import static de.embl.cba.crosshair.utils.GeometryUtils.*;
import static java.lang.Math.*;

//TODO - add all sliders up here?
//TODO - some variable for block has been initialised at least once before can control it with sliders

public class MicrotomeManager extends JPanel {

    private final Image3DUniverse universe;
    private final PlaneManager planeManager;
    private final BdvStackSource bdvStackSource;
    private Map<String, CustomMesh> microtomeSTLs;
    private final Content imageContent;
    private MicrotomePanel microtomePanel;
    private VertexAssignmentPanel vertexAssignmentPanel;

    private boolean microtomeModeActive;
    private boolean cuttingModeActive;

    private Microtome microtome;
    private MicrotomeSetup microtomeSetup;
    private Solutions solutions;
    private Cutting cutting;

    public MicrotomeManager(PlaneManager planeManager, Image3DUniverse universe, Content imageContent, BdvStackSource bdvStackSource) {

        this.planeManager = planeManager;
        this.universe = universe;
        this.imageContent = imageContent;
        this.bdvStackSource = bdvStackSource;
        microtomeModeActive = false;

        this.microtome = new Microtome();
        this.microtomeSetup = new MicrotomeSetup(microtome);
        this.solutions = new Solutions();
        this.cutting = new Cutting();

    }


    public void setMicrotomePanel(MicrotomePanel microtomePanel) {
        this.microtomePanel = microtomePanel;
    }
    public void setVertexAssignmentPanel (VertexAssignmentPanel vertexAssignmentPanel) {
        this.vertexAssignmentPanel = vertexAssignmentPanel;
    }

    public boolean isCuttingModeActive() {
        return cuttingModeActive;
    }

    public void enterMicrotomeMode (double initialKnifeAngle, double initialTiltAngle) {
        if (planeManager.checkAllPlanesPointsDefined() & planeManager.getTrackPlane() == 0) {
            microtomeModeActive = true;
            microtome.setInitialKnifeAngle(initialKnifeAngle);
            microtome.setInitialTiltAngle(initialTiltAngle);

            microtomeSetup.initialiseMicrotome();

            microtomePanel.enableSliders();
            microtomePanel.getKnifeAngle().setCurrentValue(initialKnifeAngle);
            microtomePanel.getTiltAngle().setCurrentValue(initialTiltAngle);
            microtomePanel.getRotationAngle().setCurrentValue(0);
            vertexAssignmentPanel.disableButtons();
        } else {
        System.out.println("Some of: target plane, block plane, top left, top right, bottom left, bottom right aren't defined. Or you are currently tracking a plane");
        }
    }

    public void exitMicrotomeMode (){
        if (microtomeModeActive) {
            microtomeModeActive = false;

            microtome.resetMicrotome();

            microtomePanel.getKnifeAngle().setCurrentValue(0);
            microtomePanel.getTiltAngle().setCurrentValue(0);
            microtomePanel.getRotationAngle().setCurrentValue(0);

            // inactivate sliders
            microtomePanel.disableSliders();
            vertexAssignmentPanel.enableButtons();
        } else {
            System.out.println("Microtome mode already active");
        }
    }

    public void setKnife (double angleDegrees) {
        if (microtomeModeActive) {
            microtome.setKnife(angleDegrees);
        } else {
            System.out.println("Microtome mode inactive");
        }
    }

    public void setTilt (double angleDegrees) {
        if (microtomeModeActive) {
            microtome.setTilt(angleDegrees);
        } else {
            System.out.println("Microtome mode inactive");
        }
    }

    public void setRotation (double angleDegrees) {
        if (microtomeModeActive) {
            microtome.setRotation(angleDegrees);
        } else {
            System.out.println("Microtome mode inactive");
        }
    }

    public boolean isValidSolution () {
        return solutions.isValidSolution();
    }

    public void setSolution (double rotationDegrees) {
        if (microtomeModeActive) {
            microtomePanel.getRotationAngle().setCurrentValue(rotationDegrees);
            solutions.setSolutionFromRotation(rotationDegrees);

            // Still set to value, even if not valid solution, so microtome moves / maxes out limit - makes for a smoother transition
            microtomePanel.getTiltAngle().setCurrentValue( solutions.getSolutionTilt() );
            microtomePanel.getKnifeAngle().setCurrentValue( solutions.getSolutionKnife() );

            if ( !solutions.isValidSolution() ) {
                // Display first touch as nothing, and distance as 0
                microtomePanel.setFirstTouch("");
                microtomePanel.setDistanceToCut(0);
            } else {
                microtomePanel.setFirstTouch( solutions.getSolutionFirstTouchName() );
                microtomePanel.setDistanceToCut( solutions.getDistanceToCut() );
            }
        } else {
            System.out.println("Microtome mode inactive");
        }
    }

    public void enterCuttingMode () {
        if (microtomeModeActive & !cuttingModeActive) {
            cutting.initialiseCuttingPlane();
            microtomePanel.setCuttingRange( cutting.getCuttingDepthMin(), cutting.getCuttingDepthMax() );
            cuttingModeActive = true;
        } else {
            System.out.println("Microtome mode inactive, or cutting mode already active");
        }
    }

    public void setCuttingDepth (double cuttingDepth) {
        if (cuttingModeActive) {
            cutting.updateCut(cuttingDepth);
        } else {
            System.out.println("Cutting mode inactive");
        }
    }

    public void exitCuttingMode() {
        if (cuttingModeActive) {
            cuttingModeActive = false;
            cutting.removeCuttingPlane();
        } else {
            System.out.println("Cutting mode inactive");
        }
    }


}
