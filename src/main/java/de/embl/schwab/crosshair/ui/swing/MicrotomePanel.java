package de.embl.schwab.crosshair.ui.swing;

import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.*;
import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.points.VertexPoint;
import ij.IJ;
import net.imglib2.RealPoint;
import org.apache.commons.math3.util.Precision;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;


// similar to mobie source panel - https://github.com/mobie/mobie-viewer-fiji/blob/master/src/main/java/de/embl/cba/mobie/ui/viewer/SourcesPanel.java

public class MicrotomePanel extends CrosshairPanel {

    private MicrotomeManager microtomeManager;
    private PlaneManager planeManager;

    private BoundedValueDouble initialKnifeAngle;
    private BoundedValueDouble initialTiltAngle;
    private BoundedValueDouble knifeAngle;
    private BoundedValueDouble tiltAngle;
    private BoundedValueDouble rotationAngle;
    private BoundedValueDouble rotationSolution;
    private BoundedValueDouble cuttingDepth;

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

    public void initialisePanel( CrosshairFrame crosshairFrame ) {
        this.crosshairFrame = crosshairFrame;
        microtomeManager = crosshairFrame.getMicrotomeManager();
        otherPanel = crosshairFrame.getPointsPanel();
        vertexAssignmentPanel = crosshairFrame.getVertexAssignmentPanel();
        planeManager = crosshairFrame.getPlaneManager();
        planePanel = crosshairFrame.getPlanePanel();
        savePanel = crosshairFrame.getSavePanel();
        unit = crosshairFrame.getUnit();

        displayDecimalPlaces = 4;

        sliderPanels = new HashMap<>();

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        addMicrotomeSetupPanel(this);
        addMicrotomeControlsPanel(this);
        addCuttingControlsPanel(this);
        addCurrentSettingsPanel(this);
    }

    public BoundedValueDouble getKnifeAngle() {
        return knifeAngle;
    }

    public BoundedValueDouble getTiltAngle() {
        return tiltAngle;
    }

    public BoundedValueDouble getRotationAngle() {
        return rotationAngle;
    }

    public BoundedValueDouble getRotationSolutionAngle() {
        return rotationSolution;
    }

    public void setFirstTouchLabel (String firstTouchVertex) {
        firstTouchLabel.setText("First Touch:    " + firstTouchVertex);
    }

    public void setDistanceToCutLabel (double distance) {
        distanceToCutLabel.setText("Distance to  cut:    " + Precision.round(distance, displayDecimalPlaces) +" " + unit + " ");
    }

    public void setRotationLabel (double rotation) {
        currentRotationLabel.setText("Rotation:    " + Precision.round(rotation, displayDecimalPlaces) +"\u00B0 ");
    }

    public void setTiltLabel (double tilt) {
        currentTiltLabel.setText("Tilt:    " + Precision.round(tilt, displayDecimalPlaces) +"\u00B0  ");
    }

    public void setKnifeLabel (double knife) {
        currentKnifeLabel.setText("Knife:    " + Precision.round(knife, displayDecimalPlaces) +"\u00B0  ");
    }

    public void setKnifeTargetAngleLabel (double angle) {
        currentAngleKnifeTargetLabel.setText("Knife-Target Angle:    " + Precision.round(angle, displayDecimalPlaces) +"\u00B0  ");
    }

    public void setCuttingRange (double min, double max) {
        cuttingDepth.setRange(min, max);
    }

    private void addMicrotomeSetupPanel (JPanel panel) {
        JPanel initialMicrotomeSetup = new JPanel();
        initialMicrotomeSetup.setLayout(new BoxLayout(initialMicrotomeSetup, BoxLayout.PAGE_AXIS));

        initialMicrotomeSetup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Microtome Setup"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        // Initial knife angle
        initialKnifeAngle =
                addSliderToPanel(initialMicrotomeSetup, "Initial Knife", -30, 30, 0);

        // Initial tilt angle
        initialTiltAngle =
                addSliderToPanel(initialMicrotomeSetup, "Initial Tilt", -20, 20, 0);

        panel.add(initialMicrotomeSetup);
    }

