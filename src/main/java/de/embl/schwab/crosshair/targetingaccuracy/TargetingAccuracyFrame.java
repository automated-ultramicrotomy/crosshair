package de.embl.schwab.crosshair.targetingaccuracy;

import bdv.util.BdvHandle;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.points.PointOverlay2d;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.ui.swing.*;
import ij3d.Content;
import ij3d.Image3DUniverse;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Map;

public class TargetingAccuracyFrame extends JFrame {

    private final Image3DUniverse universe;
    private final PlaneManager planeManager;
    private final BdvHandle bdvHandle;
    private final String unit;

    private ArrayList<CrosshairPanel> allPanels;
    private PlanePanel planePanel;
    private OtherPanel otherPanel;
    private ImagesPanel imagesPanel;

    // primary image is the one fed to the planemanager to control extent of planes and points in the 3d view.
    // For accuracy measuring purposes, it should be the block before trimming
    public TargetingAccuracyFrame( Image3DUniverse universe, Map<String, Content> imageNametoContent,
                                  PlaneManager planeManager, BdvHandle bdvHandle, String unit  ) {

        this.universe = universe;
        this.planeManager = planeManager;
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

        ArrayList<String> planeNames = new ArrayList<>();
        planeNames.add( TargetingAccuracy.beforeBlock );
        planeNames.add( TargetingAccuracy.beforeTarget );
        planeNames.add( TargetingAccuracy.afterBlock );
        planePanel.initialisePanel( planeManager, planeNames,  new ArrayList<>() );
        otherPanel.initialisePanel( new ArrayList<>( imageNametoContent.values() ), universe, new ArrayList<>(),
                bdvHandle, false );
        imagesPanel.initialisePanel( imageNametoContent, otherPanel, universe );

        mainPane.add(imagesPanel);
        mainPane.add(planePanel);
        mainPane.add(otherPanel);

        this.pack();
        this.setVisible( true );

    }
}
