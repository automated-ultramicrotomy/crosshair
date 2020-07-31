package de.embl.cba.targeting.ui.command;
import de.embl.cba.targeting.Crosshair;
import ij.ImagePlus;
import ij.WindowManager;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>Crosshair>Open Current Image..." )
public class OpenCrosshairFromCurrentImageCommand implements Command
{
    @Override
    public void run()
    {
        final ImagePlus imagePlus = WindowManager.getCurrentImage();
        new Crosshair(imagePlus);
    }
}
