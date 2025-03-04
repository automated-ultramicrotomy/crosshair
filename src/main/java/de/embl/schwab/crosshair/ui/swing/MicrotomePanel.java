package de.embl.schwab.crosshair.ui.swing;

import bdv.util.*;
import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.microtome.Cutting;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.solution.SolutionsCalculator;
import ij.IJ;
import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Class for UI Panels controlling the microtome display
 */
public class MicrotomePanel extends CrosshairPanel {

    private static final Logger logger = LoggerFactory.getLogger(MicrotomePanel.class);

    private final String cuttingDepthString = "Cutting Depth";
    private final String initialKnifeString = "Initial Knife";
    private final String initialTiltString = "Initial Tilt";
    private final String solutionRotationString = "Solution Rotation";
    private final String enterMicrotomeModeCommand = "enter_microtome_mode";
    private final String exitMicrotomeModeCommand = "exit_microtome_mode";
    private final String enterCuttingModeCommand = "enter_cutting_mode";
    private final String exitCuttingModeCommand = "exit_cutting_mode";

    private MicrotomeManager microtomeManager;
    private PlaneManager planeManager;

    // BoundedValueDouble for sliders in microtome panel
    private BoundedValueDouble initialKnifeAngle;
    private BoundedValueDouble initialTiltAngle;
    private BoundedValueDouble knifeAngle;
    private BoundedValueDouble tiltAngle;
    private BoundedValueDouble rotationAngle;
    private BoundedValueDouble rotationSolution;
    private BoundedValueDouble cuttingDepth;

    private Map<String, SliderPanelDouble> sliderPanelsDouble;
    private Map<String, JPanel> sliderPanels;

    private JLabel firstTouchLabel;
    private JLabel distanceToCutLabel;
    private JLabel currentRotationLabel;
    private JLabel currentKnifeLabel;
    private JLabel currentTiltLabel;
    private JLabel currentAngleKnifeTargetLabel;

    private JButton enterMicrotomeModeButton;
    private JButton exitMicrotomeModeButton;
    private JButton enterCuttingModeButton;
    private JButton exitCuttingModeButton;

    private JPanel cuttingControlsPanel;
    private JPanel cuttingUnitsPanel;
    private JPanel currentSettingsPanel;
    private PlanePanel planePanel;
    private SavePanel savePanel;
    private OtherPanel otherPanel;
    private VertexAssignmentPanel vertexAssignmentPanel;

    private CrosshairFrame crosshairFrame;

    private int displayDecimalPlaces;
    private String unit;

    public MicrotomePanel() {}

    /**
     * Initialise panel from settings in main Crosshair UI
     * @param crosshairFrame main crosshair UI
     */
    public void initialisePanel( CrosshairFrame crosshairFrame ) {
        this.crosshairFrame = crosshairFrame;
        microtomeManager = crosshairFrame.getCrosshair().getMicrotomeManager();
        otherPanel = crosshairFrame.getPointsPanel();
        vertexAssignmentPanel = crosshairFrame.getVertexAssignmentPanel();
        planeManager = crosshairFrame.getCrosshair().getPlaneManager();
        planePanel = crosshairFrame.getPlanePanel();
        savePanel = crosshairFrame.getSavePanel();
        unit = crosshairFrame.getCrosshair().getUnit();

        displayDecimalPlaces = 4;

        sliderPanels = new HashMap<>();
        sliderPanelsDouble = new HashMap<>();

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        addMicrotomeSetupPanel(this);
        addMicrotomeControlsPanel(this);
        addCuttingControlsPanel(this);
        addCurrentSettingsPanel(this);
    }

    public BoundedValueDouble getRotationSolutionAngle() {
        return rotationSolution;
    }

    private void setFirstTouchLabel(String firstTouchVertex) {
        firstTouchLabel.setText("First Touch:    " + firstTouchVertex);
    }

    private void setDistanceToCutLabel(double distance) {
        distanceToCutLabel.setText("Distance to  cut:    " + Precision.round(distance, displayDecimalPlaces) +" " + unit + " ");
    }

