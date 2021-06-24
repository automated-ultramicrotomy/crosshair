package de.embl.schwab.crosshair.ui.swing;

import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.*;
import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.plane.BlockPlane;
import de.embl.schwab.crosshair.plane.Plane;
import de.embl.schwab.crosshair.plane.PlaneManager;
import ij.IJ;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.MouseInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// similar to mobie source panel - https://github.com/mobie/mobie-viewer-fiji/blob/master/src/main/java/de/embl/cba/mobie/ui/viewer/SourcesPanel.java

    public class PlanePanel extends CrosshairPanel {

        private PlaneManager planeManager;
        private Map<String, JButton> trackingButtons;
        private Map<String, JButton> goToButtons;
        private Map<String, ArrayList<JButton>> planeNameToButtonsAffectedByTracking;
        private List<String> planeNames;
        private List<String> blockPlaneNames;

        public PlanePanel() {}

        public void initialisePanel( PlaneManager planeManager, List<String> planeNames,  List<String> blockPlaneNames ) {
            this.planeManager = planeManager;
            trackingButtons = new HashMap<>();
            goToButtons = new HashMap<>();
            planeNameToButtonsAffectedByTracking = new HashMap<>();
            this.planeNames = planeNames;
            this.blockPlaneNames = blockPlaneNames;

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Planes"),
                    BorderFactory.createEmptyBorder(5,5,5,5)));

            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            for ( String planeName: planeNames ) {
                addPlaneToPanel( planeName );
            }
            for ( String planeName: blockPlaneNames ) {
                addPlaneToPanel( planeName );
            }
        }

        public void initialisePanel( CrosshairFrame crosshairFrame ) {
            ArrayList<String> planeNames = new ArrayList<>();
            planeNames.add( Crosshair.target );
            ArrayList<String> blockPlaneNames = new ArrayList<>();
            blockPlaneNames.add( Crosshair.block );
            initialisePanel( crosshairFrame.getPlaneManager(), planeNames, blockPlaneNames );
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

            addButtonAffectedByTracking( planeName, colorButton );
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

            goToButtons.put( planeName, goToButton );
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
            for ( JButton button : planeNameToButtonsAffectedByTracking.get( planeName ) ) {
                button.setEnabled(false);
            }
        }

        private void enableButtonsAffectedByTracking( String planeName ) {
            for ( JButton button : planeNameToButtonsAffectedByTracking.get( planeName ) ) {
                button.setEnabled(true);
            }
        }

        private void toggleTracking( JButton trackButton, String planeName ) {
            if ( !planeManager.isTrackingPlane() ) {
                if ( planeManager.checkNamedPlaneExists( planeName ) ) {

                    // check if there are already vertex points
                    Plane plane = planeManager.getPlane(planeName);
                    if (plane instanceof BlockPlane && ((BlockPlane) plane).getVertexDisplay().getVertices().size() > 0) {
                        int result = JOptionPane.showConfirmDialog(null, "If you track a block plane, you will lose all current vertex points. Continue?", "Are you sure?",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                        if (result == JOptionPane.YES_OPTION) {
                            ((BlockPlane) plane).getVertexDisplay().removeAllVertices();
                            enablePlaneTracking(trackButton, planeName);
                        }
                    } else {
                        enablePlaneTracking(trackButton, planeName);
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

            if ( planeManager.checkNamedPlaneExists( planeName ) ) {
                planeManager.updatePlaneCurrentView(planeName);
            } else {
                if ( blockPlaneNames.contains( planeName ) ) {
                    planeManager.addBlockPlaneAtCurrentView( planeName );
                } else {
                    planeManager.addPlaneAtCurrentView( planeName );
                }
            }
            planeManager.getPlane(planeName).setVisible(true);
            trackButton.setBackground(new Color (255, 0,0));
            disableButtonsAffectedByTracking( planeName );
            disableAllTrackingButtonsExceptNamed( planeName );
            disableAllGoToButtons();
        }

        private void disablePlaneTracking( JButton trackButton, String planeName ) {
            planeManager.setTrackingPlane( false );
            trackButton.setBackground(null);
            enableButtonsAffectedByTracking( planeName );
            enableAllTrackingButtons();
            enableAllGoToButtons();
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

            addButtonAffectedByTracking( planeName, visbilityButton );
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

            addButtonAffectedByTracking( planeName, button );
            panel.add(button);
        }

        public void addButtonAffectedByTracking( String planeName, JButton button ) {
            if ( !planeNameToButtonsAffectedByTracking.containsKey( planeName ) ) {
                ArrayList<JButton> buttons = new ArrayList<>();
                buttons.add(button);
                planeNameToButtonsAffectedByTracking.put( planeName, buttons );
            } else {
                planeNameToButtonsAffectedByTracking.get( planeName ).add( button );
            }
        }

        public void disableAllTrackingButtons() {
            for ( JButton button: trackingButtons.values() ) {
                button.setEnabled( false );
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
            for ( JButton button: trackingButtons.values() ) {
                button.setEnabled( true );
            }
        }

        public void enableAllGoToButtons() {
            for ( JButton button: goToButtons.values() ) {
                button.setEnabled( true );
            }
        }

        public void disableAllGoToButtons() {
            for ( JButton button: goToButtons.values() ) {
                button.setEnabled( false );
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
