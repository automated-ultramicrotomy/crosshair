package de.embl.schwab.crosshair.ui.command;

import bdv.cache.SharedQueue;
import de.embl.schwab.crosshair.Crosshair;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import org.embl.mobie.io.imagedata.N5ImageData;

import javax.swing.*;

/**
 * ImageJ command to open Crosshair from local ome-zarr file
 */
@Plugin(type = Command.class, menuPath = "Plugins>Crosshair>Open>Target Ome-Zarr File (Local)" )
public class OpenCrosshairFromOmeZarrLocal implements Command {
    public String omeZarrFilePath;

    // Can't use @Parameter for File, as this seems to affect the appearance of the Swing panels for crosshair,
    // instead use JFileChooser

    @Override
    public void run() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            omeZarrFilePath = chooser.getSelectedFile().getAbsolutePath();
            openPathInCrosshair();
        }
    }

    public void openPathInCrosshair() {
        N5ImageData< ? > imageData = new N5ImageData<>(
                omeZarrFilePath,
                new SharedQueue( Math.max( 1, Runtime.getRuntime().availableProcessors() / 2 ) )
        );
        new Crosshair(imageData);
    }
}
