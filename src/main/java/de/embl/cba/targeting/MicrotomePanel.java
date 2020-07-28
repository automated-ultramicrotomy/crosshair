package de.embl.cba.targeting;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SliderPanel;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.*;
import bdv.viewer.Interpolation;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.BrightnessUpdateListener;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.bdv.utils.sources.ARGBConvertedRealSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.LazyLabelsARGBConverter;
import de.embl.cba.tables.ij3d.UniverseUtils;
import de.embl.cba.tables.image.DefaultImageSourcesModel;
import de.embl.cba.tables.image.SourceAndMetadata;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.Segments3dView;
import de.embl.cba.tables.view.SegmentsBdvView;
import de.embl.cba.tables.view.TableRowsTableView;
import de.embl.cba.tables.view.combined.SegmentsTableBdvAnd3dViews;
import ij3d.Content;
import ij3d.ContentConstants;
import ij3d.Image3DUniverse;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.java3d.Transform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.vecmath.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.*;
import java.util.List;


// similar to mobie source panel - https://github.com/mobie/mobie-viewer-fiji/blob/master/src/main/java/de/embl/cba/mobie/ui/viewer/SourcesPanel.java

public class MicrotomePanel extends JPanel {

    private final MicrotomeManager microtomeManager;
    private final PlaneManager planeManager;
    private final BoundedValueDouble initialKnifeAngle;
    private final BoundedValueDouble initialTiltAngle;
    private final BoundedValueDouble knifeAngle;
    private final BoundedValueDouble tiltAngle;
    private final BoundedValueDouble rotationAngle;
    private final BoundedValueDouble rotationSolution;
    private final BoundedValueDouble cuttingDepth;
    private final Map<String, SliderPanelDouble> sliders;
    private final Map<String, JPanel> sliderPanels;
    private String firstTouch;
    private boolean validSolution;
    private double distanceToCut;

    JLabel firstTouchLabel;
    JLabel distanceToCutLabel;
    JLabel currentRotationLabel;
    JLabel currentKnifeLabel;
    JLabel currentTiltLabel;
    JLabel currentAngleKnifeTargetLabel;

    JButton enterMicrotomeMode;
    JButton exitMicrotomeMode;
    JButton enterCutting;
    JButton exitCutting;

    JPanel cuttingControls;
    JPanel currentSettings;
    private SavePanel savePanel;
    private PointsPanel pointsPanel;

    private boolean inCuttingMode;
    private boolean inMicrotomeMode;
    private JFrame parentFrame;

    public MicrotomePanel(MicrotomeManager microtomeManager, PlaneManager planeManager, PointsPanel pointsPanel) {
        this.microtomeManager = microtomeManager;
        this.pointsPanel = pointsPanel;
        sliders = new HashMap<>();
        sliderPanels = new HashMap<>();
        inCuttingMode = false;
        inMicrotomeMode = false;
        this.planeManager = planeManager;

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

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

        add(initialMicrotomeSetup);

        JPanel microtomeControls = new JPanel();
        microtomeControls.setLayout(new BoxLayout(microtomeControls, BoxLayout.PAGE_AXIS));

        microtomeControls.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Microtome Controls"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        JPanel toggleMicrotomeModePanel = new JPanel();
        toggleMicrotomeModePanel.setLayout(new GridLayout(1, 2));

        ActionListener blockListener = new blockListener();
        enterMicrotomeMode = new JButton("Enter Microtome Mode");
        enterMicrotomeMode.setActionCommand("initialise_block");
        enterMicrotomeMode.addActionListener(blockListener);
        toggleMicrotomeModePanel.add(enterMicrotomeMode);

        exitMicrotomeMode = new JButton("Exit Microtome Mode");
        exitMicrotomeMode.setActionCommand("exit_microtome_mode");
        exitMicrotomeMode.addActionListener(blockListener);
        toggleMicrotomeModePanel.add(exitMicrotomeMode);
        exitMicrotomeMode.setEnabled(false);

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

        add(microtomeControls);

        cuttingControls = new JPanel();
        cuttingControls.setLayout(new BoxLayout(cuttingControls, BoxLayout.PAGE_AXIS));
        cuttingControls.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Cutting Simulation"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        JPanel toggleCuttingModePanel = new JPanel();
        toggleCuttingModePanel.setLayout(new GridLayout(1, 2));
        enterCutting = new JButton("Enter Cutting Mode");
        enterCutting.setActionCommand("Cutting_mode");
        enterCutting.addActionListener(blockListener);
        toggleCuttingModePanel.add(enterCutting);
        enterCutting.setVisible(false);

        exitCutting = new JButton("Exit Cutting Mode");
        exitCutting.setActionCommand("exit_cutting_mode");
        exitCutting.addActionListener(blockListener);
        toggleCuttingModePanel.add(exitCutting);
        exitCutting.setEnabled(false);
        exitCutting.setVisible(false);

        cuttingControls.add(toggleCuttingModePanel);
        cuttingControls.add(Box.createVerticalStrut(5));

        // Cutting simulator
        CuttingListener cuttingListener = new CuttingListener();
        // min max arbitrary here, these are set when microtome mode is initialised, depends on final location of knife and
        // holder after scaling
        cuttingDepth =
                addSliderToPanel(cuttingControls, "Cutting Depth", 0, 100, 0, cuttingListener);

        disableSliders();
        cuttingControls.setVisible(false);
        add(cuttingControls);

        currentSettings = new JPanel();
        currentSettings.setLayout(new GridLayout(1, 2));
        currentSettings.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Current Settings"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        JPanel leftSettingsPanel = new JPanel();
        leftSettingsPanel.setLayout(new BoxLayout(leftSettingsPanel, BoxLayout.PAGE_AXIS));
        // TODO - round the distnace values
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

        currentSettings.add(leftSettingsPanel);
        currentSettings.add(rightSettingsPanel);
        currentSettings.setVisible(false);
        add(currentSettings);

//        Orientation of axes matches those in original blender file, object positions also match
//        Interactive transform setter in 3d viewer: https://github.com/fiji/3D_Viewer/blob/master/src/main/java/ij3d/gui/InteractiveTransformDialog.java
    }

