package de.embl.schwab.crosshair.ui.swing;

import bdv.util.BdvHandle;
import de.embl.schwab.crosshair.bdv.PointsOverlaySizeChange;
import ij3d.Content;
import ij3d.Image3DUniverse;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

// similar to mobie source panel - https://github.com/mobie/mobie-viewer-fiji/blob/master/src/main/java/de/embl/cba/mobie/ui/viewer/SourcesPanel.java

public class PointsPanel extends CrosshairPanel {

    private Content imageContent;
    private Image3DUniverse universe;
    private boolean threeDPointsVisible;
    private PointsOverlaySizeChange pointOverlay;
    private BdvHandle bdvHandle;
    private ArrayList<JButton> microtomeVisibilityButtons;
    private CrosshairFrame crosshairFrame;

    public PointsPanel(CrosshairFrame crosshairFrame) {
        this.crosshairFrame = crosshairFrame;
    }

    public void initialisePanel () {
        imageContent = crosshairFrame.getImageContent();
        universe = crosshairFrame.getUniverse();
        pointOverlay = crosshairFrame.getPointOverlay();
        bdvHandle = crosshairFrame.getBdvHandle();
        threeDPointsVisible = true;
        microtomeVisibilityButtons = new ArrayList<>();

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Other"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        setLayout(new GridLayout(2, 3));
        addPointToPanel("3d points ", "3D");
        addPointToPanel("Knife", "/knife.stl");
        addPointToPanel("Rotation Axis", "rotationAxis");
        addPointToPanel("2d points ", "2D");
        addPointToPanel("Holder", "holder");
        deactivateMicrotomeButtons();
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
            imageContent.showPointList(false);
            threeDPointsVisible = false;
        } else {
            imageContent.showPointList(true);
            threeDPointsVisible = true;
        }
    }

    private void add2DVisibilityButton(JPanel panel, int[] buttonDimensions) {
        JButton visbilityButton;
        visbilityButton = new JButton("V");

        visbilityButton.setPreferredSize(
                new Dimension(buttonDimensions[0], buttonDimensions[1]));

        visbilityButton.addActionListener(e -> {
            pointOverlay.toggleShowPoints();
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