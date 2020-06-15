package de.embl.cba.tables.command;

import de.embl.cba.tables.ExploreIntensityImageAndLabelImageAndTable;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;


@Plugin(type = Command.class,
		menuPath = "Plugins>Segmentation>Explore>Explore Intensity Image and Label Image and Table" )
public class ExploreIntensityImageAndLabelImageAndTableCommand implements Command
{

	@Parameter ( label = "Intensity image", required = false )
	public File intensityImage;

	@Parameter ( label = "Label mask image" )
	public File labelImage;

	@Parameter ( label = "Object table" )
	public File objectTable;

	@Parameter ( label = "Time points in table are one based" )
	public boolean timePointsInTableAreOneBased = true;

	@Parameter ( label = "Coordinates in table are in calibrated units" )
	private boolean coordinatesInTableAreCalibrated = false;

	@Override
	public void run()
	{
		new ExploreIntensityImageAndLabelImageAndTable(
				intensityImage.getAbsolutePath(),
				labelImage.getAbsolutePath(),
				objectTable.getAbsolutePath(),
				timePointsInTableAreOneBased,
				coordinatesInTableAreCalibrated );
	}

}
