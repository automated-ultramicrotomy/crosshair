package de.embl.cba.crosshair.ui.swing;

import bdv.util.BdvHandle;
import de.embl.cba.crosshair.PlaneManager;
import de.embl.cba.crosshair.bdv.PointsOverlaySizeChange;
import de.embl.cba.crosshair.microtome.MicrotomeManager;
import ij3d.Content;
import ij3d.Image3DUniverse;

import javax.swing.*;
import java.util.ArrayList;

public class CrosshairFrame extends JFrame {

    private Image3DUniverse universe;
    private Content imageContent;
    private PlaneManager planeManager;
    private MicrotomeManager microtomeManager;
    private PointsOverlaySizeChange pointOverlay;
    private BdvHandle bdvHandle;

    private ImagesPanel imagesPanel;
    private PlanePanel planePanel;
    private PointsPanel pointsPanel;
    private VertexAssignmentPanel vertexAssignmentPanel;
    private MicrotomePanel microtomePanel;
    private SavePanel savePanel;
    private ArrayList<CrosshairPanel> allPanels;

    public CrosshairFrame(Image3DUniverse universe, Content imageContent, PlaneManager planeManager, MicrotomeManager microtomeManager,
                          PointsOverlaySizeChange pointOverlay, BdvHandle bdvHandle) {

        this.universe = universe;
        this.imageContent = imageContent;
        this.planeManager = planeManager;
        this.microtomeManager = microtomeManager;
        this.pointOverlay = pointOverlay;
        this.bdvHandle = bdvHandle;

        allPanels = new ArrayList<>();

        this.setTitle("Crosshair");
        this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

        // main panel
        JPanel mainPane = new JPanel();
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));
        mainPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        mainPane.setOpaque(true);
        this.setContentPane(mainPane);

        planePanel = new PlanePanel(this);
        allPanels.add(planePanel);
        pointsPanel = new PointsPanel(this);
        allPanels.add(pointsPanel);
        imagesPanel = new ImagesPanel( this);
        allPanels.add(imagesPanel);
        vertexAssignmentPanel = new VertexAssignmentPanel(this);
        allPanels.add(vertexAssignmentPanel);
        microtomePanel = new MicrotomePanel(this);
        allPanels.add(microtomePanel);
        savePanel = new SavePanel(this);
        allPanels.add(savePanel);

        // this happens separately as many panels depend on eachother, so they must all be created before initialising
        for (CrosshairPanel panel : allPanels) {
            panel.initialisePanel();
        }

        microtomeManager.setMicrotomePanel(microtomePanel);
        microtomeManager.setVertexAssignmentPanel(vertexAssignmentPanel);

        mainPane.add(imagesPanel);
        mainPane.add(planePanel);
        mainPane.add(pointsPanel);
        mainPane.add(vertexAssignmentPanel);
        mainPane.add(microtomePanel);
        mainPane.add(savePanel);

        this.pack();
        this.setVisible( true );

    }

    public ImagesPanel getImagesPanel() {
        return imagesPanel;
    }

    public MicrotomePanel getMicrotomePanel() {
        return microtomePanel;
    }

    public PlanePanel getPlanePanel() {
        return planePanel;
    }

    public PointsPanel getPointsPanel() {
        return pointsPanel;
    }

    public SavePanel getSavePanel() {
        return savePanel;
    }

    public VertexAssignmentPanel getVertexAssignmentPanel() {
        return vertexAssignmentPanel;
    }

    public Content getImageContent() {
        return imageContent;
    }

    public PlaneManager getPlaneManager() {
        return planeManager;
    }

    public Image3DUniverse getUniverse() {
        return universe;
    }

    public BdvHandle getBdvHandle() {
        return bdvHandle;
    }

    public MicrotomeManager getMicrotomeManager() {
        return microtomeManager;
    }

    public PointsOverlaySizeChange getPointOverlay() {
        return pointOverlay;
    }
}
