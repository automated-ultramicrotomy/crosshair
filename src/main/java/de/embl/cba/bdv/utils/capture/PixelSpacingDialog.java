package de.embl.cba.bdv.utils.capture;

import ij.gui.NonBlockingGenericDialog;

public class PixelSpacingDialog
{

	double pixelSpacing;
	private final String pixelUnit;

	public PixelSpacingDialog( double pixelSpacing, String pixelUnit )
	{
		this.pixelSpacing = pixelSpacing;
		this.pixelUnit = pixelUnit;
	}

	public boolean showDialog()
	{
		final NonBlockingGenericDialog gd = new NonBlockingGenericDialog( "Pixel Spacing" );
		gd.addNumericField( "Pixel Spacing", pixelSpacing, 3,  10, pixelUnit );
		gd.showDialog();
		if( gd.wasCanceled() ) return false;
		pixelSpacing = gd.getNextNumber();
		return true;
	}

	public double getPixelSpacing()
	{
		return pixelSpacing;
	}
}
