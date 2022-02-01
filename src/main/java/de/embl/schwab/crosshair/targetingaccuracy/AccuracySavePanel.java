package de.embl.schwab.crosshair.targetingaccuracy;

import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.Settings;
import de.embl.schwab.crosshair.settings.SettingsReader;
import de.embl.schwab.crosshair.settings.SettingsWriter;
import de.embl.schwab.crosshair.ui.swing.*;
import ij.IJ;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static de.embl.schwab.crosshair.io.IoHelper.chooseOpenFilePath;
import static de.embl.schwab.crosshair.io.IoHelper.chooseSaveFilePath;

public class AccuracySavePanel extends CrosshairPanel {

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

    class SaveLoadListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("save_settings")) {

                String filePath = chooseSaveFilePath();
                if (filePath != null) {
                    SettingsWriter writer = new SettingsWriter();
                    Settings settings = writer.createSettings(planeManager, imagesPanel.getImageNameToContent());
                    writer.writeSettings(settings, filePath);
                }

            } else if (e.getActionCommand().equals("load_settings")) {

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
                    reader.loadSettings( settings, planeManager, imagesPanel.getImageNameToContent(), otherPanel );
                }

            } else if (e.getActionCommand().equals("save_measures")) {
                if ( planeManager.checkNamedPlaneExists( TargetingAccuracy.beforeTarget ) &&
                planeManager.checkNamedPlaneExists(TargetingAccuracy.beforeBlock) &&
                planeManager.checkNamedPlaneExists(TargetingAccuracy.afterBlock) ) {
                    String filePath = chooseSaveFilePath();
                    if (filePath != null) {
                        AccuracyCalculator accuracyCalculator =
                                new AccuracyCalculator(planeManager.getPlane(TargetingAccuracy.beforeTarget),
                                        planeManager.getBlockPlane(TargetingAccuracy.beforeBlock),
                                        planeManager.getPlane(TargetingAccuracy.afterBlock),
                                        accuracyFrame.getSolution());
                        accuracyCalculator.calculateAngleError();
                        accuracyCalculator.calculateDistanceError();
                        accuracyCalculator.saveAccuracy(filePath);
                    }
                } else {
                    IJ.log( "Not all planes are initialised!" );
                }
            }
        }
    }
}
