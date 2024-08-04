package de.embl.schwab.crosshair.ui.swing;

import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;

import javax.swing.*;
import java.util.ArrayList;

/**
 * Class for main Crosshair UI, to hold all the various panels
 */
public class CrosshairFrame extends JFrame {

    private Crosshair crosshair;
    private ImagesPanel imagesPanel;
    private PlanePanel planePanel;
    private OtherPanel otherPanel;
    private VertexAssignmentPanel vertexAssignmentPanel;
    private MicrotomePanel microtomePanel;
    private SavePanel savePanel;
    private ArrayList<CrosshairPanel> allPanels;

    /**
     * Create the main Crosshair UI
     * @param crosshair crosshair
     */
    public CrosshairFrame(Crosshair crosshair) {

        this.crosshair = crosshair;
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

        MicrotomeManager microtomeManager = crosshair.getMicrotomeManager();
        microtomeManager.setMicrotomePanel(microtomePanel);

        mainPane.add(imagesPanel);
        mainPane.add(planePanel);
        mainPane.add(otherPanel);
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

    public OtherPanel getPointsPanel() {
        return otherPanel;
    }

    public SavePanel getSavePanel() {
        return savePanel;
    }

    public VertexAssignmentPanel getVertexAssignmentPanel() {
        return vertexAssignmentPanel;
    }

    public Crosshair getCrosshair() {
        return crosshair;
    }
}
