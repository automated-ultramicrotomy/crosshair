package de.embl.schwab.crosshair.ui.swing;

import bdv.util.BdvHandle;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.points.overlays.PointOverlay2d;
import ij3d.Content;
import ij3d.Image3DUniverse;

import javax.swing.*;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.util.ArrayList;

/**
 * Class for UI Panel containing controls for visibility of various items
 */
public class OtherPanel extends CrosshairPanel {

    private ArrayList<Content> imageContents;
    private Image3DUniverse universe;
    private PlaneManager planeManager;
    private boolean threeDPointsVisible;
    private BdvHandle bdvHandle;
    private ArrayList<JButton> microtomeVisibilityButtons;

    public OtherPanel() {}

    /**
     * Initialise panel
     * @param imageContents image contents displayed in 3D viewer
     * @param universe universe of the 3D viewer
     * @param planeManager plane manager
     * @param bdvHandle bdvHandle of the BigDataViewer window
     * @param includeMicrotomeButtons whether to include buttons to control visibility of microtome pieces in
     *                                the 3D viewer
     */
    public void initialisePanel( ArrayList<Content> imageContents, Image3DUniverse universe,
                                PlaneManager planeManager,  BdvHandle bdvHandle,
                                boolean includeMicrotomeButtons ) {
        this.imageContents = imageContents;
        this.universe = universe;
        this.bdvHandle = bdvHandle;
        this.planeManager = planeManager;
        threeDPointsVisible = true;
        microtomeVisibilityButtons = new ArrayList<>();

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Other"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        setLayout(new GridLayout(2, 3));
        addPointToPanel("3d points ", "3D");
        if ( includeMicrotomeButtons ) {
            addPointToPanel("Knife", "/knife.stl");
            addPointToPanel("Rotation Axis", "rotationAxis");
        }
        addPointToPanel("2d points ", "2D");
        if ( includeMicrotomeButtons ) {
            addPointToPanel("Holder", "holder");
            deactivateMicrotomeButtons();
        }
    }

    /**
     * Initialise panel from settings in main Crosshair UI
     * @param crosshairFrame main crosshair UI
     */
    public void initialisePanel( CrosshairFrame crosshairFrame ) {
        ArrayList<Content> imageContents = new ArrayList<>();
        imageContents.add( crosshairFrame.getImageContent() );

        initialisePanel( imageContents, crosshairFrame.getUniverse(),
                crosshairFrame.getPlaneManager(),
                crosshairFrame.getBdvHandle(), true );
    }

    public boolean check3DPointsVisible() {
        return threeDPointsVisible;
    }

    private void add3DVisibilityButton(JPanel panel, int[] buttonDimensions) {
        JButton visbilityButton;
        visbilityButton = new JButton("V");

        visbilityButton.setPreferredSize(
                new Dimension(buttonDimensions[0], buttonDimensions[1]));

        visbilityButton.addActionListener(e -> {
            toggleVisiblity3DPoints();
        });

        panel.add(visbilityButton);
    }

    public void toggleVisiblity3DPoints () {
        if (threeDPointsVisible) {
            for ( Content imageContent: imageContents ) {
                imageContent.showPointList(false);
            }
            threeDPointsVisible = false;
        } else {
            for ( Content imageContent: imageContents ) {
                imageContent.showPointList(true);
            }
            universe.getPointListDialog().setVisible(false);
            threeDPointsVisible = true;
        }
    }

    private void add2DVisibilityButton(JPanel panel, int[] buttonDimensions) {
        JButton visbilityButton;
        visbilityButton = new JButton("V");

        visbilityButton.setPreferredSize(
                new Dimension(buttonDimensions[0], buttonDimensions[1]));

        visbilityButton.addActionListener(e -> {
            for ( PointOverlay2d pointOverlay: planeManager.getAll2dPointOverlays() ) {
                pointOverlay.toggleShowPoints();
            }
            bdvHandle.getViewerPanel().requestRepaint();
        });

        panel.add(visbilityButton);
    }

    private void addMicrotomeVisiblityButton(JPanel panel, int[] buttonDimensions, String microtomePart) {
        JButton visbilityButton;
        visbilityButton = new JButton("V");

        visbilityButton.setPreferredSize(
                new Dimension(buttonDimensions[0], buttonDimensions[1]));

        microtomeVisibilityButtons.add(visbilityButton);

        visbilityButton.addActionListener(e -> {
            if (!microtomePart.equals("holder")) {
                Content microtomeObject = universe.getContent(microtomePart);
                if (microtomeObject.isVisible()) {
                    microtomeObject.setVisible(false);
                } else {
                    microtomeObject.setVisible(true);
                }
            } else {
                ArrayList<String> holderParts = new ArrayList<>();
                holderParts.add("/arc.stl");
                holderParts.add( "/holder_back.stl");
                holderParts.add("/holder_front.stl");
                for (String part : holderParts) {
                    Content microtomeObject = universe.getContent(part);
                    if (microtomeObject.isVisible()) {
                        microtomeObject.setVisible(false);
                    } else {
                        microtomeObject.setVisible(true);
                    }
                }
            }
        });

        panel.add(visbilityButton);
    }

    private void addPointToPanel(String pointName, String pointType) {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        panel.add(Box.createHorizontalGlue());

        JLabel sourceNameLabel = new JLabel(pointName);
        sourceNameLabel.setHorizontalAlignment(SwingUtilities.CENTER);

        int[] buttonDimensions = new int[]{50, 30};

        panel.add(sourceNameLabel);
        if (pointType.equals("3D")) {
            add3DVisibilityButton(panel, buttonDimensions);
        } else if (pointType.equals("2D")) {
            add2DVisibilityButton(panel, buttonDimensions);
        } else {
            addMicrotomeVisiblityButton(panel, buttonDimensions, pointType);
        }

        add(panel);
        refreshGui();
    }

    public void deactivateMicrotomeButtons () {
        for (JButton button : microtomeVisibilityButtons) {
            button.setEnabled(false);
        }
    }

    public void activateMicrotomeButtons () {
        for (JButton button : microtomeVisibilityButtons) {
            button.setEnabled(true);
        }
    }

    private void refreshGui() {
        this.revalidate();
        this.repaint();
    }
}