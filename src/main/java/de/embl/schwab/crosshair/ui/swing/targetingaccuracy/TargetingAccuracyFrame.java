package de.embl.schwab.crosshair.ui.swing.targetingaccuracy;

import bdv.util.BdvHandle;
import de.embl.schwab.crosshair.PlaneManager;
import de.embl.schwab.crosshair.bdv.PointsOverlaySizeChange;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.ui.swing.*;
import ij3d.Content;
import ij3d.Image3DUniverse;

import javax.swing.*;
import java.util.ArrayList;

public class TargetingAccuracyFrame extends JFrame {

    private PlaneManager planeManager;

    public TargetingAccuracyFrame( PlaneManager planeManager  ) {

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
