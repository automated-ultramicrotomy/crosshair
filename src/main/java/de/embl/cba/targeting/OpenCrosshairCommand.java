package de.embl.cba.targeting;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>Crosshair" )
public class OpenCrosshairCommand implements Command
{
    @Parameter ( label = "Image Location" , style="directory")
    public String imageLocation;

    @Override
    public void run()
    {
        final Crosshair crosshair = new Crosshair( imageLocation );
    }
}
