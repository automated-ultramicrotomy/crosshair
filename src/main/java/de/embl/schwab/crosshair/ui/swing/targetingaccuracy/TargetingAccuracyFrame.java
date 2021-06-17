package de.embl.schwab.crosshair.ui.swing.targetingaccuracy;

import bdv.util.BdvHandle;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.bdv.PointsOverlaySizeChange;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.ui.swing.*;
import ij3d.Content;
import ij3d.Image3DUniverse;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Map;

public class TargetingAccuracyFrame extends JFrame {

    private Image3DUniverse universe;
    private Content imageContent;
    private PlaneManager planeManager;
    private PointsOverlaySizeChange pointOverlay;
    private BdvHandle bdvHandle;
    private String unit;

    private ArrayList<CrosshairPanel> allPanels;
    private PlanePanel planePanel;
    private OtherPanel otherPanel;
    private ImagesPanel imagesPanel;

    // primary image is the one fed to the planemanager to control extent of planes and points in the 3d view.
    // For accuracy measuring purposes, it should be the block before trimming
    public TargetingAccuracyFrame( Image3DUniverse universe, Map<String, Content> imageNametoContent,
                                   String primaryImageName, PlaneManager planeManager,
                                   MicrotomeManager microtomeManager, PointsOverlaySizeChange pointOverlay, BdvHandle bdvHandle, String unit  ) {

        this.universe = universe;
        this.imageContent = imageContent;
        this.planeManager = planeManager;
        this.pointOverlay = pointOverlay;
        this.bdvHandle = bdvHandle;
        this.unit = unit;

        allPanels = new ArrayList<>();

        this.setTitle("Crosshair targeting accuracy");
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

        planePanel.initialisePanel();
        otherPanel.initialisePanel((ArrayList<Content>) imageNametoContent.values(), universe, pointOverlay,
                bdvHandle, false );
        imagesPanel.initialisePanel( imageNametoContent, otherPanel, universe );

        // this happens separately as many panels depend on eachother, so they must all be created before initialising
        for (CrosshairPanel panel : allPanels) {
            panel.initialisePanel( this );
        }

        mainPane.add(imagesPanel);
        mainPane.add(planePanel);
        mainPane.add(otherPanel);

        this.pack();
        this.setVisible( true );



        this.planeManager = planeManager;

        this.setTitle("Crosshair targeting accuracy");
        this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

        // main panel
        JPanel mainPane = new JPanel();
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));
        mainPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        mainPane.setOpaque(true);
        this.setContentPane(mainPane);

        this.pack();
        this.setVisible( true );

    }
}
