package de.embl.cba.bdv.utils.capture;

import bdv.cache.CacheControl;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.util.Prefs;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;

import bdv.viewer.ViewerPanel;
import bdv.viewer.overlay.ScaleBarOverlayRenderer;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.state.ViewerState;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.FileUtils;
import de.embl.cba.bdv.utils.sources.ARGBConvertedRealSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.bdv.utils.sources.Sources;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.process.LUT;
import net.imglib2.*;
import net.imglib2.Cursor;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.converter.Converter;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static de.embl.cba.bdv.utils.BdvUtils.*;


/**
 * TODO:
 * - Rather return an ImagePlus and let the user decide what to do with it
 * - Implement different rendering modes for different image modalities
 *
 */
public abstract class BdvViewCaptures < R extends RealType< R > >
{

	/**
	 * @param bdv
	 * @param pixelSpacing
	 * @param voxelUnit
	 * @return
	 *
	 * TODO: Replace this with bdv-playground!!
	 */
	public static synchronized < R extends RealType< R > > ViewCaptureResult captureView(
			BdvHandle bdv,
			double pixelSpacing,
			String voxelUnit,
			boolean checkSourceIntersectionWithViewerPlaneOnlyIn2D )
	{
		final AffineTransform3D viewerTransform = new AffineTransform3D();
		bdv.getViewerPanel().getState().getViewerTransform( viewerTransform );

		final double viewerVoxelSpacing = getViewerVoxelSpacing( bdv );
		double dxy = pixelSpacing / viewerVoxelSpacing;

		final int w = getBdvWindowWidth( bdv );
		final int h = getBdvWindowHeight( bdv );

		final long captureWidth = ( long ) Math.ceil( w / dxy );
		final long captureHeight = ( long ) Math.ceil( h / dxy );

		final ArrayList< RandomAccessibleInterval< UnsignedShortType > > captures = new ArrayList<>();
		final ArrayList< ARGBType > colors = new ArrayList<>();
		final ArrayList< Boolean > isSegmentations = new ArrayList<>();
		final ArrayList< double[] > displayRanges = new ArrayList<>();

		final List< Integer > sourceIndices = getVisibleSourceIndices( bdv );

		final int t = bdv.getViewerPanel().getState().getCurrentTimepoint();

		final RandomAccessibleInterval< ARGBType > argbCapture
				= ArrayImgs.argbs( captureWidth, captureHeight );

		for ( int sourceIndex : sourceIndices )
		{
			if ( checkSourceIntersectionWithViewerPlaneOnlyIn2D )
				if ( ! BdvUtils.isSourceIntersectingCurrentViewIn2D( bdv, sourceIndex ) ) continue;
			else
				if ( ! BdvUtils.isSourceIntersectingCurrentView( bdv, sourceIndex ) ) continue;

			final RandomAccessibleInterval< UnsignedShortType > realCapture
					= ArrayImgs.unsignedShorts( captureWidth, captureHeight );

			Source< ? > source = getSource( bdv, sourceIndex );
			final Converter converter = (Converter) getConverter( bdv, sourceIndex );

			final int level = getLevel( source, pixelSpacing );
			final AffineTransform3D sourceTransform =
					BdvUtils.getSourceTransform( source, t, level );

			AffineTransform3D viewerToSourceTransform = new AffineTransform3D();
			viewerToSourceTransform.preConcatenate( viewerTransform.inverse() );
			viewerToSourceTransform.preConcatenate( sourceTransform.inverse() );

			final boolean interpolate = isInterpolate( source );
			isSegmentations.add( ! interpolate );

			Grids.collectAllContainedIntervals(
					Intervals.dimensionsAsLongArray( argbCapture ),
					new int[]{100, 100}).parallelStream().forEach( interval ->
			{
				RealRandomAccess< ? extends RealType< ? > > sourceRealTypeAccess =
						getInterpolatedRealRandomAccess( t, source, level, interpolate );

				RealRandomAccess< ? > sourceAccess = null;
				if ( interpolate )
					sourceAccess = source.getInterpolatedSource( t, level, Interpolation.NLINEAR ).realRandomAccess();
				else
					sourceAccess = source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR ).realRandomAccess();

				// to collect raw data
				final IntervalView< UnsignedShortType > realCrop = Views.interval( realCapture, interval );
				final Cursor< UnsignedShortType > realCaptureCursor = Views.iterable( realCrop ).localizingCursor();
				final RandomAccess< UnsignedShortType > realCaptureAccess = realCrop.randomAccess();

				// to collect coloured data
				final IntervalView< ARGBType > argbCrop = Views.interval( argbCapture, interval );
				final RandomAccess< ARGBType > argbCaptureAccess = argbCrop.randomAccess();

				final double[] canvasPosition = new double[ 3 ];
				final double[] sourceRealPosition = new double[ 3 ];

				final ARGBType argbType = new ARGBType();


				while ( realCaptureCursor.hasNext() )
				{
					realCaptureCursor.fwd();
					realCaptureCursor.localize( canvasPosition );
					realCaptureAccess.setPosition( realCaptureCursor );

					argbCaptureAccess.setPosition( realCaptureCursor );

					// canvasPosition is the position on the canvas, in calibrated units
					// dxy is the step size that is needed to get the desired resolution in the
					// output image
					canvasPosition[ 0 ] *= dxy;
					canvasPosition[ 1 ] *= dxy;

					viewerToSourceTransform.apply( canvasPosition, sourceRealPosition );
					sourceRealTypeAccess.setPosition( sourceRealPosition );
					final RealType< ? > realType = sourceRealTypeAccess.get();
					realCaptureAccess.get().setReal( realType.getRealDouble() );


					sourceAccess.setPosition( sourceRealPosition );
					final Object pixel = sourceAccess.get();
					if ( pixel instanceof ARGBType )
						argbType.set( ( ARGBType ) pixel );
					else
						converter.convert( pixel, argbType );

					final int sourceARGBIndex = argbType.get();
					final int captureARGBIndex = argbCaptureAccess.get().get();
					int a = ARGBType.alpha( sourceARGBIndex ) + ARGBType.alpha( captureARGBIndex );
					int r = ARGBType.red( sourceARGBIndex ) + ARGBType.red( captureARGBIndex );
					int g = ARGBType.green( sourceARGBIndex )+ ARGBType.green( captureARGBIndex );
					int b = ARGBType.blue( sourceARGBIndex )+ ARGBType.blue( captureARGBIndex );

					if ( a > 255 )
						a = 255;
					if ( r > 255 )
						r = 255;
					if ( g > 255 )
						g = 255;
					if ( b > 255 )
						b = 255;


					argbCaptureAccess.get().set( ARGBType.rgba( r, g, b, a ) );


				}
			});

			captures.add( realCapture );
			// colors.add( getSourceColor( bdv, sourceIndex ) ); Not used, show GrayScale
			displayRanges.add( BdvUtils.getDisplayRange( bdv, sourceIndex) );
		}

