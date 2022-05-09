package de.embl.schwab.crosshair.ui.swing;

import bdv.util.BdvHandle;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import ij3d.Content;
import ij3d.Image3DUniverse;

import javax.swing.*;
import java.util.ArrayList;

public class CrosshairFrame extends JFrame {

    private Image3DUniverse universe;
    private Content imageContent;
    private PlaneManager planeManager;
    private MicrotomeManager microtomeManager;
    private BdvHandle bdvHandle;

    private ImagesPanel imagesPanel;
    private PlanePanel planePanel;
    private OtherPanel otherPanel;
    private VertexAssignmentPanel vertexAssignmentPanel;
    private MicrotomePanel microtomePanel;
    private SavePanel savePanel;
    private ArrayList<CrosshairPanel> allPanels;

    private String unit;

    public CrosshairFrame(Image3DUniverse universe, Content imageContent, PlaneManager planeManager, MicrotomeManager microtomeManager,
                          BdvHandle bdvHandle, String unit) {

        this.universe = universe;
        this.imageContent = imageContent;
        this.planeManager = planeManager;
        this.microtomeManager = microtomeManager;
        this.bdvHandle = bdvHandle;
        this.unit = unit;

        allPanels = new ArrayList<>();

        this.setTitle("Crosshair");
        this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

        // main panel
        JPanel mainPane = new JPanel();
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));
        mainPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        mainPane.setOpaque(true);
        this.setContentPane(mainPane);

        planePanel = new PlanePanel();
        allPanels.add(planePanel);
        otherPanel = new OtherPanel();
        allPanels.add(otherPanel);
        imagesPanel = new ImagesPanel();
        allPanels.add(imagesPanel);
        vertexAssignmentPanel = new VertexAssignmentPanel();
        allPanels.add(vertexAssignmentPanel);
        microtomePanel = new MicrotomePanel();
        allPanels.add(microtomePanel);
        savePanel = new SavePanel();
        allPanels.add(savePanel);

        // this happens separately as many panels depend on eachother, so they must all be created before initialising
        for (CrosshairPanel panel : allPanels) {
            panel.initialisePanel( this );
        }

        microtomeManager.setMicrotomePanel(microtomePanel);
        microtomeManager.setVertexAssignmentPanel(vertexAssignmentPanel);

        mainPane.add(imagesPanel);
        mainPane.add(planePanel);
        mainPane.add(otherPanel);
        mainPane.add(vertexAssignmentPanel);
        mainPane.add(microtomePanel);
        mainPane.add(savePanel);

        refreshGui();

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

    public OtherPanel getPointsPanel() {
        return otherPanel;
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

    public String getUnit() { return unit; }

    public void refreshGui() {
        this.pack();
        this.setVisible( true );
    }
}
