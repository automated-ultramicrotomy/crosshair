package de.embl.schwab.crosshair.ui.swing;

import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.*;
import de.embl.schwab.crosshair.plane.BlockPlane;
import de.embl.schwab.crosshair.plane.Plane;
import de.embl.schwab.crosshair.plane.PlaneManager;
import ij.IJ;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.MouseInfo;
import java.util.HashMap;
import java.util.Map;

// similar to mobie source panel - https://github.com/mobie/mobie-viewer-fiji/blob/master/src/main/java/de/embl/cba/mobie/ui/viewer/SourcesPanel.java

    public class PlanePanel extends CrosshairPanel {

        private PlaneManager planeManager;
        private Map<String, JButton> trackingButtons;

        public PlanePanel() {}

        public void initialisePanel( PlaneManager planeManager ) {
            this.planeManager = planeManager;
            trackingButtons = new HashMap<>();

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Planes"),
                    BorderFactory.createEmptyBorder(5,5,5,5)));

            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            for ( String planeName: planeManager.getPlaneNames() ) {
                addPlaneToPanel( planeName );
            }
        }

        public void initialisePanel( CrosshairFrame crosshairFrame ) {
            initialisePanel( crosshairFrame.getPlaneManager() );
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
                    planeManager.getPlane( planeName ).setColor( colour );
                } else {
                    IJ.log( planeName + " plane not initialised" );
                }

            });

            planeManager.getPlane( planeName ).addButtonAffectedByTracking( colorButton );
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

            planeManager.getPlane( planeName ).addButtonAffectedByTracking( goToButton );

            panel.add(goToButton);
        }

        private void addTrackingButton(JPanel panel, int[] buttonDimensions, String planeName) {
            JButton trackButton;
            trackButton = new JButton("TRACK");

            trackButton.setPreferredSize(
                    new Dimension(2*buttonDimensions[0], buttonDimensions[1]));

            trackButton.addActionListener(e -> {
                toggleTracking( trackButton, planeName );
            });

            trackingButtons.put(planeName, trackButton);
            panel.add(trackButton);
        }

        private void disableButtonsAffectedByTracking( String planeName ) {
            for ( JButton button : planeManager.getPlane( planeName ).getButtonsAffectedByTracking() ) {
                button.setEnabled(false);
            }
        }

        private void enableButtonsAffectedByTracking( String planeName ) {
            for ( JButton button : planeManager.getPlane( planeName ).getButtonsAffectedByTracking() ) {
                button.setEnabled(true);
            }
        }

        private void toggleTracking( JButton trackButton, String planeName ) {
            if ( !planeManager.isTrackingPlane() ) {

                // check if there are already vertex points
                Plane plane = planeManager.getPlane( planeName );
                if ( plane instanceof BlockPlane && ((BlockPlane) plane).getVertices().size() > 0 ) {
                    int result = JOptionPane.showConfirmDialog(null, "If you track a block plane, you will lose all current vertex points. Continue?", "Are you sure?",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (result == JOptionPane.YES_OPTION) {
                        ((BlockPlane) plane).removeAllVertices();
                        enablePlaneTracking( trackButton, planeName );
                    }
                } else {
                    enablePlaneTracking( trackButton, planeName );
                }
            } else if ( planeManager.getTrackedPlaneName().equals( planeName ) ) {
                disablePlaneTracking( trackButton, planeName );
            }
        }

        private void enablePlaneTracking( JButton trackButton, String planeName ) {
            planeManager.setTrackingPlane( true );
            planeManager.setTrackedPlaneName( planeName );
            planeManager.getPlane( planeName ).setVisible( true );
            planeManager.updatePlaneCurrentView( planeName );
            trackButton.setBackground(new Color (255, 0,0));
            disableButtonsAffectedByTracking( planeName );
            disableAllTrackingButtonsExceptNamed( planeName );
        }

        private void disablePlaneTracking( JButton trackButton, String planeName ) {
            planeManager.setTrackingPlane( false );
            trackButton.setBackground(null);
            enableButtonsAffectedByTracking( planeName );
            enableAllTrackingButtons();
        }

        private void addVisibilityButton (JPanel panel, int[] buttonDimensions, String planeName) {
            JButton visbilityButton;
            visbilityButton = new JButton("V");

            visbilityButton.setPreferredSize(
                    new Dimension(buttonDimensions[0], buttonDimensions[1]));

            visbilityButton.addActionListener(e -> {
                if ( planeManager.checkNamedPlaneExists( planeName ) ) {
                    planeManager.getPlane( planeName ).toggleVisible();
                } else {
                    IJ.log(planeName + " plane not initialised" );
                }
            });

            planeManager.getPlane( planeName ).addButtonAffectedByTracking( visbilityButton );
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
                    currentTransparency = planeManager.getPlane( planeName ).getTransparency();
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

            planeManager.getPlane( planeName ).addButtonAffectedByTracking( button );
            panel.add(button);
        }

        public void disableAllTrackingButtons() {
            for (String key : trackingButtons.keySet()) {
                trackingButtons.get(key).setEnabled(false);
            }
        }

        public void disableAllTrackingButtonsExceptNamed( String planeName ) {
            for (String key : trackingButtons.keySet()) {
                if ( !key.equals(planeName) ) {
                    trackingButtons.get(key).setEnabled(false);
                }
            }
        }

        public void enableAllTrackingButtons() {
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
                    planeManager.getPlane( planeName ).setTransparency( (float) transparencyValue.getCurrentValue() );
                } else {
                    IJ.log(planeName + " plane not initialised");
                }
            }
        }

    }