    private void addMicrotomeControlsPanel (JPanel panel) {
        JPanel microtomeControls = new JPanel();
        microtomeControls.setLayout(new BoxLayout(microtomeControls, BoxLayout.PAGE_AXIS));

        microtomeControls.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Microtome Controls"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        JPanel toggleMicrotomeModePanel = new JPanel();
        toggleMicrotomeModePanel.setLayout(new GridLayout(1, 2));

        ActionListener microtomeModeListener = new microtomeModeListener();
        enterMicrotomeModeButton = new JButton("Enter Microtome Mode");
        enterMicrotomeModeButton.setActionCommand("enter_microtome_mode");
        enterMicrotomeModeButton.addActionListener(microtomeModeListener);
        toggleMicrotomeModePanel.add(enterMicrotomeModeButton);
        planePanel.addButtonAffectedByTracking( Crosshair.target, enterMicrotomeModeButton );
        planePanel.addButtonAffectedByTracking( Crosshair.block, enterMicrotomeModeButton );

        exitMicrotomeModeButton = new JButton("Exit Microtome Mode");
        exitMicrotomeModeButton.setActionCommand("exit_microtome_mode");
        exitMicrotomeModeButton.addActionListener(microtomeModeListener);
        toggleMicrotomeModePanel.add(exitMicrotomeModeButton);
        exitMicrotomeModeButton.setEnabled(false);

        microtomeControls.add(toggleMicrotomeModePanel);
        microtomeControls.add(Box.createVerticalStrut(5));

        // Knife Rotation
        MicrotomeListener knifeListener = new KnifeListener();
        knifeAngle = addSliderToPanel(microtomeControls, "Knife",  -30, 30, 0, knifeListener);

        // Arc Tilt
        MicrotomeListener holderBackListener = new HolderBackListener();
        tiltAngle =
                addSliderToPanel(microtomeControls, "Tilt", -20, 20, 0, holderBackListener);

        // Holder Rotation
        MicrotomeListener holderFrontListener = new HolderFrontListener();
        rotationAngle =
                addSliderToPanel(microtomeControls, "Rotation", -180, 180, 0, holderFrontListener);

        // Rotation solution
        SolutionListener solutionListener = new SolutionListener();
        rotationSolution =
                addSliderToPanel(microtomeControls, "Solution Rotation", -180, 180, 0, solutionListener);

        panel.add(microtomeControls);
    }

    private void addCuttingControlsPanel (JPanel panel) {
        cuttingControlsPanel = new JPanel();
        cuttingControlsPanel.setLayout(new BoxLayout(cuttingControlsPanel, BoxLayout.PAGE_AXIS));
        cuttingControlsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Cutting Simulation"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        ActionListener cuttingModeListener = new cuttingModeListener();
        JPanel toggleCuttingModePanel = new JPanel();
        toggleCuttingModePanel.setLayout(new GridLayout(1, 2));
        enterCuttingModeButton = new JButton("Enter Cutting Mode");
        enterCuttingModeButton.setActionCommand("enter_cutting_mode");
        enterCuttingModeButton.addActionListener(cuttingModeListener);
        toggleCuttingModePanel.add(enterCuttingModeButton);
        enterCuttingModeButton.setVisible(false);

        exitCuttingModeButton = new JButton("Exit Cutting Mode");
        exitCuttingModeButton.setActionCommand("exit_cutting_mode");
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
        cuttingDepth =
                addSliderToPanel(cuttingControlsPanel, "Cutting Depth", 0, 100, 0, cuttingListener);

        cuttingUnitsPanel = new JPanel();
        cuttingUnitsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cuttingUnitsPanel.add(new JLabel(unit, SwingConstants.LEFT));
        cuttingUnitsPanel.setVisible(false);
        cuttingControlsPanel.add(cuttingUnitsPanel);

        disableSliders();
        cuttingControlsPanel.setVisible(false);
        panel.add(cuttingControlsPanel);
    }

