package de.embl.cba.crosshair.ui.swing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.embl.cba.crosshair.*;
import de.embl.cba.crosshair.bdv.PointsOverlaySizeChange;
import de.embl.cba.crosshair.io.SettingsToSave;
import de.embl.cba.crosshair.io.SolutionToSave;
import de.embl.cba.crosshair.microtome.MicrotomeManager;
import ij.IJ;
import ij3d.Content;
import net.imglib2.RealPoint;
import org.scijava.vecmath.Vector3d;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class SavePanel extends JPanel {
    private PlaneManager planeManager;
    private MicrotomeManager microtomeManager;
    private Content imageContent;
    private MicrotomePanel microtomePanel;
    private PointsPanel pointsPanel;
    private PointsOverlaySizeChange pointOverlay;
    private JButton saveSolution;


    public SavePanel(PlaneManager planeManager, MicrotomeManager microtomeManager, Content imageContent, MicrotomePanel microtomePanel, PointsPanel pointsPanel,
                     PointsOverlaySizeChange pointOverlay) {
        this.planeManager = planeManager;
        this.microtomeManager = microtomeManager;
        this.imageContent = imageContent;
        this.microtomePanel = microtomePanel;
        this.pointsPanel = pointsPanel;
        this.pointOverlay = pointOverlay;

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Save and Load"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        setLayout(new GridLayout(1, 3));

        ActionListener saveLoadListener = new saveLoadListener();

        JButton saveSettings = new JButton("Save Settings");
        saveSettings.setActionCommand("save_settings");
        saveSettings.addActionListener(saveLoadListener);
        add(saveSettings);

        JButton loadSettings = new JButton("Load Settings");
        loadSettings.setActionCommand("load_settings");
        loadSettings.addActionListener(saveLoadListener);
        add(loadSettings);

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

                String filePath = "";
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("json", "json"));
                int returnVal = chooser.showSaveDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    filePath = chooser.getSelectedFile().getAbsolutePath() + ".json";
                }

                if (filePath != "") {

                    // transfer function settings
                    int[] redLut = new int[256];
                    int[] greenLut = new int[256];
                    int[] blueLut = new int[256];
                    int[] alphaLut = new int[256];
                    imageContent.getRedLUT(redLut);
                    imageContent.getGreenLUT(greenLut);
                    imageContent.getBlueLUT(blueLut);
                    imageContent.getAlphaLUT(alphaLut);

                    SettingsToSave settingsToSave = new SettingsToSave(planeManager.getPlaneNormals(), planeManager.getPlanePoints(),
                            planeManager.getNamedVertices(), planeManager.getPoints(),
                            planeManager.getBlockVertices(), planeManager.getTargetPlaneColour(), planeManager.getBlockPlaneColour(),
                            planeManager.getTargetTransparency(), planeManager.getBlockTransparency(),
                            imageContent.getTransparency(), imageContent.getColor(), redLut, greenLut, blueLut, alphaLut
                    );

                    try {
                        FileWriter fileWriter = new FileWriter(filePath);
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        gson.toJson(settingsToSave, fileWriter);
                        fileWriter.flush();
                        fileWriter.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }

            } else if (e.getActionCommand().equals("load_settings")) {
                String filePath = "";
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("json", "json"));
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    filePath = chooser.getSelectedFile().getAbsolutePath();
                }

                if (filePath != "") {
                    Gson gson = new Gson();
                    try {
                        FileReader fileReader = new FileReader(filePath);
                        SettingsToSave settingsToSave = gson.fromJson(fileReader, SettingsToSave.class);
                        loadSettings(settingsToSave);

                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    }
                }

            } else if (e.getActionCommand().equals("save_solution")) {
                if ( microtomeManager.isValidSolution() ) {
                    String filePath = "";
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileFilter(new FileNameExtensionFilter("json", "json"));
                    int returnVal = chooser.showSaveDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        filePath = chooser.getSelectedFile().getAbsolutePath() + ".json";
                    }

                    if (filePath != "") {
                        // if you set a solution then move other sliders, it will no longer be a solution. Here we force it
                        // to react and set all sliders for that current solution.
                        double currentSolutionRot = microtomePanel.getRotationSolutionAngle().getCurrentValue();
                        microtomePanel.getRotationSolutionAngle().setCurrentValue(currentSolutionRot);

                        SolutionToSave solutionToSave = microtomeManager.getCurrentSolution();

                        try {
                            FileWriter fileWriter = new FileWriter(filePath);
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            gson.toJson(solutionToSave, fileWriter);
                            fileWriter.flush();
                            fileWriter.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }

                    }
                } else {
                    JOptionPane.showMessageDialog(null, "No valid solutions for this rotation. Try another rotation, or revise your target plane.");
                }
            }
        }
    }

    private void loadSettings(SettingsToSave settingsToSave) {
        if (microtomeManager.isMicrotomeModeActive()) {
            microtomePanel.exitMicrotomeMode();
        }

        if (planeManager.getTrackPlane() == 0) {
            planeManager.removeAllBlockVertices();
            planeManager.removeAllPoints();

            Map<String, Vector3d> settingsPlaneNormals = settingsToSave.getPlaneNormals();
            for (String planeName : settingsPlaneNormals.keySet()) {
                planeManager.updatePlane(settingsPlaneNormals.get(planeName), settingsToSave.getPlanePoints().get(planeName), planeName);
            }

            // if some planes aren't defined in loaded settings, remove them if present
            for (String plane : new String[]{"target", "block"}) {
                if (!settingsPlaneNormals.containsKey(plane)) {
                    planeManager.removeNamedPlane(plane);
                }
            }

            planeManager.setTargetPlaneColour(settingsToSave.getTargetPlaneColour().get());
            planeManager.setBlockPlaneColour(settingsToSave.getBlockPlaneColour().get());
            planeManager.setTargetTransparency(settingsToSave.getTargetTransparency());
            planeManager.setBlockTransparency(settingsToSave.getBlockTransparency());

            for (RealPoint point : settingsToSave.getPoints()) {
                planeManager.addRemovePointFromPointList(planeManager.getPoints(), point);
            }

//            TODO - should do this still with checks that it lies on teh plane
            for (RealPoint point : settingsToSave.getBlockVertices()) {
                planeManager.addRemovePointFromPointList(planeManager.getBlockVertices(), point);
            }

            for (Map.Entry<String, RealPoint> entry : settingsToSave.getNamedVertices().entrySet()) {
                planeManager.nameVertex(entry.getKey(), entry.getValue());
            }

            imageContent.setColor(settingsToSave.getImageColour());
            imageContent.setTransparency(settingsToSave.getImageTransparency());

//            TODO - if null, set to teh default lut, need to look up what this is and recreate it
            if (settingsToSave.getRedLut() != null & settingsToSave.getBlueLut() != null & settingsToSave.getGreenLut() != null & settingsToSave.getAlphaLut() != null) {
//            transfer function
                imageContent.setLUT(settingsToSave.getRedLut(), settingsToSave.getGreenLut(), settingsToSave.getBlueLut(), settingsToSave.getAlphaLut());
            }

//        Set everything to be visible if not already
            if (!planeManager.getVisiblityNamedPlane("target")) {
                planeManager.toggleTargetVisbility();
            }

            if (!planeManager.getVisiblityNamedPlane("block")) {
                planeManager.toggleBlockVisbility();
            }

            if (!pointsPanel.check3DPointsVisible()) {
                pointsPanel.toggleVisiblity3DPoints();
            }

            if (!pointOverlay.checkPointsVisible()) {
                pointOverlay.toggleShowPoints();
            }
        } else {
            IJ.log("Cant load settings when tracking a plane");
        }

    }
}