    public void setSavePanel (SavePanel savePanel) {this.savePanel = savePanel;}
    public void setParentFrame(JFrame jFrame) {
        parentFrame = jFrame;
    }
    public void setValidSolution(boolean valid) {validSolution = valid;}
    public boolean getValidSolution () {return validSolution;}

    public BoundedValueDouble getKnifeAngle() {
        return knifeAngle;
    }
    public BoundedValueDouble getTiltAngle() {
        return tiltAngle;
    }
    public BoundedValueDouble getRotationAngle() {
        return rotationAngle;
    }
    public BoundedValueDouble getRotationSolutionAngle() {return rotationSolution;}

    public String getFirstTouch() {
        return firstTouch;
    }

    public double getDistanceToCut() {
        return distanceToCut;
    }

    public boolean checkMicrotomeMode () {return inMicrotomeMode;}

    public Map<String, SliderPanelDouble> getSliders (){return sliders;}

    public void setFirstTouch (String firstTouchVertex) {
        firstTouchLabel.setText("First Touch:    " + firstTouchVertex);
        firstTouch = firstTouchVertex;
    }

    public void setDistanceToCut (double distance) {
        distanceToCutLabel.setText("Distance to  cut:    " + distance+"");
        distanceToCut = distance;
        System.out.println(distance);
    }

    public void setRotationLabel (double rotation) {
        currentRotationLabel.setText("Rotation:    " + rotation+"");
    }

    public void setTiltLabel (double tilt) {
        currentTiltLabel.setText("Tilt:    " + tilt+"");
    }

    public void setKnifeLabel (double knife) {
        currentKnifeLabel.setText("Knife:    " + knife+"");
    }

    public void setCuttingRange (double min, double max) {
        cuttingDepth.setRange(min, max);
    }