    private void setRotationLabel(double rotation) {
        currentRotationLabel.setText("Rotation:    " + Precision.round(rotation, displayDecimalPlaces) +"\u00B0 ");
    }

    private void setTiltLabel(double tilt) {
        currentTiltLabel.setText("Tilt:    " + Precision.round(tilt, displayDecimalPlaces) +"\u00B0  ");
    }

    private void setKnifeLabel(double knife) {
        currentKnifeLabel.setText("Knife:    " + Precision.round(knife, displayDecimalPlaces) +"\u00B0  ");
    }

    private void setKnifeTargetAngleLabel(double angle) {
        currentAngleKnifeTargetLabel.setText("Knife-Target Angle:    " +
                Precision.round(angle, displayDecimalPlaces) +"\u00B0  ");
    }

    private void addMicrotomeSetupPanel(JPanel panel) {
        JPanel initialMicrotomeSetup = new JPanel();
        initialMicrotomeSetup.setLayout(new BoxLayout(initialMicrotomeSetup, BoxLayout.PAGE_AXIS));

        initialMicrotomeSetup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Microtome Setup"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        // Initial knife angle
        initialKnifeAngle =
                addSliderToPanel(initialMicrotomeSetup, initialKnifeString, -30, 30, 0);

        // Initial tilt angle
        initialTiltAngle =
                addSliderToPanel(initialMicrotomeSetup, initialTiltString, -20, 20, 0);

        panel.add(initialMicrotomeSetup);
    }

    private void addMicrotomeControlsPanel(JPanel panel) {
        JPanel microtomeControls = new JPanel();
        microtomeControls.setLayout(new BoxLayout(microtomeControls, BoxLayout.PAGE_AXIS));

        microtomeControls.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Microtome Controls"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        JPanel toggleMicrotomeModePanel = new JPanel();
        toggleMicrotomeModePanel.setLayout(new GridLayout(1, 2));

        ActionListener microtomeModeListener = new microtomeModeListener();
        enterMicrotomeModeButton = new JButton("Enter Microtome Mode");
        enterMicrotomeModeButton.setActionCommand( enterMicrotomeModeCommand );
        enterMicrotomeModeButton.addActionListener(microtomeModeListener);
        toggleMicrotomeModePanel.add(enterMicrotomeModeButton);
        planePanel.addButtonAffectedByTracking( Crosshair.target, enterMicrotomeModeButton );
        planePanel.addButtonAffectedByTracking( Crosshair.block, enterMicrotomeModeButton );

        exitMicrotomeModeButton = new JButton("Exit Microtome Mode");
        exitMicrotomeModeButton.setActionCommand( exitMicrotomeModeCommand );
        exitMicrotomeModeButton.addActionListener(microtomeModeListener);
        toggleMicrotomeModePanel.add(exitMicrotomeModeButton);
        exitMicrotomeModeButton.setEnabled(false);

        microtomeControls.add(toggleMicrotomeModePanel);
        microtomeControls.add(Box.createVerticalStrut(5));

        // Knife Rotation slider
        MicrotomeListener knifeListener = new KnifeListener();
        knifeAngle = addSliderToPanel(
                microtomeControls,
                "Knife",
                -30,
                30,
                0,
                knifeListener
        );

        // Arc Tilt slider
        MicrotomeListener holderBackListener = new HolderBackListener();
        tiltAngle = addSliderToPanel(
                microtomeControls,
                "Tilt",
                -20,
                20,
                0,
                holderBackListener
        );

        // Holder Rotation slider
        MicrotomeListener holderFrontListener = new HolderFrontListener();
        rotationAngle = addSliderToPanel(
                microtomeControls,
                "Rotation",
                -180,
                180,
                0,
                holderFrontListener
        );

        // Rotation solution slider
        SolutionListener solutionListener = new SolutionListener();
        rotationSolution = addSliderToPanel(
                microtomeControls,
                solutionRotationString,
                -180,
                180,
                0,
                solutionListener
        );

        panel.add(microtomeControls);
    }

