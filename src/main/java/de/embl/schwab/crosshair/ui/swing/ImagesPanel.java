package de.embl.schwab.crosshair.ui.swing;

import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BoundedValueDouble;
import de.embl.schwab.crosshair.Crosshair;
import ij3d.Content;
import ij3d.Image3DUniverse;
import org.scijava.vecmath.Color3f;

import javax.swing.*;
import java.awt.MouseInfo;
import java.awt.Dimension;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for UI Panel containing controls for 3D image display
 */
public class ImagesPanel extends CrosshairPanel {

    private Map<String, Content> imageNameToContent;
    private Image3DUniverse universe;
    private OtherPanel otherPanel;

    public ImagesPanel() {}

    /**
     * Initialise panel
     * @param imageNameToContent Map of image names to their image content displayed in 3D viewer
     * @param otherPanel UI panel controlling visibility of various items in the viewers
     * @param universe universe of the 3D viewer
     */
    public void initialisePanel( Map<String, Content> imageNameToContent, OtherPanel otherPanel, Image3DUniverse universe ) {
        this.imageNameToContent = imageNameToContent;
        this.otherPanel = otherPanel;
        this.universe = universe;

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Images"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        for (String imageName: imageNameToContent.keySet() ) {
            addImageToPanel( imageName );
        }
    }

    /**
     * Initialise panel from settings in main Crosshair UI
     * @param crosshairFrame main crosshair UI
     */
    public void initialisePanel ( CrosshairFrame crosshairFrame ) {
        Map<String, Content> imageNameToContent = new HashMap<>();
        imageNameToContent.put( Crosshair.image, crosshairFrame.getImageContent() );

        initialisePanel( imageNameToContent, crosshairFrame.getPointsPanel(), crosshairFrame.getUniverse() );
    }

    public Map<String, Content> getImageNameToContent() {
        return imageNameToContent;
    }

    private void addColorButton(JPanel panel, int[] buttonDimensions, String imageName ) {
        JButton colorButton;
        colorButton = new JButton("C");

        colorButton.setPreferredSize(
                new Dimension(buttonDimensions[0], buttonDimensions[1]));

        colorButton.addActionListener(e -> {
            Color colour = JColorChooser.showDialog(null, "", null);

            if (colour == null) return;
            Content imageContent = imageNameToContent.get( imageName );
            imageContent.setColor( new Color3f(colour) );
        });

        panel.add(colorButton);
    }

    private void addVisibilityButton ( JPanel panel, int[] buttonDimensions, String imageName ) {
        JButton visbilityButton;
        visbilityButton = new JButton("V");

        visbilityButton.setPreferredSize(
                new Dimension(buttonDimensions[0], buttonDimensions[1]));

        visbilityButton.addActionListener(e -> {
            Content imageContent = imageNameToContent.get( imageName );
            if ( imageContent.isVisible() ) {
               imageContent.setVisible(false);
                // Making image content invisible, also makes 3d points invisible > reverse this
                if ( otherPanel.check3DPointsVisible() ) {
                    imageContent.showPointList(true);
                    universe.getPointListDialog().setVisible(false);
                }
            } else {
                imageContent.setVisible(true);
            }
        });

        panel.add(visbilityButton);
    }

    private void addImageToPanel(String imageName) {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        panel.add(Box.createHorizontalGlue());

        JLabel sourceNameLabel = new JLabel(imageName);
        sourceNameLabel.setHorizontalAlignment(SwingUtilities.CENTER);

        int[] buttonDimensions = new int[]{50, 30};

        panel.add(sourceNameLabel);

        addColorButton(panel, buttonDimensions, imageName);
        addTransparencyButton(panel, buttonDimensions, imageName);
        addVisibilityButton(panel, buttonDimensions, imageName);

        add(panel);
        refreshGui();
    }

    private void refreshGui() {
        this.revalidate();
        this.repaint();
    }

    private void addTransparencyButton(JPanel panel, int[] buttonDimensions,
                                      String imageName ) {
        JButton button = new JButton("T");
        button.setPreferredSize(new Dimension(
                buttonDimensions[0],
                buttonDimensions[1]));

        button.addActionListener(e ->
        {

            JFrame frame = new JFrame("Transparency");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            Content imageContent = imageNameToContent.get( imageName );

            float currentTransparency = imageContent.getTransparency();

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
            transparencyValue.setUpdateListener(
                    new TransparencyUpdateListener(transparencyValue, transparencySlider, imageName));

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

    private class TransparencyUpdateListener implements BoundedValueDouble.UpdateListener {
        final private BoundedValueDouble transparencyValue;
        private final SliderPanelDouble transparencySlider;
        private final String imageName;

        public TransparencyUpdateListener( BoundedValueDouble transparencyValue,
                                          SliderPanelDouble transparencySlider,
                                           String imageName ) {
            this.transparencyValue = transparencyValue;
            this.transparencySlider = transparencySlider;
            this.imageName = imageName;
        }

        @Override
        public void update() {
            transparencySlider.update();
            Content imageContent = imageNameToContent.get( imageName );
            imageContent.setTransparency((float) transparencyValue.getCurrentValue());
        }
    }

}

