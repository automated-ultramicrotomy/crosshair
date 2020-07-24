package de.embl.cba.targeting;

import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BdvHandle;
import bdv.util.BoundedValueDouble;
import ij3d.Content;
import org.scijava.vecmath.Color3f;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

// similar to mobie source panel - https://github.com/mobie/mobie-viewer-fiji/blob/master/src/main/java/de/embl/cba/mobie/ui/viewer/SourcesPanel.java

public class PointsPanel extends JPanel {

    private Content imageContent;
    private boolean threeDPointsVisible;
    private PointsOverlaySizeChange pointOverlay;
    private BdvHandle bdvHandle;

    public PointsPanel(Content imageContent, PointsOverlaySizeChange pointOverlay, BdvHandle bdvHandle) {

        this.imageContent = imageContent;
        this.pointOverlay = pointOverlay;
        this.bdvHandle = bdvHandle;
        threeDPointsVisible = true;

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Points"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        setLayout(new GridLayout(1, 2));
        addPlaneToPanel("3d view ", "3D");
        addPlaneToPanel("2d view ", "2D");
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

    private void addPlaneToPanel(String pointName, String pointType) {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        panel.add(Box.createHorizontalGlue());

        JLabel sourceNameLabel = new JLabel(pointName);
        sourceNameLabel.setHorizontalAlignment(SwingUtilities.CENTER);

        int[] buttonDimensions = new int[]{50, 30};

        panel.add(sourceNameLabel);
        if (pointType == "3D") {
            add3DVisibilityButton(panel, buttonDimensions);
        } else if (pointType == "2D") {
            add2DVisibilityButton(panel, buttonDimensions);
        }

        add(panel);
        refreshGui();
    }

    private void refreshGui() {
        this.revalidate();
        this.repaint();
    }
}