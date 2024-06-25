package de.embl.schwab.crosshair.ui.command;

import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.schwab.crosshair.Crosshair;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

@Plugin(type = Command.class, menuPath = "Plugins>Crosshair>Open>Target Bdv File" )
public class OpenCrosshairFromBdvXmlCommand implements Command {

    public String bdvXmlFilePath;

    // Can't use @Parameter for File, as this seems to affect the appearance of the Swing panels for crosshair,
    // instead use JFileChooser

    @Override
    public void run() {

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("xml", "xml"));
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            bdvXmlFilePath = chooser.getSelectedFile().getAbsolutePath();
            openPathInCrosshair();

        }
    }

    public void openPathInCrosshair() {
        final LazySpimSource imageSource = new LazySpimSource("raw", bdvXmlFilePath);
        new Crosshair(imageSource);
    }
}
