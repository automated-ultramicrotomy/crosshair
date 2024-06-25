package de.embl.schwab.crosshair.io;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

/**
 * Class with IO utility functions
 */
public class IoHelper {

    private static String lastDirPath;

    /**
     * Opens a file chooser to select a json file
     * @return filepath of the chosen file
     */
    public static String chooseOpenFilePath() {
        String filePath = null;
        JFileChooser chooser = new JFileChooser(lastDirPath);
        chooser.setFileFilter(new FileNameExtensionFilter("json", "json"));
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            filePath = chooser.getSelectedFile().getAbsolutePath();
            lastDirPath = new File(filePath).getParent();
        }

        return filePath;
    }

    /**
     * Opens a file chooser to select where to save a json file
     * @return filepath to save json
     */
    public static String chooseSaveFilePath() {
        String filePath = null;
        JFileChooser chooser = new JFileChooser(lastDirPath);
        chooser.setFileFilter(new FileNameExtensionFilter("json", "json"));
        int returnVal = chooser.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            filePath = chooser.getSelectedFile().getAbsolutePath() + ".json";
            lastDirPath = new File(filePath).getParent();
        }

        return filePath;
    }

}
