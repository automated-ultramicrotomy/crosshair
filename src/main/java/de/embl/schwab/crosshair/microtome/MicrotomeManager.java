package de.embl.schwab.crosshair.microtome;

import bdv.util.BdvStackSource;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.solution.Solution;
import de.embl.schwab.crosshair.solution.SolutionsCalculator;
import de.embl.schwab.crosshair.ui.swing.MicrotomePanel;
import de.embl.schwab.crosshair.ui.swing.VertexAssignmentPanel;
import ij.IJ;
import ij3d.Content;
import ij3d.Image3DUniverse;

/**
 * Main class to manage interaction with the ultramicrotome
 */
public class MicrotomeManager {

    private final PlaneManager planeManager;
    private MicrotomePanel microtomePanel;
    private VertexAssignmentPanel vertexAssignmentPanel;

    private boolean microtomeModeActive;
    private boolean cuttingModeActive;

    private Microtome microtome;
    private MicrotomeSetup microtomeSetup;
    private SolutionsCalculator solutions;
    private Cutting cutting;

    private String unit;

    /**
     * Create a microtome manager
     * @param planeManager plane manager
     * @param universe universe of the 3D viewer
     * @param imageContent image content displayed in 3D viewer
     * @param bdvStackSource BigDataViewer stack source
     * @param unit unit of distance e.g. micrometre
     */
    public MicrotomeManager(PlaneManager planeManager, Image3DUniverse universe,
                            Content imageContent, BdvStackSource bdvStackSource, String unit) {

        this.planeManager = planeManager;
        microtomeModeActive = false;
        cuttingModeActive = false;

        this.microtome = new Microtome(universe, planeManager, bdvStackSource, imageContent);
        this.microtomeSetup = new MicrotomeSetup(microtome);
        this.solutions = new SolutionsCalculator(microtome);
        this.cutting = new Cutting(microtome);
        this.unit = unit;

    }

    public Solution getCurrentSolution() {
        return solutions.getSolution( unit );
    }

    public void setMicrotomePanel(MicrotomePanel microtomePanel) {
        this.microtomePanel = microtomePanel;
    }

    public void setVertexAssignmentPanel(VertexAssignmentPanel vertexAssignmentPanel) {
        this.vertexAssignmentPanel = vertexAssignmentPanel;
    }

    public boolean isCuttingModeActive() {
        return cuttingModeActive;
    }

    public boolean isMicrotomeModeActive() { return microtomeModeActive; }

    public boolean isValidSolution() {
        return solutions.isValidSolution();
    }

    /**
     * Enter microtome mode with the given initial angles.
     * @param initialKnifeAngle initial knife angle (degrees)
     * @param initialTiltAngle initial tilt angle (degrees)
     */
    public void enterMicrotomeMode(double initialKnifeAngle, double initialTiltAngle) {
        if (!microtomeModeActive) {
            microtomeModeActive = true;
            microtome.setInitialKnifeAngle(initialKnifeAngle);
            microtome.setInitialTiltAngle(initialTiltAngle);

            microtomeSetup.initialiseMicrotome();
        } else {
            IJ.log("Microtome mode already active");
        }
    }

    public void exitMicrotomeMode(){
        if (microtomeModeActive) {
            microtomeModeActive = false;
            microtome.resetMicrotome();
        } else {
            IJ.log("Microtome mode already active");
        }
    }

    public void setKnife(double angleDegrees) {
        if (microtomeModeActive) {
            microtome.setKnife(angleDegrees);
            microtomePanel.setKnifeLabel( angleDegrees );
            microtomePanel.setKnifeTargetAngleLabel( microtome.getAngleKnifeTarget() );
        } else {
            IJ.log("Microtome mode inactive");
        }
    }

    public void setTilt(double angleDegrees) {
        if (microtomeModeActive) {
            microtome.setTilt(angleDegrees);
            microtomePanel.setTiltLabel( angleDegrees );
            microtomePanel.setRotationLabel( microtome.getRotation() );
            microtomePanel.setKnifeTargetAngleLabel( microtome.getAngleKnifeTarget() );
        } else {
            IJ.log("Microtome mode inactive");
        }
    }

    public void setRotation(double angleDegrees) {
        if (microtomeModeActive) {
            microtome.setRotation(angleDegrees);
            microtomePanel.setRotationLabel(angleDegrees);
            microtomePanel.setTiltLabel( microtome.getTilt() );
            microtomePanel.setKnifeTargetAngleLabel( microtome.getAngleKnifeTarget() );
        } else {
            IJ.log("Microtome mode inactive");
        }
    }

    public void setSolution(double rotationDegrees) {
        if (microtomeModeActive) {
            microtomePanel.getRotationAngle().setCurrentValue(rotationDegrees);
            solutions.setSolutionFromRotation(rotationDegrees);

            // Still set to value, even if not valid solution, so microtome moves / maxes out limit -
            // makes for a smoother transition
            microtomePanel.getTiltAngle().setCurrentValue( solutions.getSolutionTilt() );
            microtomePanel.getKnifeAngle().setCurrentValue( solutions.getSolutionKnife() );

            if ( !solutions.isValidSolution() ) {
                // Display first touch as nothing, and distance as 0
                microtomePanel.setFirstTouchLabel("");
                microtomePanel.setDistanceToCutLabel(0);
            } else {
                microtomePanel.setFirstTouchLabel( solutions.getSolutionFirstTouchVertexPoint().toString() );
                microtomePanel.setDistanceToCutLabel( solutions.getDistanceToCut() );
            }
        } else {
            IJ.log("Microtome mode inactive");
        }
    }

    public void enterCuttingMode () {
        if (microtomeModeActive & !cuttingModeActive) {
            cutting.initialiseCuttingPlane();
            cuttingModeActive = true;
            microtomePanel.setCuttingRange( cutting.getCuttingDepthMin(), cutting.getCuttingDepthMax() );
        } else {
            IJ.log("Microtome mode inactive, or cutting mode already active");
        }
    }

    public void setCuttingDepth (double cuttingDepth) {
        if (cuttingModeActive) {
            cutting.updateCut(cuttingDepth);
        } else {
            IJ.log("Cutting mode inactive");
        }
    }

    public void exitCuttingMode() {
        if (cuttingModeActive) {
            cuttingModeActive = false;
            cutting.removeCuttingPlane();
        } else {
            IJ.log("Cutting mode inactive");
        }
    }
}
