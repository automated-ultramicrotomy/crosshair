package de.embl.cba.crosshair.ui.swing;

import bdv.util.BdvHandle;
import de.embl.cba.crosshair.PlaneManager;
import de.embl.cba.crosshair.bdv.PointsOverlaySizeChange;
import de.embl.cba.crosshair.microtome.MicrotomeManager;
import ij3d.Content;
import ij3d.Image3DUniverse;

import javax.swing.*;

public class CrosshairFrame extends JFrame {

    public CrosshairFrame(Image3DUniverse universe, Content imageContent, PlaneManager planeManager, MicrotomeManager microtomeManager,
                          PointsOverlaySizeChange pointOverlay, BdvHandle bdvHandle) {

        this.setTitle("Crosshair");
        this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

        // main panel
        JPanel mainPane = new JPanel();
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));
        mainPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        mainPane.setOpaque(true);
        this.setContentPane(mainPane);

        PlanePanel planePanel = new PlanePanel(planeManager, microtomeManager);
        PointsPanel pointsPanel = new PointsPanel(universe, imageContent, pointOverlay, bdvHandle);
        ImagesPanel imagesPanel = new ImagesPanel(imageContent, pointsPanel);
        VertexAssignmentPanel vertexAssignmentPanel = new VertexAssignmentPanel(planeManager);
        MicrotomePanel microtomePanel = new MicrotomePanel(microtomeManager, planeManager, pointsPanel, vertexAssignmentPanel);
        microtomePanel.setParentFrame(this);
        microtomeManager.setMicrotomePanel(microtomePanel);
        microtomeManager.setVertexAssignmentPanel(vertexAssignmentPanel);
        SavePanel savePanel = new SavePanel(planeManager, microtomeManager, imageContent, microtomePanel, pointsPanel, pointOverlay);
        microtomePanel.setSavePanel(savePanel);

        mainPane.add(imagesPanel);
        mainPane.add(planePanel);
        mainPane.add(pointsPanel);
        mainPane.add(vertexAssignmentPanel);
        mainPane.add(microtomePanel);
        mainPane.add(savePanel);

        this.pack();
        this.setVisible( true );

    }
}
