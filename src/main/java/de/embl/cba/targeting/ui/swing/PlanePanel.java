package de.embl.cba.targeting.ui.swing;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SliderPanel;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.*;
import de.embl.cba.targeting.PlaneManager;

import javax.swing.*;
import java.awt.*;

// similar to mobie source panel - https://github.com/mobie/mobie-viewer-fiji/blob/master/src/main/java/de/embl/cba/mobie/ui/viewer/SourcesPanel.java

    public class PlanePanel extends JPanel {

        private final PlaneManager planeManager;

        public PlanePanel(PlaneManager planeManager) {
            this.planeManager = planeManager;

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
                if (planeManager.getTrackPlane() == 0) {
                    Color colour = JColorChooser.showDialog(null, "", null);

                    if (colour == null) return;

                    if (planeName == "target") {
                        if (planeManager.checkNamedPlaneExists("target")) {
                            planeManager.setTargetPlaneColour(colour);
                        } else {
                            System.out.println("Target plane not initialised");
                        }
                    } else if (planeName == "block") {
                        if (planeManager.checkNamedPlaneExists("block")) {
                            planeManager.setBlockPlaneColour(colour);
                        } else {
                            System.out.println("Block plane not initialised");
                        }
                    }
                } else {
                    System.out.println("Can only change colour, when not tracking a plane");
                }

            });

            panel.add(colorButton);
        }

        private void addGOTOButton(JPanel panel, int[] buttonDimensions, String planeName) {
            JButton goToButton;
            goToButton = new JButton("GO TO");

            goToButton.setPreferredSize(
                    new Dimension(2*buttonDimensions[0], buttonDimensions[1]));

            goToButton.addActionListener(e -> {
                if (planeManager.getTrackPlane() == 0) {
                    if (planeManager.checkNamedPlaneExists(planeName)) {
                        planeManager.moveViewToNamedPlane(planeName);
                    } else {
                        System.out.println("Plane not initialised");
                    }
                } else {
                    System.out.println("Can only go to plane, when not tracking a plane");
                }
            });

            panel.add(goToButton);
        }

        private void addVisibilityButton (JPanel panel, int[] buttonDimensions, String planeName) {
            JButton visbilityButton;
            visbilityButton = new JButton("V");

            visbilityButton.setPreferredSize(
                    new Dimension(buttonDimensions[0], buttonDimensions[1]));

            visbilityButton.addActionListener(e -> {
                if (planeManager.getTrackPlane() == 0) {
                    if (planeName == "target") {
                        if (planeManager.checkNamedPlaneExists("target")) {
                            planeManager.toggleTargetVisbility();
                        } else {
                            System.out.println("Target plane not initialised");
                        }
                    } else if (planeName == "block") {
                        if (planeManager.checkNamedPlaneExists("block")) {
                            planeManager.toggleBlockVisbility();
                        } else {
                            System.out.println("Block plane not initialised");
                        }
                    }
                } else {
                    System.out.println("Can only toggle visiblity, when not tracking a plane");
                }
            });

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
                if (planeManager.getTrackPlane() == 0) {
                    JFrame frame = new JFrame("Transparency");
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                    float currentTransparency = 0.7f;
                    if (planeName.equals("target")) {
                        if (planeManager.checkNamedPlaneExists("target")) {
                            currentTransparency = planeManager.getTargetTransparency();
                        } else {
                            System.out.println("Target plane not initialised");
                        }
                    } else if (planeName.equals("block")) {
                        if (planeManager.checkNamedPlaneExists("block")) {
                            currentTransparency = planeManager.getBlockTransparency();
                        } else {
                            System.out.println("Block plane not initialised");
                        }
                    }

//                as here https://github.com/tischi/imagej-utils/blob/b7bdece786c1593969ec469916adf9737a7768bb/src/main/java/de/embl/cba/bdv/utils/BdvDialogs.java
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
                } else {
                    System.out.println("Can only change transparency of plane, when not tracking a plane");
                }
            });

            panel.add(button);
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
                if (planeName.equals("target")) {
                    planeManager.setTargetTransparency((float) transparencyValue.getCurrentValue());
                } else if (planeName.equals("block")) {
                    planeManager.setBlockTransparency((float) transparencyValue.getCurrentValue());
                }
            }
        }

    }
