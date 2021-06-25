package de.embl.schwab.crosshair.targetingaccuracy;

import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.io.*;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.ui.swing.*;
import ij3d.Content;

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
        saveMeasures.setEnabled(false);
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
                    reader.loadSettings( settings, planeManager, imagesPanel.getImageNameToContent(), otherPanel );
                }

            } else if (e.getActionCommand().equals("save_measures")) {
                // pass
            }
        }
    }
}
