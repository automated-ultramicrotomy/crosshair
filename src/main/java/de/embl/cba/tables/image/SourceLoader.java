package de.embl.cba.tables.image;

import bdv.util.RandomAccessibleIntervalSource4D;
import de.embl.cba.tables.Logger;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import java.io.File;

public class SourceLoader
{
	private final File file;
	private int numSpatialDimensions;

	public SourceLoader( File file )
	{
		this.file = file;
	}

	public RandomAccessibleIntervalSource4D getRandomAccessibleIntervalSource4D( )
	{
		final ImagePlus imagePlus = IJ.openImage( file.toString() );

		if ( imagePlus.getNChannels() > 1 )
		{
			Logger.error( "Only single channel image are supported.");
			return null;
		}

		RandomAccessibleInterval< RealType > wrap = ImageJFunctions.wrapReal( imagePlus );

		if ( imagePlus.getNFrames() == 1 )
		{
			// needs to be a movie
			wrap = Views.addDimension( wrap, 0, 0 );
		}


		if ( imagePlus.getNSlices() == 1 )
		{
			numSpatialDimensions = 2;
			// needs to be 3D
			wrap = Views.addDimension( wrap, 0, 0 );
			wrap = Views.permute( wrap, 2, 3 );
		}
		else
		{
			numSpatialDimensions = 2;
		}

		return new RandomAccessibleIntervalSource4D( wrap, Util.getTypeFromInterval( wrap ), imagePlus.getTitle() );
	}

	public int getNumSpatialDimensions()
	{
		return numSpatialDimensions;
	}
}
