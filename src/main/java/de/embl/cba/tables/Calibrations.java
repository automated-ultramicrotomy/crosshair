package de.embl.cba.tables;

import ij.ImagePlus;
import net.imglib2.realtransform.AffineTransform3D;

public class Calibrations
{
	public static AffineTransform3D getScalingTransform( ImagePlus imagePlus )
	{
		final AffineTransform3D scaling = new AffineTransform3D();
		scaling.set( imagePlus.getCalibration().pixelWidth, 0, 0 );
		scaling.set( imagePlus.getCalibration().pixelHeight, 1, 1 );
		scaling.set( imagePlus.getCalibration().pixelDepth, 2, 2 );
		return scaling;
	}
}
