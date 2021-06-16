package de.embl.schwab.crosshair.ui.swing;

import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.*;
import de.embl.schwab.crosshair.PlaneManager;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import ij.IJ;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.MouseInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// similar to mobie source panel - https://github.com/mobie/mobie-viewer-fiji/blob/master/src/main/java/de/embl/cba/mobie/ui/viewer/SourcesPanel.java

    public class PlanePanel extends CrosshairPanel {

        private PlaneManager planeManager;
        private Map<String, JButton> trackingButtons;
        private MicrotomePanel microtomePanel;
        private SavePanel savePanel;
        private ArrayList<JButton> buttonsAffectedByBlockTracking;
        private ArrayList<JButton> buttonsAffectedByTargetTracking;

        public PlanePanel() {}

        public void initialisePanel( CrosshairFrame crosshairFrame ) {
            planeManager = crosshairFrame.getPlaneManager();
            savePanel = crosshairFrame.getSavePanel();
            microtomePanel = crosshairFrame.getMicrotomePanel();
            trackingButtons = new HashMap<>();
            buttonsAffectedByTargetTracking = new ArrayList<>();
            buttonsAffectedByBlockTracking = new ArrayList<>();

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Planes"),
                    BorderFactory.createEmptyBorder(5,5,5,5)));

            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            addPlaneToPanel("target");
            addPlaneToPanel("block");
        }

        private void addColorButton(JPanel panel, int[] buttonDimensions, String planeName) {
            JButton colorButton;
            colorButton = new JButton("C");

            colorButton.setPreferredSize(
                    new Dimension(buttonDimensions[0], buttonDimensions[1]));

            colorButton.addActionListener(e -> {
                Color colour = JColorChooser.showDialog(null, "", null);

                if (colour == null) return;

                if ( planeManager.checkNamedPlaneExists( planeName ) ) {
                    planeManager.setPlaneColour( planeName, colour );
                } else {
                    IJ.log( planeName + " plane not initialised" );
                }

            });

            if (planeName.equals("target")) {
                buttonsAffectedByTargetTracking.add(colorButton);
            } else if (planeName.equals("block")) {
                buttonsAffectedByBlockTracking.add(colorButton);
            }
            panel.add(colorButton);
        }

        private void addGOTOButton(JPanel panel, int[] buttonDimensions, String planeName) {
            JButton goToButton;
            goToButton = new JButton("GO TO");

            goToButton.setPreferredSize(
                    new Dimension(2*buttonDimensions[0], buttonDimensions[1]));

            goToButton.addActionListener(e -> {
                if (planeManager.checkNamedPlaneExists(planeName)) {
                    planeManager.moveViewToNamedPlane(planeName);
                } else {
                    IJ.log("Plane not initialised");
                }
            });

            if (planeName.equals("target")) {
                buttonsAffectedByTargetTracking.add(goToButton);
            } else if (planeName.equals("block")) {
                buttonsAffectedByBlockTracking.add(goToButton);
            }

            panel.add(goToButton);
        }

        private void addTrackingButton(JPanel panel, int[] buttonDimensions, String planeName) {
            JButton trackButton;
            trackButton = new JButton("TRACK");

            trackButton.setPreferredSize(
                    new Dimension(2*buttonDimensions[0], buttonDimensions[1]));

            trackButton.addActionListener(e -> {
                    if (planeName == "block") {
                        toggleBlockTracking(trackButton);
                    } else if (planeName == "target") {
                        toggleTargetTracking(trackButton);
                    }
            });

            trackingButtons.put(planeName, trackButton);
            if (planeName.equals("target")) {
                buttonsAffectedByBlockTracking.add(trackButton);
            } else if (planeName.equals("block")) {
                buttonsAffectedByTargetTracking.add(trackButton);
            }
            panel.add(trackButton);
        }

        private void disableButtonsAffectedByBlockTracking () {
            for (JButton button : buttonsAffectedByBlockTracking) {
                button.setEnabled(false);
            }
        }

        private void enableButtonsAffectedByBlockTracking () {
            for (JButton button : buttonsAffectedByBlockTracking) {
                button.setEnabled(true);
            }
        }

        private void disableButtonsAffectedByTargetTracking () {
            for (JButton button : buttonsAffectedByTargetTracking) {
                button.setEnabled(false);
            }
        }

        private void enableButtonsAffectedByTargetTracking () {
            for (JButton button : buttonsAffectedByTargetTracking) {
                button.setEnabled(true);
            }
        }

        private void toggleBlockTracking (JButton trackButton) {
            if (planeManager.getTrackPlane() == 0) {
                // check if there are already vertex points
                if (planeManager.getBlockVertices().size() > 0) {
                    int result = JOptionPane.showConfirmDialog(null, "If you track the block plane, you will lose all current vertex points. Continue?", "Are you sure?",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (result == JOptionPane.YES_OPTION) {
                        planeManager.removeAllBlockVertices();
                        planeManager.setTrackPlane(2);
                        planeManager.updatePlaneCurrentView("block");
                        trackButton.setBackground(new Color (255, 0,0));
                        microtomePanel.disableEnterMicrotomeMode();
                        savePanel.disableLoadSettings();
                        savePanel.disableSaveSettings();
                        disableButtonsAffectedByBlockTracking();
                    }
                } else {
                    planeManager.setTrackPlane(2);
                    planeManager.updatePlaneCurrentView("block");
                    trackButton.setBackground(new Color (255, 0,0));
                    microtomePanel.disableEnterMicrotomeMode();
                    savePanel.disableLoadSettings();
                    savePanel.disableSaveSettings();
                    disableButtonsAffectedByBlockTracking();
                }
            } else if (planeManager.getTrackPlane() == 2) {
                planeManager.setTrackPlane(0);
                trackButton.setBackground(null);
                microtomePanel.enableEnterMicrotomeButton();
                savePanel.enableLoadSettings();
                savePanel.enableSaveSettings();
                enableButtonsAffectedByBlockTracking();
            }
        }

        private void toggleTargetTracking (JButton trackButton) {
            if (planeManager.getTrackPlane() == 0) {
                planeManager.setTrackPlane(1);
                planeManager.updatePlaneCurrentView("target");
                trackButton.setBackground(new Color (255, 0,0));
                microtomePanel.disableEnterMicrotomeMode();
                savePanel.disableLoadSettings();
                savePanel.disableSaveSettings();
                disableButtonsAffectedByTargetTracking();
            } else if (planeManager.getTrackPlane() == 1) {
                planeManager.setTrackPlane(0);
                trackButton.setBackground(null);
                microtomePanel.enableEnterMicrotomeButton();
                savePanel.enableLoadSettings();
                savePanel.enableSaveSettings();
                enableButtonsAffectedByTargetTracking();
            }
        }

        private void addVisibilityButton (JPanel panel, int[] buttonDimensions, String planeName) {
            JButton visbilityButton;
            visbilityButton = new JButton("V");

            visbilityButton.setPreferredSize(
                    new Dimension(buttonDimensions[0], buttonDimensions[1]));

            visbilityButton.addActionListener(e -> {
                if ( planeManager.checkNamedPlaneExists( planeName ) ) {
                    planeManager.togglePlaneVisbility( planeName );
                } else {
                    IJ.log(planeName + " plane not initialised" );
                }
            });

            if (planeName.equals("target")) {
                buttonsAffectedByTargetTracking.add(visbilityButton);
            } else if (planeName.equals("block")) {
                buttonsAffectedByBlockTracking.add(visbilityButton);
            }

            panel.add(visbilityButton);
        }

        private void addPlaneToPanel(String planeName) {

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            panel.add(Box.createHorizontalGlue());

            JLabel sourceNameLabel = new JLabel(planeName);
            sourceNameLabel.setHorizontalAlignment(SwingUtilities.CENTER);

            int[] buttonDimensions = new int[]{50, 30};

            panel.add(sourceNameLabel);

            addColorButton(panel, buttonDimensions, planeName);
            addTransparencyButton(panel, buttonDimensions, planeName);
            addVisibilityButton(panel, buttonDimensions, planeName);
            addTrackingButton(panel, buttonDimensions, planeName);
            addGOTOButton(panel, buttonDimensions, planeName);

            add(panel);
            refreshGui();
        }

        private void refreshGui() {
            this.revalidate();
            this.repaint();
        }


        public void addTransparencyButton(JPanel panel, int[] buttonDimensions,
                                                String planeName) {
            JButton button = new JButton("T");
            button.setPreferredSize(new Dimension(
                    buttonDimensions[0],
                    buttonDimensions[1]));

            button.addActionListener(e ->
            {
                JFrame frame = new JFrame("Transparency");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                float currentTransparency = 0.7f;
                if ( planeManager.checkNamedPlaneExists( planeName ) ) {
                    currentTransparency = planeManager.getTransparency( planeName );
                } else {
                    IJ.log(planeName + " plane not initialised");
                }

                // as here https://github.com/K-Meech/crosshair/blob/b7bdece786c1593969ec469916adf9737a7768bb/src/main/java/de/embl/cba/bdv/utils/BdvDialogs.java
                final BoundedValueDouble transparencyValue =
                        new BoundedValueDouble(
                                0,
                                1,
                                currentTransparency);

                double spinnerStepSize = 0.1;

                JPanel transparencyPanel = new JPanel();
                transparencyPanel.setLayout(new BoxLayout(transparencyPanel, BoxLayout.PAGE_AXIS));
                final SliderPanelDouble transparencySlider = new SliderPanelDouble("Transparency", transparencyValue, spinnerStepSize);
                transparencySlider.setNumColummns(7);
                transparencySlider.setDecimalFormat("####E0");
                transparencyValue.setUpdateListener(new TransparencyUpdateListener(transparencyValue,
                        transparencySlider, planeName, planeManager));

                transparencyPanel.add(transparencySlider);
                frame.setContentPane(transparencyPanel);

                //Display the window.
                frame.setBounds(MouseInfo.getPointerInfo().getLocation().x,
                        MouseInfo.getPointerInfo().getLocation().y,
                        120, 10);
                frame.setResizable(false);
                frame.pack();
                frame.setVisible(true);
            });

            if (planeName.equals("target")) {
                buttonsAffectedByTargetTracking.add(button);
            } else if (planeName.equals("block")) {
                buttonsAffectedByBlockTracking.add(button);
            }

            panel.add(button);
        }

        public void disableAllTracking () {
            for (String key : trackingButtons.keySet()) {
                trackingButtons.get(key).setEnabled(false);
            }
        }

        public void enableAllTracking () {
            for (String key : trackingButtons.keySet()) {
                trackingButtons.get(key).setEnabled(true);
            }
        }

        public class TransparencyUpdateListener implements BoundedValueDouble.UpdateListener {
            final private BoundedValueDouble transparencyValue;
            private final SliderPanelDouble transparencySlider;
            private final String planeName;
            PlaneManager planeManager;

            public TransparencyUpdateListener(BoundedValueDouble transparencyValue,
                                              SliderPanelDouble transparencySlider,
                                              String planeName,
                                              PlaneManager planeManager) {
                this.transparencyValue = transparencyValue;
                this.transparencySlider = transparencySlider;
                this.planeName = planeName;
                this.planeManager = planeManager;
            }

            @Override
            public void update() {
                transparencySlider.update();
                if ( planeManager.checkNamedPlaneExists( planeName ) ) {
                    planeManager.setPlaneTransparency( planeName, (float) transparencyValue.getCurrentValue() );
                } else {
                    IJ.log(planeName + " plane not initialised");
                }
            }
        }

    }