    private void addCuttingControlsPanel(JPanel panel) {
        cuttingControlsPanel = new JPanel();
        cuttingControlsPanel.setLayout(new BoxLayout(cuttingControlsPanel, BoxLayout.PAGE_AXIS));
        cuttingControlsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Cutting Simulation"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        ActionListener cuttingModeListener = new cuttingModeListener();
        JPanel toggleCuttingModePanel = new JPanel();
        toggleCuttingModePanel.setLayout(new GridLayout(1, 2));
        enterCuttingModeButton = new JButton("Enter Cutting Mode");
        enterCuttingModeButton.setActionCommand( enterCuttingModeCommand );
        enterCuttingModeButton.addActionListener(cuttingModeListener);
        toggleCuttingModePanel.add(enterCuttingModeButton);
        enterCuttingModeButton.setVisible(false);

        exitCuttingModeButton = new JButton("Exit Cutting Mode");
        exitCuttingModeButton.setActionCommand( exitCuttingModeCommand );
        exitCuttingModeButton.addActionListener(cuttingModeListener);
        toggleCuttingModePanel.add(exitCuttingModeButton);
        exitCuttingModeButton.setEnabled(false);
        exitCuttingModeButton.setVisible(false);

        cuttingControlsPanel.add(toggleCuttingModePanel);
        cuttingControlsPanel.add(Box.createVerticalStrut(5));

        // Cutting simulator
        CuttingListener cuttingListener = new CuttingListener();
        // min max arbitrary here, these are set when microtome mode is initialised, depends on final location of knife and
        // holder after scaling
        cuttingDepth = addSliderToPanel(
                cuttingControlsPanel,
                cuttingDepthString,
                0,
                100,
                0,
                cuttingListener
        );

        cuttingUnitsPanel = new JPanel();
        cuttingUnitsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cuttingUnitsPanel.add(new JLabel(unit, SwingConstants.LEFT));
        cuttingUnitsPanel.setVisible(false);
        cuttingControlsPanel.add(cuttingUnitsPanel);

        disableSliders();
        cuttingControlsPanel.setVisible(false);
        panel.add(cuttingControlsPanel);
    }

    private void addCurrentSettingsPanel(JPanel panel) {
        currentSettingsPanel = new JPanel();
        currentSettingsPanel.setLayout(new GridLayout(1, 2));
        currentSettingsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Current Settings"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        JPanel leftSettingsPanel = new JPanel();
        leftSettingsPanel.setLayout(new BoxLayout(leftSettingsPanel, BoxLayout.PAGE_AXIS));
        currentRotationLabel = new JLabel("Rotation:");
        currentTiltLabel = new JLabel("Tilt:");
        currentKnifeLabel = new JLabel("Knife:");
        currentAngleKnifeTargetLabel = new JLabel("Knife-Target Angle:");
        leftSettingsPanel.add(currentKnifeLabel);
        leftSettingsPanel.add(currentTiltLabel);
        leftSettingsPanel.add(currentRotationLabel);
        leftSettingsPanel.add(currentAngleKnifeTargetLabel);

        JPanel rightSettingsPanel = new JPanel();
        rightSettingsPanel.setLayout(new BoxLayout(rightSettingsPanel, BoxLayout.PAGE_AXIS));
        firstTouchLabel = new JLabel("First Touch:");
        distanceToCutLabel = new JLabel("Distance to cut:");
        rightSettingsPanel.add(firstTouchLabel);
        rightSettingsPanel.add(distanceToCutLabel);

        currentSettingsPanel.add(leftSettingsPanel);
        currentSettingsPanel.add(rightSettingsPanel);
        currentSettingsPanel.setVisible(false);
        panel.add(currentSettingsPanel);
    }

    private BoundedValueDouble addSliderToPanel(
            JPanel panel, String sliderName, double min, double max, double currentValue,
            MicrotomeListener updateListener) {

        final BoundedValueDouble sliderValue =
                new BoundedValueDouble(
                        min,
                        max,
                        currentValue);
        SliderPanelDouble slider = createSlider(panel, sliderName, sliderValue);
        updateListener.setValues(sliderValue, slider);
        sliderValue.setUpdateListener(updateListener);

        return sliderValue;
    }

