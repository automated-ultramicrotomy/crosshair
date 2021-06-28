package de.embl.schwab.crosshair.targetingaccuracy;

import bdv.util.BdvHandle;
import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.solution.Solution;
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
    private final Solution solution;

    private ArrayList<CrosshairPanel> allPanels;
    private PlanePanel planePanel;
    private OtherPanel otherPanel;
    private ImagesPanel imagesPanel;

    // primary image is the one fed to the planemanager to control extent of planes and points in the 3d view.
    // For accuracy measuring purposes, it should be the block before trimming
    public TargetingAccuracyFrame( Image3DUniverse universe, Map<String, Content> imageNametoContent,
                                  PlaneManager planeManager, BdvHandle bdvHandle, String unit, Solution solution  ) {

        this.universe = universe;
        this.planeManager = planeManager;
        this.bdvHandle = bdvHandle;
        this.unit = unit;
        this.solution = solution;

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
        AccuracySavePanel savePanel = new AccuracySavePanel();
        allPanels.add( savePanel );

        ArrayList<String> planeNames = new ArrayList<>();
        planeNames.add( TargetingAccuracy.beforeTarget );
        planeNames.add( TargetingAccuracy.beforeBlock );
        planeNames.add( TargetingAccuracy.afterBlock );
        planePanel.initialisePanel( planeManager, planeNames,  new ArrayList<>() );

        // disable and prevent enabling of before target plane tracking
        JButton targetTrackingButton = planePanel.getTrackingButtons().get( TargetingAccuracy.beforeTarget );
        targetTrackingButton.setEnabled( false );
        planePanel.getTrackingButtons().remove( TargetingAccuracy.beforeTarget );

        // disable and prevent enabling of before block plane trakcing
        JButton blockTrackingButton = planePanel.getTrackingButtons().get( TargetingAccuracy.beforeBlock );
        blockTrackingButton.setEnabled( false );
        planePanel.getTrackingButtons().remove( TargetingAccuracy.beforeBlock );

        otherPanel.initialisePanel( new ArrayList<>( imageNametoContent.values() ), universe, planeManager,
                bdvHandle, false );
        imagesPanel.initialisePanel( imageNametoContent, otherPanel, universe );
        savePanel.initialisePanel( this );

        mainPane.add(imagesPanel);
        mainPane.add(planePanel);
        mainPane.add(otherPanel);
        mainPane.add( savePanel );

        this.pack();
        this.setVisible( true );

    }

    public ImagesPanel getImagesPanel() {
        return imagesPanel;
    }

    public OtherPanel getOtherPanel() {
        return otherPanel;
    }

    public PlanePanel getPlanePanel() {
        return planePanel;
    }

    public PlaneManager getPlaneManager() {
        return planeManager;
    }

    public Solution getSolution() {
        return solution;
    }

    public String getUnit() {
        return unit;
    }
}