		final double[] voxelSpacing = new double[ 3 ];
		for ( int d = 0; d < 2; d++ )
			voxelSpacing[ d ] = pixelSpacing;

		voxelSpacing[ 2 ] = viewerVoxelSpacing; // TODO: ???

		if ( captures.size() > 0 )
		{
			final ViewCaptureResult compositeAndRGBImage = new ViewCaptureResult();
			compositeAndRGBImage.rgbImage = createRgbImage(
					voxelUnit, argbCapture, voxelSpacing );
			compositeAndRGBImage.rawImagesStack = createCompositeImage(
					voxelSpacing, voxelUnit, captures, colors, displayRanges, isSegmentations );
			return compositeAndRGBImage;
		}
		else
			return null;
	}

	private static ImagePlus createRgbImage( String voxelUnit, RandomAccessibleInterval< ARGBType > argbCapture, double[] voxelSpacing )
	{
		final ImagePlus rgbImage = ImageJFunctions.wrap( argbCapture, "View Capture RGB" );

		IJ.run( rgbImage,
				"Properties...",
				"channels=" + 1
						+" slices=1 frames=1 unit=" + voxelUnit
						+" pixel_width=" + voxelSpacing[ 0 ]
						+" pixel_height=" + voxelSpacing[ 1 ]
						+" voxel_depth=" + voxelSpacing[ 2 ] );
		return rgbImage;
	}

	public static RealRandomAccess< ? extends RealType< ? > >
	getInterpolatedRealRandomAccess( int t, Source< ? > source, int level, boolean interpolate )
	{
		RealRandomAccess< ? extends RealType< ? > > sourceAccess;
		if ( interpolate )
			sourceAccess = getInterpolatedRealTypeNonVolatileRealRandomAccess( source, t, level, Interpolation.NLINEAR );
		else
			sourceAccess = getInterpolatedRealTypeNonVolatileRealRandomAccess( source, t, level, Interpolation.NEARESTNEIGHBOR );

		return sourceAccess;
	}

	public static boolean isInterpolate( Source< ? > source )
	{
		if ( source instanceof TransformedSource )
			source = ((TransformedSource)source).getWrappedSource();

		if ( source instanceof ARGBConvertedRealSource )
			source = ((ARGBConvertedRealSource)source).getWrappedSource();

		boolean interpolate = true;
		if ( Sources.sourceToMetadata.containsKey( source ) )
		{
			final Metadata metadata = Sources.sourceToMetadata.get( source );
			if ( metadata.modality.equals( Metadata.Modality.Segmentation ) )
				interpolate = false;
		}
		return interpolate;
	}

	public static CompositeImage createCompositeImage(
			double[] voxelSpacing,
			String voxelUnit,
			ArrayList< RandomAccessibleInterval< UnsignedShortType > > rais,
			ArrayList< ARGBType > colors,
			ArrayList< double[] > displayRanges,
			ArrayList< Boolean > isSegmentations )
	{
		final RandomAccessibleInterval< UnsignedShortType > stack = Views.stack( rais );

		final ImagePlus imp = ImageJFunctions.wrap( stack, "View Capture Raw" );

		// duplicate: otherwise it is virtual and cannot be modified
		final ImagePlus dup = new Duplicator().run( imp );

		IJ.run( dup,
				"Properties...",
				"channels="+rais.size()
						+" slices=1 frames=1 unit=" + voxelUnit
						+" pixel_width=" + voxelSpacing[ 0 ]
						+" pixel_height=" + voxelSpacing[ 1 ]
						+" voxel_depth=" + voxelSpacing[ 2 ] );

		final CompositeImage compositeImage = new CompositeImage( dup );

		for ( int channel = 1; channel <= compositeImage.getNChannels(); ++channel )
		{
			final LUT lut = compositeImage.createLutFromColor( Color.WHITE );
			compositeImage.setC( channel );
			compositeImage.setChannelLut( lut );
			final double[] range = displayRanges.get( channel - 1 );
			compositeImage.setDisplayRange( range[ 0 ], range[ 1 ] );
		}

		compositeImage.setTitle( "View Capture Raw" );
		return compositeImage;
	}

	public static void saveScreenShot( final File outputFile, ViewerPanel viewer )
	{
		saveScreenShot( outputFile, viewer, viewer.getDisplay().getWidth(), viewer.getDisplay().getHeight() );
	}

	private static void saveScreenShot( final File outputFile, ViewerPanel viewer, int width, int height )
	{
		final ViewerState renderState = viewer.getState();

		final AffineTransform3D affine = new AffineTransform3D();
		renderState.getViewerTransform( affine );
		affine.set( affine.get( 0, 3 ) - width / 2, 0, 3 );
		affine.set( affine.get( 1, 3 ) - height / 2, 1, 3 );
		affine.scale( ( double ) width / width );
		affine.set( affine.get( 0, 3 ) + width / 2, 0, 3 );
		affine.set( affine.get( 1, 3 ) + height / 2, 1, 3 );
		renderState.setViewerTransform( affine );

		final ScaleBarOverlayRenderer scalebar = Prefs.showScaleBarInMovie() ? new ScaleBarOverlayRenderer() : null;

		class MyTarget implements RenderTarget
		{
			BufferedImage bi;

			@Override
			public BufferedImage setBufferedImage( final BufferedImage bufferedImage )
			{
				bi = bufferedImage;
				return null;
			}

			@Override
			public int getWidth()
			{
				return width;
			}

			@Override
			public int getHeight()
			{
				return height;
			}
		}

		final MyTarget target = new MyTarget();
		final MultiResolutionRenderer renderer = new MultiResolutionRenderer(
				target, new PainterThread( null ), new double[] { 1 }, 0, false, 1, null, false,
				viewer.getOptionValues().getAccumulateProjectorFactory(), new CacheControl.Dummy() );

		int minTimepointIndex = 0;
		int maxTimepointIndex = 0;

		for ( int timepoint = minTimepointIndex; timepoint <= maxTimepointIndex; ++timepoint )
		{
			renderState.setCurrentTimepoint( timepoint );
			renderer.requestRepaint();
			renderer.paint( renderState );

			if ( Prefs.showScaleBarInMovie() )
			{
				final Graphics2D g2 = target.bi.createGraphics();
				g2.setClip( 0, 0, width, height );
				scalebar.setViewerState( renderState );
				scalebar.paint( g2 );
			}

			try
			{
				ImageIO.write( target.bi, "jpg",
						FileUtils.changeExtension( outputFile, "jpg" ) );
			} catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	public static ImagePlus getSimpleScreenShot( ViewerPanel viewer )
	{
		return getSimpleScreenShot( viewer, viewer.getWidth(), viewer.getHeight() );
	}

	public static ImagePlus getSimpleScreenShot( ViewerPanel viewer, int width, int height )
	{
		final ViewerState renderState = viewer.getState();

		final AffineTransform3D affine = new AffineTransform3D();
		renderState.getViewerTransform( affine );
		affine.set( affine.get( 0, 3 ) - width / 2, 0, 3 );
		affine.set( affine.get( 1, 3 ) - height / 2, 1, 3 );
		affine.scale( ( double ) width / width );
		affine.set( affine.get( 0, 3 ) + width / 2, 0, 3 );
		affine.set( affine.get( 1, 3 ) + height / 2, 1, 3 );
		renderState.setViewerTransform( affine );

		final ScaleBarOverlayRenderer scalebar = Prefs.showScaleBarInMovie() ? new ScaleBarOverlayRenderer() : null;

		class MyTarget implements RenderTarget
		{
			BufferedImage bi;

			@Override
			public BufferedImage setBufferedImage( final BufferedImage bufferedImage )
			{
				bi = bufferedImage;
				return null;
			}

			@Override
			public int getWidth()
			{
				return width;
			}

			@Override
			public int getHeight()
			{
				return height;
			}
		}

		final MyTarget target = new MyTarget();
		final MultiResolutionRenderer renderer = new MultiResolutionRenderer(
				target, new PainterThread( null ), new double[] { 1 }, 0, false, 1, null, false,
				viewer.getOptionValues().getAccumulateProjectorFactory(), new CacheControl.Dummy() );

		renderState.setCurrentTimepoint( viewer.getState().getCurrentTimepoint() );
		renderer.requestRepaint();
		renderer.paint( renderState );

		if ( Prefs.showScaleBarInMovie() )
		{
			final Graphics2D g2 = target.bi.createGraphics();
			g2.setClip( 0, 0, width, height );
			scalebar.setViewerState( renderState );
			scalebar.paint( g2 );
		}

		return new ImagePlus( "ScreenShot", target.bi );
	}

	// TODO: make show raw data part of the dialog
	public static void captureViewDialog( BdvHandle bdv, boolean showRawData )
	{
		final String pixelUnit = "micrometer";
		final PixelSpacingDialog dialog = new PixelSpacingDialog( getViewerVoxelSpacing( bdv ), pixelUnit );
		if ( ! dialog.showDialog() ) return;
		final ViewCaptureResult viewCaptureResult = captureView(
				bdv,
				dialog.getPixelSpacing(),
				pixelUnit,
				false );
		viewCaptureResult.rgbImage.show();
		if ( showRawData )
			viewCaptureResult.rawImagesStack.show();
	}
}