    public void setKnifeTargetAngleLabel (double angle) {
        currentAngleKnifeTargetLabel.setText("Knife-Target Angle:    " + angle+"");
    }

//padName is total number of characters, will pad all to this length, so sliders line up
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

//                as here https://github.com/tischi/imagej-utils/blob/b7bdece786c1593969ec469916adf9737a7768bb/src/main/java/de/embl/cba/bdv/utils/BdvDialogs.java
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
            sliders.put(sliderName, s);
            sliderPanels.put(sliderName, sliderPanel);
        }

        refreshGui();
        return s;
    }

    private void refreshGui() {
        this.revalidate();
        this.repaint();
    }

    public void enableSliders () {
        // using setEnabled doesn't work with these bdv.tools style sliders, so we just change the visibility
        for (String sliderName : sliderPanels.keySet()) {
            if (sliderName != "Cutting Depth") {
                sliderPanels.get(sliderName).setVisible(true);
            }
        }
    }

    public void disableSliders () {
        for (String sliderName : sliderPanels.keySet()) {
            sliderPanels.get(sliderName).setVisible(false);
        }
    }

    // disable sliders except for given name
    public void disableSliders (String name) {
        for (String sliderName : sliderPanels.keySet()) {
            if (sliderName != name) {
                sliderPanels.get(sliderName).setVisible(false);
            }
        }
    }


    class blockListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("initialise_block")){
                if (planeManager.checkAllPlanesPointsDefined() & planeManager.getTrackPlane() == 0) {
                    enterMicrotomeMode.setEnabled(false);
                    exitMicrotomeMode.setEnabled(true);
                    enterCutting.setVisible(true);
                    exitCutting.setVisible(true);
                    microtomeManager.initialiseMicrotome(initialKnifeAngle.getCurrentValue(), initialTiltAngle.getCurrentValue());
                    cuttingControls.setVisible(true);
                    currentSettings.setVisible(true);
                    inMicrotomeMode = true;
                    savePanel.enableSaveSolution();
                    pointsPanel.activateMicrotomeButtons();
                    parentFrame.pack();
                } else {
                System.out.println("Some of: target plane, block plane, top left, top right, bottom left, bottom right aren't defined. Or you are currently tracking a plane");
            }
            } else if (e.getActionCommand().equals("exit_microtome_mode")) {
                exitMicrotomeMode();
            } else if (e.getActionCommand().equals("Cutting_mode")) {
                sliderPanels.get("Cutting Depth").setVisible(true);
                // Disable all other microtome sliders
                disableSliders("Cutting Depth");
                microtomeManager.initialiseCuttingPlane();
//                Set slider bounds
                microtomeManager.setCuttingBounds();
                cuttingDepth.setCurrentValue(0);
                enterCutting.setEnabled(false);
                exitCutting.setEnabled(true);
                inCuttingMode = true;
                parentFrame.pack();

            } else if (e.getActionCommand().equals("exit_cutting_mode")) {
                sliderPanels.get("Cutting Depth").setVisible(false);
                enableSliders();
                microtomeManager.removeCuttingPlane();
                inCuttingMode = false;
                enterCutting.setEnabled(true);
                exitCutting.setEnabled(false);
                parentFrame.pack();
            }
        }
    }

    public void exitMicrotomeMode () {
        enterMicrotomeMode.setEnabled(true);
        exitMicrotomeMode.setEnabled(false);
        enterCutting.setVisible(false);
        exitCutting.setVisible(false);
//                 if in cutting mode, also disable this
        if (inCuttingMode) {
            sliderPanels.get("Cutting Depth").setVisible(false);
            enableSliders();
            microtomeManager.removeCuttingPlane();
            inCuttingMode = false;
        }
        enterCutting.setEnabled(true);
        exitCutting.setEnabled(false);
        cuttingControls.setVisible(false);
        currentSettings.setVisible(false);
        microtomeManager.exitMicrotomeMode();
        inMicrotomeMode = false;
        savePanel.disableSaveSolution();
        pointsPanel.activateMicrotomeButtons();
        parentFrame.pack();
    }

    abstract class MicrotomeListener implements BoundedValueDouble.UpdateListener {
        public void setValues(BoundedValueDouble value, SliderPanelDouble slider) {}
    }

    class KnifeListener extends MicrotomeListener {

        private BoundedValueDouble knifeAngle;
        private SliderPanelDouble knifeSlider;

        public KnifeListener() {}

        public void setValues(BoundedValueDouble knifeAngle, SliderPanelDouble knifeSlider) {
            this.knifeAngle = knifeAngle;
            this.knifeSlider = knifeSlider;
        }

        @Override
        public void update() {
                knifeSlider.update();
                microtomeManager.setKnifeAngle(knifeAngle.getCurrentValue());
        }
    }

    class HolderBackListener extends MicrotomeListener {

        private BoundedValueDouble tiltAngle;
        private SliderPanelDouble tiltSlider;

        public HolderBackListener() {}

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

        public HolderFrontListener() {}

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

        public SolutionListener() {}

        public void setValues(BoundedValueDouble rotationSolution, SliderPanelDouble solutionSlider) {
            this.rotationSolution = rotationSolution;
            this.solutionSlider = solutionSlider;
        }

        @Override
        public void update() {
            solutionSlider.update();
            microtomeManager.setSolutionFromRotation(rotationSolution.getCurrentValue());
        }
    }

    class CuttingListener extends MicrotomeListener {

        private BoundedValueDouble cuttingDepth;
        private SliderPanelDouble cuttingSlider;

        public CuttingListener() {}

        public void setValues(BoundedValueDouble cuttingDepth, SliderPanelDouble cuttingSlider) {
            this.cuttingDepth = cuttingDepth;
            this.cuttingSlider = cuttingSlider;
        }

        @Override
        public void update() {
            cuttingSlider.update();
            microtomeManager.updateCut(cuttingDepth.getCurrentValue());
        }
    }

}

