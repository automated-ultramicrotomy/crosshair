package de.embl.schwab.crosshair.microtome;

import bdv.util.BdvStackSource;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.solution.Solution;
import de.embl.schwab.crosshair.solution.SolutionsCalculator;
import ij3d.Content;
import ij3d.Image3DUniverse;

/**
 * Main class to manage interaction with the ultramicrotome
 */
public class MicrotomeManager {

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

    public SolutionsCalculator getSolutions() { return solutions; }

    public Microtome getMicrotome() { return microtome; }

    public Cutting getCutting() { return cutting; }

    public boolean isCuttingModeActive() {
        return cuttingModeActive;
    }

    public boolean isMicrotomeModeActive() { return microtomeModeActive; }

    public boolean isValidSolution() {
        return solutions.isValidSolution();
    }

    /**
     * Exception for when the microtome is in the wrong configuration for a specific action. E.g. trying to set the
     * cutting depth when the cutting mode isn't active.
     */
    public static class IncorrectMicrotomeConfiguration extends Exception {
        public IncorrectMicrotomeConfiguration(String errorMessage) {
            super(errorMessage);
        }
    }

    /**
     * Enter microtome mode with the given initial angles.
     * @param initialKnifeAngle initial knife angle (degrees)
     * @param initialTiltAngle initial tilt angle (degrees)
     */
    public void enterMicrotomeMode(double initialKnifeAngle, double initialTiltAngle) throws IncorrectMicrotomeConfiguration {
        if (microtomeModeActive) {
            throw new IncorrectMicrotomeConfiguration("Microtome mode already active");
        }

        microtomeModeActive = true;
        microtome.setInitialKnifeAngle(initialKnifeAngle);
        microtome.setInitialTiltAngle(initialTiltAngle);

        microtomeSetup.initialiseMicrotome();
    }

    public void exitMicrotomeMode() throws IncorrectMicrotomeConfiguration {
        if (!microtomeModeActive) {
            throw new IncorrectMicrotomeConfiguration("Microtome mode already inactive");
        }

        microtomeModeActive = false;
        microtome.resetMicrotome();
    }

    public void setKnife(double angleDegrees) throws IncorrectMicrotomeConfiguration {
        if (!microtomeModeActive) {
            throw new IncorrectMicrotomeConfiguration("Microtome mode inactive");
        }

        microtome.setKnife(angleDegrees);
    }

    public void setTilt(double angleDegrees) throws IncorrectMicrotomeConfiguration {
        if (!microtomeModeActive) {
            throw new IncorrectMicrotomeConfiguration("Microtome mode inactive");
        }

        microtome.setTilt(angleDegrees);
    }

    public void setRotation(double angleDegrees) throws IncorrectMicrotomeConfiguration {
        if (!microtomeModeActive) {
            throw new IncorrectMicrotomeConfiguration("Microtome mode inactive");
        }

        microtome.setRotation(angleDegrees);
    }

    public void setSolution(double rotationDegrees) throws IncorrectMicrotomeConfiguration {
        if (!microtomeModeActive) {
            throw new IncorrectMicrotomeConfiguration("Microtome mode inactive");
        }

        solutions.setSolutionFromRotation(rotationDegrees);
    }

    public void enterCuttingMode() throws IncorrectMicrotomeConfiguration {
        if (!microtomeModeActive | cuttingModeActive) {
            throw new IncorrectMicrotomeConfiguration(
                    "Microtome mode inactive, or cutting mode already active"
            );
        }

        cutting.initialiseCuttingPlane();
        cuttingModeActive = true;
    }

    public void setCuttingDepth(double cuttingDepth) throws IncorrectMicrotomeConfiguration {
        if (!cuttingModeActive) {
            throw new IncorrectMicrotomeConfiguration("Cutting mode inactive");
        }

        cutting.updateCut(cuttingDepth);
    }

    public void exitCuttingMode() throws IncorrectMicrotomeConfiguration {
        if (!cuttingModeActive) {
            throw new IncorrectMicrotomeConfiguration("Cutting mode inactive");
        }

        cuttingModeActive = false;
        cutting.removeCuttingPlane();
    }
}