    private void addCurrentSettingsPanel (JPanel panel) {
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

    private BoundedValueDouble addSliderToPanel(JPanel panel, String sliderName, double min, double max, double currentValue,
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

    private BoundedValueDouble addSliderToPanel(JPanel panel, String sliderName, double min, double max, double currentValue) {

        // as here https://github.com/K-Meech/crosshair/blob/b7bdece786c1593969ec469916adf9737a7768bb/src/main/java/de/embl/cba/bdv/utils/BdvDialogs.java
        final BoundedValueDouble sliderValue =
                new BoundedValueDouble(
                        min,
                        max,
                        currentValue);
        createSlider(panel, sliderName, sliderValue);
        return sliderValue;
    }

    private SliderPanelDouble createSlider (JPanel panel, String sliderName, BoundedValueDouble sliderValue) {
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

        //        Don't want to disable initial angles, these aren't directly tied to microtome movements
        if (!sliderName.equals("Initial Knife") & !sliderName.equals("Initial Tilt")) {
            sliderPanels.put(sliderName, sliderPanel);
        }

        refreshGui();
        return s;
    }

    private void refreshGui() {
        this.revalidate();
        this.repaint();
    }

    private void enableSliders () {
        // using setEnabled doesn't work with these bdv.tools style sliders, so we just change the visibility
        for (String sliderName : sliderPanels.keySet()) {
            if (!sliderName.equals("Cutting Depth")) {
                sliderPanels.get(sliderName).setVisible(true);
            }
        }
    }

    private void disableSliders () {
        for (String sliderName : sliderPanels.keySet()) {
            sliderPanels.get(sliderName).setVisible(false);
        }
    }

    // disable sliders except for given name
    private void disableSliders (String name) {
        for (String sliderName : sliderPanels.keySet()) {
            if (!sliderName.equals(name)) {
                sliderPanels.get(sliderName).setVisible(false);
            }
        }
    }


    class microtomeModeListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("enter_microtome_mode")) {
                enterMicrotomeMode();
            } else if (e.getActionCommand().equals("exit_microtome_mode")) {
                exitMicrotomeMode();
            }
        }
    }

    class cuttingModeListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("enter_cutting_mode")) {
                enterCuttingMode();
            } else if (e.getActionCommand().equals("exit_cutting_mode")) {
                exitCuttingMode();
            }
        }
    }

    private void enterMicrotomeMode () {
        if ( checkAllCrosshairPlanesPointsDefined() ) {
            enterMicrotomeModeButton.setEnabled(false);
            exitMicrotomeModeButton.setEnabled(true);
            enterCuttingModeButton.setVisible(true);
            exitCuttingModeButton.setVisible(true);
            double initialKnifeAngle = this.initialKnifeAngle.getCurrentValue();
            double initialTiltAngle = this.initialTiltAngle.getCurrentValue();
            microtomeManager.enterMicrotomeMode(initialKnifeAngle, initialTiltAngle);
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
        } else {
            IJ.log("Some of: target plane, block plane, top left, top right, bottom left, bottom right aren't defined.");
        }
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
        microtomeManager.exitMicrotomeMode();
        planePanel.enableAllTrackingButtons();

        // inactivate sliders
        disableSliders();
        vertexAssignmentPanel.enableButtons();
        savePanel.disableSaveSolution();
        otherPanel.activateMicrotomeButtons();
        crosshairFrame.pack();
    }

    private void enterCuttingMode () {
        sliderPanels.get("Cutting Depth").setVisible(true);
        cuttingUnitsPanel.setVisible(true);
        // Disable all other microtome sliders
        disableSliders("Cutting Depth");
        microtomeManager.enterCuttingMode();
        cuttingDepth.setCurrentValue(0);
        enterCuttingModeButton.setEnabled(false);
        exitCuttingModeButton.setEnabled(true);
        crosshairFrame.pack();

    }

    private void exitCuttingMode () {
        sliderPanels.get("Cutting Depth").setVisible(false);
        cuttingUnitsPanel.setVisible(false);
        enableSliders();
        microtomeManager.exitCuttingMode();
        enterCuttingModeButton.setEnabled(true);
        exitCuttingModeButton.setEnabled(false);
        crosshairFrame.pack();
    }

    private boolean checkAllCrosshairPlanesPointsDefined() {
        boolean targetExists = planeManager.checkNamedPlaneExistsAndOrientationIsSet( Crosshair.target );
        boolean blockExists = planeManager.checkNamedPlaneExistsAndOrientationIsSet( Crosshair.block );

        boolean allVerticesExist = false;
        if ( blockExists ) {
            allVerticesExist = true;
            Map<VertexPoint, RealPoint> assignedVertices =  planeManager.getVertexDisplay( Crosshair.block ).getAssignedVertices();

            for ( VertexPoint vertexPoint: VertexPoint.values() ) {
                if ( !assignedVertices.containsKey( vertexPoint ) ) {
                    allVerticesExist = false;
                    break;
                }
            }
        }

        if (targetExists & blockExists & allVerticesExist) {
            return true;
        } else {
            return false;
        }

    }

    abstract class MicrotomeListener implements BoundedValueDouble.UpdateListener {
        public void setValues(BoundedValueDouble value, SliderPanelDouble slider) {}
    }

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
                microtomeManager.setKnife( knifeAngle.getCurrentValue() );
        }
    }

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
                microtomeManager.setTilt(tiltAngle.getCurrentValue());
        }
    }

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
                microtomeManager.setRotation(rotationAngle.getCurrentValue());
        }
    }

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
            microtomeManager.setSolution( rotationSolution.getCurrentValue() );
        }
    }

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
            microtomeManager.setCuttingDepth( cuttingDepth.getCurrentValue() );
        }
    }

}

