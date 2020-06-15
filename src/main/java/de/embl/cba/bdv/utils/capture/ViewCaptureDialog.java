package de.embl.cba.bdv.utils.capture;

import bdv.util.BdvHandle;
import ij.gui.NonBlockingGenericDialog;

import static de.embl.cba.bdv.utils.BdvUtils.getViewerVoxelSpacing;

public class ViewCaptureDialog implements Runnable
{
	private BdvHandle bdvHandle;

	public ViewCaptureDialog( BdvHandle bdvHandle )
	{
		this.bdvHandle = bdvHandle;
	}

	@Override
	public void run()
	{
		final String pixelUnit = "micrometer";
		final NonBlockingGenericDialog gd = new NonBlockingGenericDialog( "Pixel Spacing" );
		gd.addNumericField( "Pixel Spacing", getViewerVoxelSpacing( bdvHandle ), 3,  10, pixelUnit );
		gd.addCheckbox( "Show raw data", true );
		gd.showDialog();
		if( gd.wasCanceled() ) return;
		final double pixelSpacing = gd.getNextNumber();
		final boolean showRawData = gd.getNextBoolean();

		// TODO: make own class of captureView!
		final ViewCaptureResult viewCaptureResult = BdvViewCaptures.captureView(
				bdvHandle,
				pixelSpacing,
				pixelUnit,
				false );
		viewCaptureResult.rgbImage.show();

		if ( showRawData )
			viewCaptureResult.rawImagesStack.show();

	}
}
