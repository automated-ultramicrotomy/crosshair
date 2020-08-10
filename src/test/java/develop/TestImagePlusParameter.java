package develop;

import de.embl.schwab.crosshair.ui.command.OpenCrosshairFromCurrentImageCommand;

import ij.ImagePlus;
import net.imagej.ImageJ;
import org.scijava.command.CommandInfo;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;

public class TestImagePlusParameter
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		final CommandInfo commandInfo = new CommandInfo( OpenCrosshairFromCurrentImageCommand.class );

		final Module module = imageJ.module().createModule( commandInfo );
		final ModuleItem< ImagePlus > singleInput = imageJ.module().getSingleInput( module, ImagePlus.class );
		System.out.println( singleInput );
	}
}
