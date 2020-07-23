package de.embl.cba.targeting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import ij3d.Content;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SavePanel extends JPanel {
    private PlaneManager planeManager;
    private Content imageContent;


    public SavePanel(PlaneManager planeManager, Content imageContent) {
        this.planeManager = planeManager;
        this.imageContent = imageContent;

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

        JButton saveSolution = new JButton("Save Solution");
        saveSolution.setActionCommand("save_solution");
        saveSolution.addActionListener(saveLoadListener);
        add(saveSolution);


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
                    // TODO - make it possible to save when e.g. some of teh planes aren't initialised yet, or not all vertices named etc...
                    SettingsToSave settingsToSave = new SettingsToSave(planeManager.getPlaneNormals(), planeManager.getPlanePoints(),
                            planeManager.getPlaneCentroids(), planeManager.getNamedVertices(), planeManager.getPoints(),
                            planeManager.getBlockVertices(), planeManager.getTargetPlaneColour(), planeManager.getBlockPlaneColour(),
                            planeManager.getTargetTransparency(), planeManager.getBlockTransparency(),
                            imageContent.getTransparency(), imageContent.getColor());

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
                        // TODO - use this object to set all teh required settings

                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    }
                }

            } else if (e.getActionCommand().equals("save_solution")) {

            }
        }
    }
}
