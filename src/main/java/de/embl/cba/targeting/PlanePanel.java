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
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.vecmath.Color3f;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.*;
import java.util.List;

    public class PlanePanel extends JPanel {

        private final PlaneManager planeManager;

        public PlanePanel(PlaneManager planeManager) {
            this.planeManager = planeManager;
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

                if (planeName == "target") {
                    planeManager.setTargetPlaneColour(colour);
                } else if (planeName == "block") {
                    planeManager.setBlockPlaneColour(colour);
                }

            });

            panel.add(colorButton);
        }

        private void addGOTOButton(JPanel panel, int[] buttonDimensions, String planeName) {
            JButton goToButton;
            goToButton = new JButton("GO TO");

            goToButton.setPreferredSize(
                    new Dimension(2*buttonDimensions[0], buttonDimensions[1]));

            //TODO - check for target plane variable
            goToButton.addActionListener(e -> {
                planeManager.moveViewToNamedPlane(planeName);
            });

            panel.add(goToButton);
        }


        private void addPlaneToPanel(String planeName) {

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            panel.add(Box.createHorizontalGlue());

            JLabel sourceNameLabel = new JLabel(planeName);
            sourceNameLabel.setHorizontalAlignment(SwingUtilities.CENTER);

            int[] buttonDimensions = new int[]{50, 30};
            int[] viewSelectionDimensions = new int[]{50, 30};

            panel.add(sourceNameLabel);

            addColorButton(panel, buttonDimensions, planeName);
            addTransparencyButton(panel, buttonDimensions, planeName);
            addGOTOButton(panel, buttonDimensions, planeName);



//            final JButton removeButton =
//                    createRemoveButton( sam, buttonDimensions );
//
//            final JCheckBox volumeVisibilityCheckbox =
//                    SourcesDisplayUI.createVolumeViewVisibilityCheckbox(
//                            this,
//                            viewSelectionDimensions,
//                            sam,
//                            sam.metadata().showImageIn3d || sam.metadata().showSelectedSegmentsIn3d );
//
//            panel.add( brightnessButton );
//            panel.add( removeButton );
//            panel.add( volumeVisibilityCheckbox );

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
                if (planeName.equals("target")) {
                    currentTransparency = planeManager.getTargetTransparency();
                } else if (planeName.equals("block")) {
                    currentTransparency = planeManager.getBlockTransparency();
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