package de.embl.schwab.crosshair.io;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class IoHelper {

    public static String chooseOpenFilePath() {
        String filePath = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("json", "json"));
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            filePath = chooser.getSelectedFile().getAbsolutePath();
        }

        return filePath;
    }

    public static String chooseSaveFilePath() {
        String filePath = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("json", "json"));
        int returnVal = chooser.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            filePath = chooser.getSelectedFile().getAbsolutePath() + ".json";
        }

        return filePath;
    }

}
