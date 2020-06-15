package de.embl.cba.tables.command;

import de.embl.cba.tables.morpholibj.ExploreMorphoLibJLabelImage;
import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;


@Plugin(type = Command.class,
		menuPath = "Plugins>Segmentation>Explore>Explore MorphoLibJ Segmentation" )
public class ExploreMorphoLibJLabelImageCommand implements Command
{

	@Parameter ( label = "Intensity image", required = false )
	public ImagePlus intensityImage;

	@Parameter ( label = "Label mask image" )
	public ImagePlus labelImage;

	@Parameter ( label = "Results table title" )
	public String resultsTableTitle;

	@Override
	public void run()
	{
		new ExploreMorphoLibJLabelImage(
						intensityImage,
						labelImage,
						resultsTableTitle );
	}

}