    private BoundedValueDouble addSliderToPanel(
            JPanel panel, String sliderName, double min, double max, double currentValue) {

        // as here https://github.com/K-Meech/crosshair/blob/b7bdece786c1593969ec469916adf9737a7768bb/src/main/java/de/embl/cba/bdv/utils/BdvDialogs.java
        final BoundedValueDouble sliderValue =
                new BoundedValueDouble(
                        min,
                        max,
                        currentValue);
        createSlider(panel, sliderName, sliderValue);
        return sliderValue;
    }

    private SliderPanelDouble createSlider(JPanel panel, String sliderName, BoundedValueDouble sliderValue) {
        double spinnerStepSize = 1;
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.LINE_AXIS));
        JLabel sliderLabel = new JLabel(sliderName);
        sliderLabel.setPreferredSize(new Dimension(100, 20));
        sliderPanel.add(sliderLabel);
        final SliderPanelDouble s = new SliderPanelDouble("", sliderValue, spinnerStepSize);
        s.setNumColummns(7);
        s.setDecimalFormat("####.####");
        sliderPanel.add(s);
        panel.add(sliderPanel);

        // Don't want to disable initial angles, these aren't directly tied to microtome movements
        if (!sliderName.equals( initialKnifeString ) & !sliderName.equals( initialTiltString )) {
            sliderPanels.put(sliderName, sliderPanel);
        }

        sliderPanelsDouble.put( sliderName, s );

        refreshGui();
        return s;
    }

    private void refreshGui() {
        this.revalidate();
        this.repaint();
    }

    private void enableSliders() {
        // using setEnabled doesn't work with these bdv.tools style sliders, so we just change the visibility
        for (String sliderName : sliderPanels.keySet()) {
            if (!sliderName.equals( cuttingDepthString )) {
                sliderPanels.get(sliderName).setVisible(true);
            }
        }
    }

    private void disableSliders() {
        for (String sliderName : sliderPanels.keySet()) {
            sliderPanels.get(sliderName).setVisible(false);
        }
    }

    /**
     * Disable sliders except for given name
     * @param name name of slider
     */
    private void disableSliders(String name) {
        for (String sliderName : sliderPanels.keySet()) {
            if (!sliderName.equals(name)) {
                sliderPanels.get(sliderName).setVisible(false);
            }
        }
    }


    class microtomeModeListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals( enterMicrotomeModeCommand )) {
                enterMicrotomeMode();
            } else if (e.getActionCommand().equals( exitMicrotomeModeCommand )) {
                exitMicrotomeMode();
            }
        }
    }

    class cuttingModeListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals( enterCuttingModeCommand )) {
                enterCuttingMode();
            } else if (e.getActionCommand().equals( exitCuttingModeCommand )) {
                exitCuttingMode();
            }
        }
    }

    private void commitInitialAngleSliders() {
        sliderPanelsDouble.get( initialKnifeString ).commitSpinnerEdits();
        sliderPanelsDouble.get( initialTiltString ).commitSpinnerEdits();
    }

    public void commitSolutionRotationSlider() {
        sliderPanelsDouble.get( solutionRotationString ).commitSpinnerEdits();
    }

    private void enterMicrotomeMode() {
        if ( !microtomeManager.allCrosshairPlanesPointsDefined() ) {
            IJ.log("Some of: target plane, block plane, top left, top right, bottom left, bottom right aren't defined.");
            return;
        }

        enterMicrotomeModeButton.setEnabled(false);
        exitMicrotomeModeButton.setEnabled(true);
        enterCuttingModeButton.setVisible(true);
        exitCuttingModeButton.setVisible(true);

        // ensure any editing of the slider text fields is properly committed
        commitInitialAngleSliders();
        double initialKnifeAngle = this.initialKnifeAngle.getCurrentValue();
        double initialTiltAngle = this.initialTiltAngle.getCurrentValue();

        try {
            microtomeManager.enterMicrotomeMode(initialKnifeAngle, initialTiltAngle);
        } catch (MicrotomeManager.IncorrectMicrotomeConfiguration e) {
            logger.error(e.getMessage(), e);
        }

        enableSliders();
        knifeAngle.setCurrentValue(initialKnifeAngle);
        tiltAngle.setCurrentValue(initialTiltAngle);
        rotationAngle.setCurrentValue(0);
        vertexAssignmentPanel.disableButtons();
        cuttingControlsPanel.setVisible(true);
        currentSettingsPanel.setVisible(true);
        savePanel.enableSaveSolution();
        planePanel.disableAllTrackingButtons();
        otherPanel.activateMicrotomeButtons();
        planeManager.setPointMode( false );
        planeManager.setVertexMode( false );
        crosshairFrame.pack();
    }

    public void exitMicrotomeMode() {
        enterMicrotomeModeButton.setEnabled(true);
        exitMicrotomeModeButton.setEnabled(false);
        enterCuttingModeButton.setVisible(false);
        exitCuttingModeButton.setVisible(false);
        // if in cutting mode, also disable this
        if (microtomeManager.isCuttingModeActive()) {
            exitCuttingMode();
        }
        cuttingControlsPanel.setVisible(false);
        currentSettingsPanel.setVisible(false);
        knifeAngle.setCurrentValue(0);
        tiltAngle.setCurrentValue(0);
        rotationAngle.setCurrentValue(0);
        try {
            microtomeManager.exitMicrotomeMode();
        } catch (MicrotomeManager.IncorrectMicrotomeConfiguration e) {
            logger.error(e.getMessage(), e);
        }
        planePanel.enableAllTrackingButtons();

        // inactivate sliders
        disableSliders();
        vertexAssignmentPanel.enableButtons();
        savePanel.disableSaveSolution();
        otherPanel.activateMicrotomeButtons();
        crosshairFrame.pack();
    }

    private void enterCuttingMode() {
        sliderPanels.get( cuttingDepthString ).setVisible(true);
        cuttingUnitsPanel.setVisible(true);
        // Disable all other microtome sliders
        disableSliders( cuttingDepthString );
        try {
            microtomeManager.enterCuttingMode();
            Cutting cutting = microtomeManager.getCutting();
            cuttingDepth.setRange(cutting.getCuttingDepthMin(), cutting.getCuttingDepthMax());
            cuttingDepth.setCurrentValue(0);
            enterCuttingModeButton.setEnabled(false);
            exitCuttingModeButton.setEnabled(true);
        } catch (MicrotomeManager.IncorrectMicrotomeConfiguration e) {
            logger.error(e.getMessage(), e);
        }
        crosshairFrame.pack();

    }

    private void exitCuttingMode() {
        sliderPanels.get( cuttingDepthString ).setVisible(false);
        cuttingUnitsPanel.setVisible(false);
        enableSliders();
        try {
            microtomeManager.exitCuttingMode();
            enterCuttingModeButton.setEnabled(true);
            exitCuttingModeButton.setEnabled(false);
        } catch (MicrotomeManager.IncorrectMicrotomeConfiguration e) {
            logger.error(e.getMessage(), e);
        }
        crosshairFrame.pack();
    }

    abstract class MicrotomeListener implements BoundedValueDouble.UpdateListener {
        public void setValues(BoundedValueDouble value, SliderPanelDouble slider) {}
    }

    /**
     * Listener to update knife angle / labels with the slider
     */
    class KnifeListener extends MicrotomeListener {

        private BoundedValueDouble knifeAngle;
        private SliderPanelDouble knifeSlider;

        KnifeListener() {}

        public void setValues(BoundedValueDouble knifeAngle, SliderPanelDouble knifeSlider) {
            this.knifeAngle = knifeAngle;
            this.knifeSlider = knifeSlider;
        }

        @Override
        public void update() {
            knifeSlider.update();
            try {
                double knifeAngleDouble = knifeAngle.getCurrentValue();
                microtomeManager.setKnife( knifeAngleDouble );
                setKnifeLabel( knifeAngleDouble );
                setKnifeTargetAngleLabel( microtomeManager.getMicrotome().getAngleKnifeTarget() );
            } catch (MicrotomeManager.IncorrectMicrotomeConfiguration e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Listener to update tilt angle / labels with the slider
     */
    class HolderBackListener extends MicrotomeListener {

        private BoundedValueDouble tiltAngle;
        private SliderPanelDouble tiltSlider;

        HolderBackListener() {}

        public void setValues(BoundedValueDouble tiltAngle, SliderPanelDouble tiltSlider) {
            this.tiltAngle = tiltAngle;
            this.tiltSlider = tiltSlider;
        }

        @Override
        public void update() {
            tiltSlider.update();
            try {
                double tiltAngleDouble = tiltAngle.getCurrentValue();
                microtomeManager.setTilt(tiltAngleDouble);
                setTiltLabel( tiltAngleDouble );
                setRotationLabel( microtomeManager.getMicrotome().getRotation() );
                setKnifeTargetAngleLabel( microtomeManager.getMicrotome().getAngleKnifeTarget() );
            } catch (MicrotomeManager.IncorrectMicrotomeConfiguration e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Listener to update rotation angle / labels with the slider
     */
    class HolderFrontListener extends MicrotomeListener {

        private BoundedValueDouble rotationAngle;
        private SliderPanelDouble rotationSlider;

        HolderFrontListener() {}

        public void setValues(BoundedValueDouble rotationAngle, SliderPanelDouble rotationSlider) {
            this.rotationAngle = rotationAngle;
            this.rotationSlider = rotationSlider;
        }

        @Override
        public void update() {
            rotationSlider.update();
            try {
                double rotationAngleDouble = rotationAngle.getCurrentValue();
                microtomeManager.setRotation(rotationAngleDouble);
                setRotationLabel(rotationAngleDouble);
                setTiltLabel( microtomeManager.getMicrotome().getTilt() );
                setKnifeTargetAngleLabel( microtomeManager.getMicrotome().getAngleKnifeTarget() );
            } catch (MicrotomeManager.IncorrectMicrotomeConfiguration e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Listener to update solution angle / labels with the slider
     */
    class SolutionListener extends MicrotomeListener {

        private BoundedValueDouble rotationSolution;
        private SliderPanelDouble solutionSlider;

        SolutionListener() {}

        public void setValues(BoundedValueDouble rotationSolution, SliderPanelDouble solutionSlider) {
            this.rotationSolution = rotationSolution;
            this.solutionSlider = solutionSlider;
        }

        @Override
        public void update() {
            solutionSlider.update();
            try {
                double rotationAngleDouble = rotationSolution.getCurrentValue();
                microtomeManager.setSolution( rotationAngleDouble );
                SolutionsCalculator solutions = microtomeManager.getSolutions();

                // Still set to value, even if not valid solution, so microtome moves / maxes out limit -
                // makes for a smoother transition
                rotationAngle.setCurrentValue(rotationAngleDouble);
                tiltAngle.setCurrentValue( solutions.getSolutionTilt() );
                knifeAngle.setCurrentValue( solutions.getSolutionKnife() );

                if ( !solutions.isValidSolution() ) {
                    // Display first touch as nothing, and distance as 0
                    setFirstTouchLabel("");
                    setDistanceToCutLabel(0);
                } else {
                    setFirstTouchLabel( solutions.getSolutionFirstTouchVertexPoint().toString() );
                    setDistanceToCutLabel( solutions.getDistanceToCut() );
                }
            } catch (MicrotomeManager.IncorrectMicrotomeConfiguration e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Listener to update cutting depth with the slider
     */
    class CuttingListener extends MicrotomeListener {

        private BoundedValueDouble cuttingDepth;
        private SliderPanelDouble cuttingSlider;

        CuttingListener() {}

        public void setValues(BoundedValueDouble cuttingDepth, SliderPanelDouble cuttingSlider) {
            this.cuttingDepth = cuttingDepth;
            this.cuttingSlider = cuttingSlider;
        }

        @Override
        public void update() {
            cuttingSlider.update();
            try {
                microtomeManager.setCuttingDepth( cuttingDepth.getCurrentValue() );
            } catch (MicrotomeManager.IncorrectMicrotomeConfiguration e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

}

