package de.embl.schwab.crosshair.ui.swing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.io.ImageContentSettings;
import de.embl.schwab.crosshair.io.PlaneSettingsListDeserializer;
import de.embl.schwab.crosshair.io.Settings;
import de.embl.schwab.crosshair.io.Solution;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.plane.BlockPlaneSettings;
import de.embl.schwab.crosshair.plane.Plane;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.plane.PlaneSettings;
import ij.IJ;
import ij3d.Content;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavePanel extends CrosshairPanel {
    private PlaneManager planeManager;
    private MicrotomeManager microtomeManager;
    private Content imageContent;
    private MicrotomePanel microtomePanel;
    private OtherPanel otherPanel;
    private ImagesPanel imagesPanel;
    private JButton saveSolution;
    private JButton saveSettings;
    private JButton loadSettings;
    private CrosshairFrame crosshairFrame;


    public SavePanel() {}

    public void initialisePanel( CrosshairFrame crosshairFrame ) {
        this.crosshairFrame = crosshairFrame;
        this.planeManager = crosshairFrame.getPlaneManager();
        this.microtomeManager = crosshairFrame.getMicrotomeManager();
        this.imageContent = crosshairFrame.getImageContent();
        this.microtomePanel = crosshairFrame.getMicrotomePanel();
        this.otherPanel = crosshairFrame.getPointsPanel();
        this.imagesPanel = crosshairFrame.getImagesPanel();

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Save and Load"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        setLayout(new GridLayout(1, 3));

        ActionListener saveLoadListener = new saveLoadListener();

        saveSettings = new JButton("Save Settings");
        saveSettings.setActionCommand("save_settings");
        saveSettings.addActionListener(saveLoadListener);
        add(saveSettings);
        PlanePanel planePanel = crosshairFrame.getPlanePanel();
        planePanel.addButtonAffectedByTracking( Crosshair.target, saveSettings );
        planePanel.addButtonAffectedByTracking( Crosshair.block, saveSettings );

        loadSettings = new JButton("Load Settings");
        loadSettings.setActionCommand("load_settings");
        loadSettings.addActionListener(saveLoadListener);
        add(loadSettings);
        planePanel.addButtonAffectedByTracking( Crosshair.target, loadSettings );
        planePanel.addButtonAffectedByTracking( Crosshair.block, loadSettings );

        saveSolution = new JButton("Save Solution");
        saveSolution.setActionCommand("save_solution");
        saveSolution.addActionListener(saveLoadListener);
        saveSolution.setEnabled(false);
        add(saveSolution);
    }

    void enableSaveSolution() {
        saveSolution.setEnabled(true);
    }

    void disableSaveSolution() {
        saveSolution.setEnabled(false);
    }

    class saveLoadListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("save_settings")) {

                String filePath = chooseSaveFilePath();
                if ( filePath != null ) {
                    Settings settings = createSettings();
                    writeSettings( settings, filePath );
                }

            } else if (e.getActionCommand().equals("load_settings")) {

                String filePath = chooseOpenFilePath();
                if ( filePath != null ) {
                    Settings settings = readSettings( filePath );
                    if ( settings != null ) {
                        loadSettings( settings );
                    }
                }

            } else if (e.getActionCommand().equals("save_solution")) {
                if ( microtomeManager.isValidSolution() ) {
                    String filePath = chooseSaveFilePath();

                    if ( filePath != null ) {
                        // if you set a solution then move other sliders, it will no longer be a solution. Here we force it
                        // to react and set all sliders for that current solution.
                        double currentSolutionRot = microtomePanel.getRotationSolutionAngle().getCurrentValue();
                        microtomePanel.getRotationSolutionAngle().setCurrentValue(currentSolutionRot);

                        Solution solution = microtomeManager.getCurrentSolution();
                        writeSolution( solution, filePath );

                    }
                } else {
                    JOptionPane.showMessageDialog(null,
                            "No valid solutions for this rotation. Try another rotation, or revise your target plane.");
                }
            }
        }
    }

    private String chooseSaveFilePath() {
        String filePath = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("json", "json"));
        int returnVal = chooser.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            filePath = chooser.getSelectedFile().getAbsolutePath() + ".json";
        }

        return filePath;
    }

    private String chooseOpenFilePath() {
        String filePath = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("json", "json"));
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            filePath = chooser.getSelectedFile().getAbsolutePath();
        }

        return filePath;
    }

    private Settings createSettings() {
        Settings settings = new Settings();

        List<PlaneSettings> planeSettings = new ArrayList<>();
        for ( Plane plane: planeManager.getPlanes() ) {
            planeSettings.add( plane.getSettings() );
        }
        settings.planeSettings = planeSettings;

        List<ImageContentSettings> imageSettings = new ArrayList<>();
        Map<String, Content> imageNameTocontent = imagesPanel.getImageNameToContent();
        for ( String imageName: imageNameTocontent.keySet() ) {

            Content imageContent = imageNameTocontent.get( imageName );

            // transfer function settings
            int[] redLut = new int[256];
            int[] greenLut = new int[256];
            int[] blueLut = new int[256];
            int[] alphaLut = new int[256];

            imageContent.getRedLUT(redLut);
            imageContent.getGreenLUT(greenLut);
            imageContent.getBlueLUT(blueLut);
            imageContent.getAlphaLUT(alphaLut);

            imageSettings.add(
                    new ImageContentSettings( imageName, imageContent.getTransparency(),
                    imageContent.getColor(), redLut, greenLut, blueLut, alphaLut ) ) ;
        }

        settings.imageSettings = imageSettings;

        return settings;
    }

    private void writeSettings( Settings settings, String filePath ) {
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson( settings, fileWriter);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void writeSolution( Solution solution, String filePath ) {
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson( solution, fileWriter);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private Settings readSettings( String filePath ) {
        Gson gson = new GsonBuilder().
                registerTypeAdapter( new TypeToken<List<PlaneSettings>>(){}.getType(), new PlaneSettingsListDeserializer()).create();

        try {
            FileReader fileReader = new FileReader(filePath);
            return gson.fromJson(fileReader, Settings.class);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return null;
        }
    }

    private void loadImageSettings( Content imageContent, ImageContentSettings imageSettings ) {
        imageContent.setColor( imageSettings.imageColour );
        imageContent.setTransparency( imageSettings.imageTransparency );

        // TODO - if null, set to the default lut, need to look up what this is and recreate it
        if ( imageSettings.redLut != null & imageSettings.greenLut != null &
                imageSettings.blueLut != null & imageSettings.alphaLut != null) {
            // transfer function
            imageContent.setLUT( imageSettings.redLut, imageSettings.greenLut,
                    imageSettings.blueLut, imageSettings.alphaLut );
        }
    }

    private void loadSettings( Settings settings ) {
        if ( microtomeManager.isMicrotomeModeActive() ) {
            microtomePanel.exitMicrotomeMode();
        }

        if ( !planeManager.isTrackingPlane() ) {

            // setup plane settings
            // make a copy, so not modifying as we loop
            ArrayList<String> planeNames = new ArrayList<>( planeManager.getPlaneNames() );
            for ( String planeName : planeNames ) {
                planeManager.removeNamedPlane(planeName);
            }

            for ( PlaneSettings planeSettings: settings.planeSettings ) {
                if ( planeSettings instanceof BlockPlaneSettings ) {
                    planeManager.addBlockPlane( (BlockPlaneSettings) planeSettings );
                } else {
                    planeManager.addPlane( planeSettings );
                }
            }

            // setup image settings
            Map<String, Content> imageNameToContent = imagesPanel.getImageNameToContent();
            for ( ImageContentSettings imageSettings: settings.imageSettings ) {
                loadImageSettings( imageNameToContent.get( imageSettings.name ), imageSettings );
            }

            if ( !otherPanel.check3DPointsVisible() ) {
                otherPanel.toggleVisiblity3DPoints();
            }
        } else {
            IJ.log("Cant load settings when tracking a plane");
        }

    }
}
