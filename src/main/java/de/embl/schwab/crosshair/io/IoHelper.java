package de.embl.schwab.crosshair.io;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class IoHelper {

    private static String lastDirPath;

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
