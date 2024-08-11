package de.embl.schwab.crosshair.targetingaccuracy;

import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.Settings;
import de.embl.schwab.crosshair.settings.SettingsReader;
import de.embl.schwab.crosshair.settings.SettingsWriter;
import de.embl.schwab.crosshair.ui.swing.*;
import ij.IJ;
import net.imglib2.RealPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import static de.embl.schwab.crosshair.io.IoHelper.chooseOpenFilePath;
import static de.embl.schwab.crosshair.io.IoHelper.chooseSaveFilePath;

public class AccuracySavePanel extends CrosshairPanel {

    private static final Logger logger = LoggerFactory.getLogger(AccuracySavePanel.class);

    private PlaneManager planeManager;
    private OtherPanel otherPanel;
    private ImagesPanel imagesPanel;
    private JButton saveMeasures;
    private JButton saveSettings;
    private JButton loadSettings;
    private TargetingAccuracyFrame accuracyFrame;

    public AccuracySavePanel() {}

    public void initialisePanel( TargetingAccuracyFrame accuracyFrame ) {
        this.accuracyFrame = accuracyFrame;
        this.planeManager = accuracyFrame.getPlaneManager();
        this.otherPanel = accuracyFrame.getOtherPanel();
        this.imagesPanel = accuracyFrame.getImagesPanel();

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Save and Load"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        setLayout(new GridLayout(1, 3));

        ActionListener saveLoadListener = new SaveLoadListener();

        saveSettings = new JButton("Save Settings");
        saveSettings.setActionCommand("save_settings");
        saveSettings.addActionListener(saveLoadListener);
        add(saveSettings);
        PlanePanel planePanel = accuracyFrame.getPlanePanel();
        planePanel.addButtonAffectedByTracking( TargetingAccuracy.afterBlock, saveSettings );
        planePanel.addButtonAffectedByTracking( TargetingAccuracy.afterBlock, saveSettings );

        loadSettings = new JButton("Load Settings");
        loadSettings.setActionCommand("load_settings");
        loadSettings.addActionListener(saveLoadListener);
        add(loadSettings);
        planePanel.addButtonAffectedByTracking( TargetingAccuracy.afterBlock, loadSettings );
        planePanel.addButtonAffectedByTracking( TargetingAccuracy.afterBlock, loadSettings );

        saveMeasures = new JButton("Save Measures");
        saveMeasures.setActionCommand("save_measures");
        saveMeasures.addActionListener(saveLoadListener);
        saveMeasures.setEnabled(true);
        add(saveMeasures);
    }

    private void saveMeasures() {
        if ( planeManager.checkNamedPlaneExists( TargetingAccuracy.beforeTarget ) &&
                planeManager.checkNamedPlaneExists(TargetingAccuracy.beforeBlock) &&
                planeManager.checkNamedPlaneExists(TargetingAccuracy.afterBlock) ) {
            ArrayList<RealPoint> vertices = planeManager.getBlockPlane( TargetingAccuracy.beforeTarget ).
                    getVertexDisplay().getVertices();
            if ( vertices.size() < 1 ) {
                IJ.log("Before target vertex is not set!" );
            } else if ( vertices.size() > 1 ) {
                IJ.log( "There are too many before target vertices. There should only be one!");
            } else {
                String filePath = chooseSaveFilePath();
                if (filePath != null) {
                    AccuracyCalculator accuracyCalculator =
                            new AccuracyCalculator(planeManager.getBlockPlane(TargetingAccuracy.beforeTarget),
                                    planeManager.getBlockPlane(TargetingAccuracy.beforeBlock),
                                    planeManager.getPlane(TargetingAccuracy.afterBlock),
                                    accuracyFrame.getSolution());
                    accuracyCalculator.calculateAngleError();
                    accuracyCalculator.calculateSolutionDistanceError();
                    accuracyCalculator.calculateTargetPointToPlaneDistanceError();
                    accuracyCalculator.saveAccuracy(filePath);
                }
            }
        } else {
            IJ.log( "Not all planes are initialised!" );
        }
    }

    private void saveSettings() {
        String filePath = chooseSaveFilePath();
        if (filePath != null) {
            SettingsWriter writer = new SettingsWriter();
            Settings settings = writer.createSettings(planeManager, imagesPanel.getImageNameToContent());
            writer.writeSettings(settings, filePath);
        }
    }

    private void loadSettings() {
        String filePath = chooseOpenFilePath();
        if (filePath != null) {
            SettingsReader reader = new SettingsReader();
            Settings settings = reader.readSettings(filePath);

            // if before target isn't a block plane, make it one so that vertices can be added for the point
            // to plane distance measure.
            if (!(settings.planeNameToSettings.get( TargetingAccuracy.beforeTarget )
                    instanceof BlockPlaneSettings)) {
                settings.planeNameToSettings.put( TargetingAccuracy.beforeTarget,
                        new BlockPlaneSettings( settings.planeNameToSettings.get( TargetingAccuracy.beforeTarget )) );
            }

            try {
                reader.loadSettings( settings, planeManager, imagesPanel.getImageNameToContent() );
                if ( !otherPanel.check3DPointsVisible() ) {
                    otherPanel.toggleVisiblity3DPoints();
                }
            } catch (MicrotomeManager.IncorrectMicrotomeConfiguration e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    class SaveLoadListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("save_settings")) {
                saveSettings();
            } else if (e.getActionCommand().equals("load_settings")) {
                loadSettings();
            } else if (e.getActionCommand().equals("save_measures")) {
                saveMeasures();
            }
        }
    }
}
