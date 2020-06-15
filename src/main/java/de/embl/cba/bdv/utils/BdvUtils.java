package de.embl.cba.bdv.utils;

import bdv.BigDataViewer;
import bdv.ViewerSetupImgLoader;
import bdv.VolatileSpimSource;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.TransformedSource;
import bdv.viewer.state.ViewerState;
import de.embl.cba.bdv.utils.measure.PixelValueStatistics;
import de.embl.cba.bdv.utils.sources.*;
import de.embl.cba.bdv.utils.transforms.ConcatenatedTransformAnimator;

import bdv.util.*;
import bdv.viewer.*;
import bdv.viewer.animate.AbstractTransformAnimator;
import bdv.viewer.animate.SimilarityTransformAnimator;
import bdv.viewer.state.SourceState;

import de.embl.cba.transforms.utils.Transforms;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.*;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.converter.Converter;
import net.imglib2.histogram.DiscreteFrequencyDistribution;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.*;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.DoubleStream;

import static de.embl.cba.transforms.utils.Transforms.createBoundingIntervalAfterTransformation;


public abstract class BdvUtils
{

	public static final String OVERLAY = "overlay";

	public static < R extends RealType< R > > Source< R > openSource( String path, int sourceIndex )
	{
		final SpimData spimData = openSpimData( path );
		final ArrayList< ConverterSetup > converterSetups = new ArrayList<>();
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList<>();
		BigDataViewer.initSetups( spimData, converterSetups, sources );
		return ( Source ) sources.get( sourceIndex ).getSpimSource();
	}

	public static Interval getSourceGlobalBoundingInterval( Bdv bdv, int sourceId )
	{
		final AffineTransform3D sourceTransform =
				getSourceTransform( bdv, sourceId );
		final RandomAccessibleInterval< ? > rai =
				getRandomAccessibleInterval( bdv, sourceId );
		final Interval interval =
				Intervals.smallestContainingInterval( sourceTransform.estimateBounds( rai ) );
		return interval;
	}

	public static void zoomToSource( Bdv bdv, String sourceName )
	{
		zoomToSource( bdv, getSourceIndex( bdv, sourceName ) );
	}

	public static void zoomToSource( Bdv bdv, int sourceId )
	{
		setActive( bdv, sourceId, true );

		final FinalInterval interval = getInterval( bdv, sourceId );
		zoomToInterval( bdv, interval, 1.0 );
	}

	public static void setActive( Bdv bdv, int sourceId, boolean active )
	{
		bdv.getBdvHandle().getViewerPanel().getVisibilityAndGrouping().setSourceActive( sourceId, active );
	}

	public static FinalInterval getInterval( Bdv bdv, int sourceId )
	{
		final AffineTransform3D sourceTransform = getSourceTransform( bdv, sourceId );
		final RandomAccessibleInterval< ? > rai = getRandomAccessibleInterval( bdv, sourceId );
		return createBoundingIntervalAfterTransformation( rai, sourceTransform );
	}

	public static FinalRealInterval getViewerGlobalBoundingInterval( Bdv bdv )
	{
		AffineTransform3D viewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( viewerTransform );
		viewerTransform = viewerTransform.inverse();
		final long[] min = new long[ 3 ];
		final long[] max = new long[ 3 ];
		max[ 0 ] = bdv.getBdvHandle().getViewerPanel().getWidth();
		max[ 1 ] = bdv.getBdvHandle().getViewerPanel().getHeight();
		final FinalRealInterval realInterval
				= viewerTransform.estimateBounds( new FinalInterval( min, max ) );
		return realInterval;
	}

	public static int getSourceIndex( Bdv bdv, Source< ? > source )
	{
		final List< SourceState< ? > > sources =
				bdv.getBdvHandle().getViewerPanel().getState().getSources();

		for ( int i = 0; i < sources.size(); ++i )
			if ( sources.get( i ).getSpimSource().equals( source ) )
				return i;

		return -1;
	}

	public static Source< ? > getSource( Bdv bdv, int sourceIndex )
	{
		final List< SourceState< ? > > sources =
				bdv.getBdvHandle().getViewerPanel().getState().getSources();

		return sources.get( sourceIndex ).getSpimSource();
	}

	public static Converter< ?, ARGBType > getConverter( Bdv bdv, int sourceIndex )
	{
		final List< SourceState< ? > > sources =
				bdv.getBdvHandle().getViewerPanel().getState().getSources();

		return sources.get( sourceIndex ).getConverter();
	}


	public static Source< ? > getVolatileSource( Bdv bdv, int sourceIndex )
	{
		final List< SourceState< ? > > sources =
				bdv.getBdvHandle().getViewerPanel().getState().getSources();

		return sources.get( sourceIndex ).asVolatile().getSpimSource();
	}

	public static String getSourceName( Bdv bdv, int sourceId )
	{
		return bdv.getBdvHandle().getViewerPanel()
				.getState().getSources().get( sourceId ).getSpimSource().getName();
	}

	public static ArrayList< String > getSourceNames( Bdv bdv )
	{
		final ArrayList< String > sourceNames = new ArrayList<>();

		final List< SourceState< ? > > sources = bdv.getBdvHandle().getViewerPanel().getState().getSources();

		for ( SourceState source : sources )
			sourceNames.add( source.getSpimSource().getName() );

		return sourceNames;
	}


	public static int getSourceIndex( Bdv bdv, String sourceName )
	{
		return getSourceNames( bdv ).indexOf( sourceName );
	}

	public static VoxelDimensions getVoxelDimensions( Bdv bdv, int sourceId )
	{
		return bdv.getBdvHandle().getViewerPanel().getState().getSources().get( sourceId ).getSpimSource().getVoxelDimensions();
	}

	/**
	 * TODO: does that make sense?
	 *
	 * @param bdv
	 * @return
	 */
	public static double[] getViewerVoxelSpacingOLD( BdvHandle bdv )
	{

		// TODO: understand this logic!
		final AffineTransform3D viewerTransform = new AffineTransform3D();
		bdv.getViewerPanel().getState().getViewerTransform( viewerTransform );

		final double[] zeroCanvas = { 0.0, 0.0, 0.0 };
		final double[] zeroGlobal = new double[ 3 ];

		final double[] oneCanvas = { 1.0, 1.0, 1.0 };
		final double[] oneGlobal = new double[ 3 ];

		viewerTransform.applyInverse( zeroGlobal, zeroCanvas );
		viewerTransform.applyInverse( oneGlobal, oneCanvas );

		final double[] viewerVoxelSpacing = new double[ 3 ];
		for ( int d = 0; d < 3; d++ )
			viewerVoxelSpacing[ d ] = Math.abs( zeroGlobal[ d ] - oneGlobal[ d ]);

		return viewerVoxelSpacing;
	}

	public static double getViewerVoxelSpacing( BdvHandle bdv )
	{
		final int windowWidth = getBdvWindowWidth( bdv );
		final int windowHeight = getBdvWindowHeight( bdv );

		// TODO: understand this logic!
		final AffineTransform3D viewerTransform = new AffineTransform3D();
		bdv.getViewerPanel().getState().getViewerTransform( viewerTransform );

		final double[] physicalA = new double[ 3 ];
		final double[] physicalB = new double[ 3 ];

		viewerTransform.applyInverse( physicalA, new double[]{ 0, 0, 0} );
		viewerTransform.applyInverse( physicalB, new double[]{ 0, windowWidth, 0} );

		double viewerPhysicalWidth = LinAlgHelpers.distance( physicalA, physicalB );

		viewerTransform.applyInverse( physicalA, new double[]{ 0, 0, 0} );
		viewerTransform.applyInverse( physicalB, new double[]{ windowHeight, 0, 0} );

		double viewerPhysicalHeight = LinAlgHelpers.distance( physicalA, physicalB );

		final double viewerPhysicalVoxelSpacingX = viewerPhysicalWidth / windowWidth;
		final double viewerPhysicalVoxelSpacingY = viewerPhysicalHeight / windowHeight;

//		IJ.log( "[DEBUG] windowWidth = " + windowWidth );
//		IJ.log( "[DEBUG] windowHeight = " + windowHeight );
//		IJ.log( "[DEBUG] viewerPhysicalWidth = " + viewerPhysicalWidth );
//		IJ.log( "[DEBUG] viewerPhysicalHeight = " + viewerPhysicalHeight );
//		IJ.log( "[DEBUG] viewerPhysicalVoxelSpacingX = " + viewerPhysicalVoxelSpacingX );
//		IJ.log( "[DEBUG] viewerPhysicalVoxelSpacingY = " + viewerPhysicalVoxelSpacingY );

		return viewerPhysicalVoxelSpacingX;
	}


	public static AffineTransform3D getSourceTransform( Bdv bdv, int sourceId )
	{
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().getState().getSources().get( sourceId ).getSpimSource().getSourceTransform( 0, 0, sourceTransform );
		return sourceTransform;
	}


	public static AffineTransform3D getSourceTransform( Source source, int t, int level )
	{
		AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( t, level, sourceTransform );
		return sourceTransform;
	}


	public static RandomAccessibleInterval< ? > getRandomAccessibleInterval( Bdv bdv, int sourceId )
	{
		return bdv.getBdvHandle().getViewerPanel().getState().getSources().get( sourceId ).getSpimSource().getSource( 0, 0 );
	}

	public static RealRandomAccessible< ? > getRealRandomAccessible( Bdv bdv, int sourceId )
	{
		return bdv.getBdvHandle().getViewerPanel().getState().getSources().get( sourceId ).getSpimSource().getInterpolatedSource( 0, 0, Interpolation.NLINEAR );
	}

	public static void zoomToInterval( Bdv bdv, FinalInterval interval, double zoomFactor )
	{
		final AffineTransform3D affineTransform3D = getImageZoomTransform( bdv, interval, zoomFactor );

		bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( affineTransform3D );
	}

	public static AffineTransform3D getImageZoomTransform( Bdv bdv, FinalInterval interval, double zoomFactor )
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();

		double[] shiftToImage = new double[ 3 ];

		for ( int d = 0; d < 3; ++d )
			shiftToImage[ d ] = -( interval.min( d ) + interval.dimension( d ) / 2.0 );

		affineTransform3D.translate( shiftToImage );

		int[] bdvWindowDimensions = new int[ 2 ];
		bdvWindowDimensions[ 0 ] = bdv.getBdvHandle().getViewerPanel().getWidth();
		bdvWindowDimensions[ 1 ] = bdv.getBdvHandle().getViewerPanel().getHeight();

		affineTransform3D.scale( zoomFactor * bdvWindowDimensions[ 0 ] / interval.dimension( 0 ) );

		double[] shiftToBdvWindowCenter = new double[ 3 ];

		for ( int d = 0; d < 2; ++d )
			shiftToBdvWindowCenter[ d ] += bdvWindowDimensions[ d ] / 2.0;

		affineTransform3D.translate( shiftToBdvWindowCenter );

		return affineTransform3D;
	}

	public static Source getSource( BdvStackSource bdvStackSource, int i )
	{
		final SourceAndConverter sourceAndConverter = ( SourceAndConverter ) bdvStackSource.getSources().get( 0 );

		return sourceAndConverter.getSpimSource();
	}

	public static ARGBType asArgbType( Color color )
	{
		return new ARGBType( ARGBType.rgba(
							color.getRed(),
							color.getGreen(),
							color.getBlue(),
							color.getAlpha() ) );
	}

	public static Color asColor( ARGBType argbType )
	{
		return new Color( argbType.get() );
	}

	public static long[] getPositionInSource(
			Source source,
			RealPoint positionInViewer,
			int t,
			int level )
	{
		int n = 3;

		final AffineTransform3D sourceTransform =
				BdvUtils.getSourceTransform( source, t, level );

		final RealPoint positionInSourceInPixelUnits = new RealPoint( n );

		sourceTransform.inverse().apply(
				positionInViewer, positionInSourceInPixelUnits );

		final long[] longPosition = new long[ n ];

		for ( int d = 0; d < n; ++d )
			longPosition[ d ] = (long) positionInSourceInPixelUnits.getFloatPosition( d );

		return longPosition;
	}



	public static double[] getCurrentViewNormalVector( Bdv bdv )
	{
		AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentViewerTransform );

		final double[] viewerC = new double[]{ 0, 0, 0 };
		final double[] viewerX = new double[]{ 1, 0, 0 };
		final double[] viewerY = new double[]{ 0, 1, 0 };

		final double[] dataC = new double[ 3 ];
		final double[] dataX = new double[ 3 ];
		final double[] dataY = new double[ 3 ];

		final double[] dataV1 = new double[ 3 ];
		final double[] dataV2 = new double[ 3 ];
		final double[] currentNormalVector = new double[ 3 ];

		currentViewerTransform.inverse().apply( viewerC, dataC );
		currentViewerTransform.inverse().apply( viewerX, dataX );
		currentViewerTransform.inverse().apply( viewerY, dataY );

		LinAlgHelpers.subtract( dataX, dataC, dataV1 );
		LinAlgHelpers.subtract( dataY, dataC, dataV2 );

		LinAlgHelpers.cross( dataV1, dataV2, currentNormalVector );

		LinAlgHelpers.normalize( currentNormalVector );

		return currentNormalVector;
	}


	public static void levelCurrentView( Bdv bdv, double[] targetNormalVector )
	{

		double[] currentNormalVector = BdvUtils.getCurrentViewNormalVector( bdv );

		AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentViewerTransform );

		LinAlgHelpers.normalize( targetNormalVector ); // just to be sure.

		// determine rotation axis
		double[] rotationAxis = new double[ 3 ];
		LinAlgHelpers.cross( currentNormalVector, targetNormalVector, rotationAxis );
		if ( LinAlgHelpers.length( rotationAxis ) > 0 ) LinAlgHelpers.normalize( rotationAxis );

		// The rotation axis is in the coordinate system of the original data set => transform to viewer coordinate system
		double[] qCurrentRotation = new double[ 4 ];
		Affine3DHelpers.extractRotation( currentViewerTransform, qCurrentRotation );
		final AffineTransform3D currentRotation = quaternionToAffineTransform3D( qCurrentRotation );

		double[] rotationAxisInViewerSystem = new double[ 3 ];
		currentRotation.apply( rotationAxis, rotationAxisInViewerSystem );

		// determine rotation angle
		double angle = - Math.acos( LinAlgHelpers.dot( currentNormalVector, targetNormalVector ) );

		// construct rotation of angle around axis
		double[] rotationQuaternion = new double[ 4 ];
		LinAlgHelpers.quaternionFromAngleAxis( rotationAxisInViewerSystem, angle, rotationQuaternion );
		final AffineTransform3D rotation = quaternionToAffineTransform3D( rotationQuaternion );

		// apply transformation (rotating around current viewer centre position)
		final AffineTransform3D translateCenterToOrigin = new AffineTransform3D();
		translateCenterToOrigin.translate( DoubleStream.of( getBdvWindowCenter( bdv )).map( x -> -x ).toArray() );

		final AffineTransform3D translateCenterBack = new AffineTransform3D();
		translateCenterBack.translate( getBdvWindowCenter( bdv ) );

		ArrayList< AffineTransform3D > viewerTransforms = new ArrayList<>(  );

		viewerTransforms.add( currentViewerTransform.copy()
				.preConcatenate( translateCenterToOrigin )
				.preConcatenate( rotation )
				.preConcatenate( translateCenterBack )	);

		changeBdvViewerTransform( bdv, viewerTransforms, 2000 );

	}

	public static double[] getBdvWindowCenter( Bdv bdv )
	{
		final double[] centre = new double[ 3 ];

		centre[ 0 ] = bdv.getBdvHandle().getViewerPanel().getDisplay().getWidth() / 2.0;
		centre[ 1 ] = bdv.getBdvHandle().getViewerPanel().getDisplay().getHeight() / 2.0;

		return centre;
	}

	public static int getBdvWindowWidth( Bdv bdv )
	{
		return bdv.getBdvHandle().getViewerPanel().getDisplay().getWidth();
	}


	public static int getBdvWindowHeight( Bdv bdv )
	{
		return bdv.getBdvHandle().getViewerPanel().getDisplay().getHeight();
	}

	public static AffineTransform3D quaternionToAffineTransform3D( double[] rotationQuaternion )
	{
		double[][] rotationMatrix = new double[ 3 ][ 3 ];
		LinAlgHelpers.quaternionToR( rotationQuaternion, rotationMatrix );
		return matrixAsAffineTransform3D( rotationMatrix );
	}

	public static AffineTransform3D matrixAsAffineTransform3D( double[][] rotationMatrix )
	{
		final AffineTransform3D rotation = new AffineTransform3D();
		for ( int row = 0; row < 3; ++row )
			for ( int col = 0; col < 3; ++ col)
				rotation.set( rotationMatrix[ row ][ col ], row, col);
		return rotation;
	}

	public static void changeBdvViewerTransform(
			Bdv bdv,
			AffineTransform3D newViewerTransform,
			long duration)
	{
		AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentViewerTransform );

		final SimilarityTransformAnimator similarityTransformAnimator =
				new SimilarityTransformAnimator(
						currentViewerTransform,
						newViewerTransform,
						0 ,
						0,
						duration );

		bdv.getBdvHandle().getViewerPanel().setTransformAnimator( similarityTransformAnimator );
		bdv.getBdvHandle().getViewerPanel().transformChanged( newViewerTransform );
	}

	public static void changeBdvViewerTransform(
			Bdv bdv,
			ArrayList< AffineTransform3D > transforms,
			long duration)
	{

		AffineTransform3D currentTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentTransform );

		ArrayList< SimilarityTransformAnimator > animators = new ArrayList<>(  );

		final SimilarityTransformAnimator firstAnimator =
				new SimilarityTransformAnimator(
						currentTransform.copy(),
						transforms.get( 0 ).copy(),
						0 ,
						0,
						duration );

		animators.add( firstAnimator );

		for ( int i = 1; i < transforms.size(); i++ )
		{
			final SimilarityTransformAnimator animator =
					new SimilarityTransformAnimator(
							transforms.get( i - 1 ).copy(),
							transforms.get( i ).copy(),
							0 ,
							0,
							duration );

			animators.add( animator );
		}


		AbstractTransformAnimator transformAnimator = new ConcatenatedTransformAnimator( duration, animators );

		bdv.getBdvHandle().getViewerPanel().setTransformAnimator( transformAnimator );
		//bdv.getBdvHandle().getViewerPanel().transformChanged( currentTransform.copy() );

	}

	public static < T extends RealType< T > & NativeType< T > > void showAsIJ1MultiColorImage( Bdv bdv, double resolution, ArrayList< RandomAccessibleInterval< T > > randomAccessibleIntervals )
	{
		final ImagePlus imp = ImageJFunctions.wrap( Views.stack( randomAccessibleIntervals ), "capture" );
		final ImagePlus dup = new Duplicator().run( imp ); // otherwise it is virtual and cannot be modified
		IJ.run( dup, "Subtract...", "value=32768 slice");
		VoxelDimensions voxelDimensions = getVoxelDimensions( bdv, 0 );
		IJ.run( dup, "Properties...", "channels="+randomAccessibleIntervals.size()+" slices=1 frames=1 unit="+voxelDimensions.unit()+" pixel_width="+resolution+" pixel_height="+resolution+" voxel_depth=1.0");
		final CompositeImage compositeImage = new CompositeImage( dup );
		for ( int channel = 1; channel <= compositeImage.getNChannels(); ++channel )
		{
			compositeImage.setC( channel );
			switch ( channel )
			{
				case 1: // tomogram
					compositeImage.setChannelLut( compositeImage.createLutFromColor( Color.GRAY ) );
					compositeImage.setDisplayRange( 0, 1000 ); // TODO: get from bdv
					break;
				case 2: compositeImage.setChannelLut( compositeImage.createLutFromColor( Color.GRAY ) ); break;
				case 3: compositeImage.setChannelLut( compositeImage.createLutFromColor( Color.RED ) ); break;
				case 4: compositeImage.setChannelLut( compositeImage.createLutFromColor( Color.GREEN ) ); break;
				default: compositeImage.setChannelLut( compositeImage.createLutFromColor( Color.BLUE ) ); break;
			}
		}
		compositeImage.show();
		compositeImage.setTitle( "capture" );
		IJ.run(compositeImage, "Make Composite", "");
	}

	public static ArrayList< Color > getColors( List< Integer > nonOverlaySources )
	{
		ArrayList< Color > defaultColors = new ArrayList<>(  );
		if ( nonOverlaySources.size() > 1 )
		{
			defaultColors.add( Color.BLUE );
			defaultColors.add( Color.GREEN );
			defaultColors.add( Color.RED );
			defaultColors.add( Color.MAGENTA );
			defaultColors.add( Color.GRAY );
			defaultColors.add( Color.GRAY );
			defaultColors.add( Color.GRAY );
			defaultColors.add( Color.GRAY );
			defaultColors.add( Color.GRAY );
		}
		else
		{
			defaultColors.add( Color.GRAY );
		}
		return defaultColors;
	}

	public static List< Integer > getNonOverlaySourceIndices( Bdv bdv, List< SourceState< ? > > sources )
	{
		final List< Integer > nonOverlaySources = new ArrayList<>(  );

		for ( int sourceIndex = 0; sourceIndex < sources.size(); ++sourceIndex )
		{
			String name = getSourceName( bdv, sourceIndex );
			if ( ! name.contains( OVERLAY ) )
			{
				nonOverlaySources.add( sourceIndex );
			}
		}

		return nonOverlaySources;
	}

//	public static boolean isARGBConvertedRealSource( BdvStackSource bdvStackSource )
//	{
//		final Source source = getSource( bdvStackSource, 0 );
//
//		return isARGBConvertedRealSource( source );
//
//	}
//
//	public static boolean isARGBConvertedRealSource( Source source )
//	{
//		if ( source instanceof TransformedSource )
//		{
//			final Source wrappedVolatileSource = ( ( TransformedSource ) source ).getWrappedRealSource();
//
//			if ( wrappedVolatileSource instanceof ARGBConvertedRealSource )
//			{
//				return true;
//			}
//		}
//
//		return false;
//	}
//
//	public static ARGBConvertedRealSource getLabelsSource( BdvStackSource bdvStackSource )
//	{
//		final Source source = getSource( bdvStackSource, 0 );
//
//		return getLabelsSource( source );
//	}
//
//	private static ARGBConvertedRealSource getLabelsSource( Source source )
//	{
//		if ( source instanceof TransformedSource )
//		{
//			final Source wrappedVolatileSource = ( ( TransformedSource ) source ).getWrappedRealSource();
//
//			if ( wrappedVolatileSource instanceof ARGBConvertedRealSource )
//			{
//				return  ( ( ARGBConvertedRealSource ) wrappedVolatileSource) ;
//			}
//		}
//
//		return null;
//	}
//
	public static RealPoint getGlobalMouseCoordinates( Bdv bdv )
	{
		final RealPoint posInBdvInMicrometer = new RealPoint( 3 );
		bdv.getBdvHandle().getViewerPanel()
				.getGlobalMouseCoordinates( posInBdvInMicrometer );
		return posInBdvInMicrometer;
	}

	public static < R extends RealType< R > >
	Map< Integer, Double >
	getPixelValuesOfActiveSources( Bdv bdv, RealPoint point, int t )
	{
		final HashMap< Integer, Double > sourceIndexToPixelValue = new HashMap<>();

		final List< Integer > visibleSourceIndices = getVisibleSourceIndices( bdv );

		for ( int sourceIndex : visibleSourceIndices )
		{
			final Double realDouble = getPixelValue( bdv, sourceIndex, point, t );

			if ( realDouble != null )
				sourceIndexToPixelValue.put( sourceIndex, realDouble );
		}

		return sourceIndexToPixelValue;
	}

	public static
	HashMap< Integer, PixelValueStatistics > getPixelValueStatisticsOfActiveSources(
			Bdv bdv,
			RealPoint point,
			double radius,
			int t )
	{
		final HashMap< Integer, PixelValueStatistics > sourceIndexToPixelValue = new HashMap<>();

		final List< Integer > visibleSourceIndices = getVisibleSourceIndices( bdv );

		for ( int sourceIndex : visibleSourceIndices )
		{
			final PixelValueStatistics statistics = getPixelValueStatistics( bdv, sourceIndex, point, radius, t );

			if ( statistics != null )
				sourceIndexToPixelValue.put( sourceIndex, statistics );
		}

		return sourceIndexToPixelValue;
	}

	public static List< Integer > getVisibleSourceIndices( Bdv bdv )
	{
		return bdv.getBdvHandle().getViewerPanel()
				.getState().getVisibleSourceIndices();
	}

	public static Double getPixelValue( Bdv bdv, int sourceIndex, RealPoint point, int t )
	{
		final SourceState< ? > sourceState =
				bdv.getBdvHandle().getViewerPanel()
						.getState().getSources().get( sourceIndex );

		final Source source = sourceState.getSpimSource();

		return getPixelValue( source, point, t );
	}

	public static PixelValueStatistics getPixelValueStatistics( Bdv bdv, int sourceIndex, RealPoint point, double radius, int t )
	{
		final SourceState< ? > sourceState =
				bdv.getBdvHandle().getViewerPanel()
						.getState().getSources().get( sourceIndex );

		final Source source = sourceState.getSpimSource();

		return getPixelValueStatistics( bdv, source, point, radius, t );
	}

	private static PixelValueStatistics getPixelValueStatistics( Bdv bdv, Source source, RealPoint point, double radius, int t )
	{
		final RandomAccessibleInterval< ? extends RealType< ? > > rai = getRealTypeNonVolatileRandomAccessibleInterval( source, t, 0 );

		if ( rai == null ) return null;

		final long[] positionInSource = BdvUtils.getPositionInSource( source, point, t, 0 );

		final int sourceIndex = getSourceIndex( bdv, source );

		final VoxelDimensions voxelDimensions = BdvUtils.getVoxelDimensions( bdv, sourceIndex );

		double pixelSize = 1;
		if ( voxelDimensions != null )
			pixelSize = voxelDimensions.dimension( 0 );

		// Create a sphere shaped ROI
		final double[] center = new double[ 3 ];
		point.localize( center );
		RealMaskRealInterval mask = GeomMasks.closedSphere(center, radius );
		IterableRegion< BoolType > iterableRegion = toIterableRegion(mask, rai);
		IterableInterval< ? extends RealType<?>> iterable = Regions.sample(iterableRegion, rai);

		final ArrayList< Double > values = new ArrayList<>();
		for( RealType<?> pixel : iterable )
		{
			values.add( pixel.getRealDouble() );
		}

		final DoubleStatistics summaryStatistics = values.stream().collect(
				DoubleStatistics::new,
				DoubleStatistics::accept,
				DoubleStatistics::combine );


		final PixelValueStatistics statistics = new PixelValueStatistics();
		statistics.numVoxels = summaryStatistics.getCount();
		statistics.mean = summaryStatistics.getAverage();
		statistics.sdev = summaryStatistics.getStandardDeviation();

		return statistics;
	}

	public static IterableRegion< BoolType > toIterableRegion( RealMaskRealInterval mask, Interval image )
	{
		// Convert ROI from R^n to Z^n.
		final RandomAccessible<BoolType > discreteROI = Views.raster( Masks.toRealRandomAccessible(mask) );
		// Apply finite bounds to the discrete ROI.
		final Interval maskInterval = Intervals.smallestContainingInterval( mask );
		final FinalInterval intersect = Intervals.intersect( maskInterval, image );
		final IntervalView<BoolType> boundedDiscreteROI = Views.interval(discreteROI, intersect);
		// Create an iterable version of the finite discrete ROI.
		final IterableRegion< BoolType > iterable = Regions.iterable( boundedDiscreteROI );

		return iterable;
	}

	public static ArrayList< Integer > getSourceIndicesAtSelectedPoint( Bdv bdv, RealPoint selectedPoint, boolean evalSourcesAtPointIn2D )
	{
		final ArrayList< Integer > sourceIndicesAtSelectedPoint = new ArrayList<>();

		final int numSources = bdv.getBdvHandle().getViewerPanel()
				.getState().getSources().size();

		for ( int sourceIndex = 0; sourceIndex < numSources; sourceIndex++ )
		{
			final SourceState< ? > sourceState =
					bdv.getBdvHandle().getViewerPanel()
							.getState().getSources().get( sourceIndex );

			final Source< ? > source = sourceState.getSpimSource();

			final long[] positionInSource = getPositionInSource( source, selectedPoint, 0, 0 );

			Interval interval = source.getSource( 0, 0 );
			final Point point = new Point( positionInSource );

			if ( evalSourcesAtPointIn2D )
			{
				final long[] min = new long[ 2 ];
				final long[] max = new long[ 2 ];
				final long[] positionInSource2D = new long[ 2 ];
				for ( int d = 0; d < 2; d++ )
				{
					min[ d ] = interval.min( d );
					max[ d ] = interval.max( d );
					positionInSource2D[ d ] = positionInSource[ d ];
				}

				final FinalInterval interval2D = new FinalInterval( min, max );
				final Point point2D = new Point( positionInSource2D );

				if ( Intervals.contains( interval2D, point2D ) )
					sourceIndicesAtSelectedPoint.add( sourceIndex );
			}
			else
			{
				if ( Intervals.contains( interval, point ) )
					sourceIndicesAtSelectedPoint.add( sourceIndex );
			}
		}

		return sourceIndicesAtSelectedPoint;
	}


	public static Double getPixelValue(
			Source source, RealPoint point, int t  )
	{
		final RandomAccess< ? extends RealType< ? > > sourceAccess =
				getRealTypeNonVolatileRandomAccess( source, t, 0 );

		if ( sourceAccess == null ) return null;

		final long[] positionInSource =
				BdvUtils.getPositionInSource( source, point, t, 0 );

		sourceAccess.setPosition( positionInSource );

		try
		{
			return sourceAccess.get().getRealDouble();
		}
		catch ( Exception e )
		{
			return null;
		}

	}

	public static RandomAccess< ? extends RealType< ? >  >
	getRealTypeNonVolatileRandomAccess( Source source, int t, int level )
	{
		return getRealTypeNonVolatileRandomAccessibleInterval( source, t, level ).randomAccess();
	}

	/**
	 * Recursively go through the wrapped sources until the actual source is found.
	 *
	 * @param source
	 * @param t
	 * @param level
	 * @return
	 */
	public static RandomAccessibleInterval< ? extends RealType< ? > >
	getRealTypeNonVolatileRandomAccessibleInterval( Source source, int t, int level )
	{
		if ( source instanceof TransformedSource )
			return getRealTypeNonVolatileRandomAccessibleInterval(
					( ( TransformedSource ) source ).getWrappedSource(), t, level );

		if ( source instanceof ARGBConvertedRealSource )
			return getRealTypeNonVolatileRandomAccessibleInterval(
					( ( ARGBConvertedRealSource ) source ).getWrappedSource(), t, level );

		if ( source instanceof ModifiableRandomAccessibleIntervalSource4D )
			return ( ( ModifiableRandomAccessibleIntervalSource4D ) source ).getRawSource( t, level );

		if ( source instanceof ImagePlusFileSource )
			return getRealTypeNonVolatileRandomAccessibleInterval(
					( ( ImagePlusFileSource ) source ).getWrappedSource(), t, level );

		if ( source instanceof LazySpimSource )
		{
			return ( ( LazySpimSource ) source ).getNonVolatileSource( t, level );
		}
		else if ( source instanceof VolatileSpimSource )
		{
			throw new UnsupportedOperationException( source.getName() + " is of type VolatileSpimSource; cannot get nonVolatile access" );
		}
		else
		{
			return source.getSource( t, 0 );
		}
	}

	/**
	 * Returns the highest level where the source voxel spacings are <= the requested ones.
	 *
	 *
	 * @param source
	 * @param voxelSpacings
	 * @return
	 */
	public static int getLevel( Source< ? > source, double... voxelSpacings )
	{
		final int numMipmapLevels = source.getNumMipmapLevels();
		final int numDimensions = voxelSpacings.length;

		for ( int level = numMipmapLevels - 1; level >= 0 ; level-- )
		{
			final double[] calibration = BdvUtils.getCalibration( source, level );

			boolean allSpacingsSmallerThanRequested = true;

			for ( int d = 0; d < numDimensions; d++ )
			{
				if ( calibration[ d ] > voxelSpacings[ d ] )
					allSpacingsSmallerThanRequested = false;
			}

			if ( allSpacingsSmallerThanRequested )
				return level;
		}
		return 0;
	}



	public static RealRandomAccess
	getInterpolatedRealTypeNonVolatileRealRandomAccess( Source source, int t, int level, Interpolation interpolation )
	{
		if ( source instanceof TransformedSource )
			return getInterpolatedRealTypeNonVolatileRealRandomAccess(
					( ( TransformedSource ) source ).getWrappedSource(), t, level, interpolation );

		if ( source instanceof ARGBConvertedRealSource )
			return getInterpolatedRealTypeNonVolatileRealRandomAccess(
					( ( ARGBConvertedRealSource ) source ).getWrappedSource(), t, level, interpolation );

		if ( source instanceof ModifiableRandomAccessibleIntervalSource4D )
			return null; // TODO

		if ( source instanceof LazySpimSource )
		{
			return ( ( LazySpimSource ) source ).getInterpolatedNonVolatileSource( t, level, interpolation ).realRandomAccess();
		}
		else if ( source instanceof VolatileSpimSource )
		{
			throw new UnsupportedOperationException( source.getName() + " is of type VolatileSpimSource; cannot get nonVolatile access" );
		}
		else
		{
			return source.getInterpolatedSource( t, level, Interpolation.NLINEAR ).realRandomAccess();
		}
	}

	public static double[] getCalibration( Source source, int level )
	{
		final AffineTransform3D sourceTransform = new AffineTransform3D();

		source.getSourceTransform( 0, level, sourceTransform );

		final double[] calibration = Transforms.getScale( sourceTransform );

		return calibration;
	}

	public static void zoomToInterval( Bdv bdv, FinalRealInterval interval )
	{
		final AffineTransform3D affineTransform3D = getImageZoomTransform( bdv, interval );

		bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( affineTransform3D );
	}

	public static AffineTransform3D getImageZoomTransform( Bdv bdv, FinalRealInterval interval  )
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();

		double[] centerPosition = new double[ 3 ];

		for( int d = 0; d < 3; ++d )
		{
			final double center = ( interval.realMin( d ) + interval.realMax( d ) ) / 2.0;
			centerPosition[ d ] = - center;
		}

		affineTransform3D.translate( centerPosition );

		int[] bdvWindowDimensions = new int[ 2 ];
		bdvWindowDimensions[ 0 ] = bdv.getBdvHandle().getViewerPanel().getWidth();
		bdvWindowDimensions[ 1 ] = bdv.getBdvHandle().getViewerPanel().getHeight();

		final double intervalSize = interval.realMax( 0 ) - interval.realMin( 0 );
		affineTransform3D.scale(  1.0 * bdvWindowDimensions[ 0 ] / intervalSize );

		double[] shiftToBdvWindowCenter = new double[ 3 ];

		for( int d = 0; d < 2; ++d )
		{
			shiftToBdvWindowCenter[ d ] += bdvWindowDimensions[ d ] / 2.0;
		}

		affineTransform3D.translate( shiftToBdvWindowCenter );

		return affineTransform3D;
	}

	public static void moveToPosition( Bdv bdv, double[] xyz, int t, long durationMillis )
	{
		bdv.getBdvHandle().getViewerPanel().setTimepoint( t );

		final AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentViewerTransform );

		AffineTransform3D newViewerTransform = currentViewerTransform.copy();

		// ViewerTransform
		// applyInverse: coordinates in viewer => coordinates in image
		// apply: coordinates in image => coordinates in viewer

		final double[] locationOfTargetCoordinatesInCurrentViewer = new double[ 3 ];
		currentViewerTransform.apply( xyz, locationOfTargetCoordinatesInCurrentViewer );

		for ( int d = 0; d < 3; d++ )
		{
			locationOfTargetCoordinatesInCurrentViewer[ d ] *= -1;
		}

		newViewerTransform.translate( locationOfTargetCoordinatesInCurrentViewer );

		newViewerTransform.translate( getBdvWindowCenter( bdv ) );

		if ( durationMillis <= 0 )
		{
			bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( newViewerTransform );
			return;
		}
		else
		{
			final SimilarityTransformAnimator similarityTransformAnimator =
					new SimilarityTransformAnimator(
							currentViewerTransform,
							newViewerTransform,
							0,
							0,
							durationMillis );

			bdv.getBdvHandle().getViewerPanel().setTransformAnimator( similarityTransformAnimator );
			bdv.getBdvHandle().getViewerPanel().transformChanged( currentViewerTransform );
		}
	}

	public static void zoomToPosition( Bdv bdv, double[] xyzt, Double scale, long durationMillis )
	{
		final AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentViewerTransform );

		if ( scale == null )
		{
			final double[] currentScales = Transforms.getScale( currentViewerTransform );
			scale = currentScales[ 0 ];
		}

		final AffineTransform3D newViewerTransform = getViewerTransform( bdv, xyzt, scale );

		final SimilarityTransformAnimator similarityTransformAnimator =
				new SimilarityTransformAnimator(
						currentViewerTransform,
						newViewerTransform,
						0,
						0,
						durationMillis );

		bdv.getBdvHandle().getViewerPanel().setTransformAnimator( similarityTransformAnimator );
		bdv.getBdvHandle().getViewerPanel().transformChanged( currentViewerTransform );
	}

	public static AffineTransform3D getTranslatedViewerTransform( Bdv bdv, double[] position, AffineTransform3D currentViewerTransform )
	{
		final AffineTransform3D viewerTransform = currentViewerTransform.copy();

		double[] translation = new double[ 3 ];
		for( int d = 0; d < 3; ++d )
		{
			translation[ d ] = - position[ d ];
		}

		viewerTransform.setTranslation( new double[]{0,0,0} );
		viewerTransform.translate( translation );

		double[] centerBdvWindowTranslation = getBdvWindowCentre( bdv );
		viewerTransform.translate( centerBdvWindowTranslation );

		return viewerTransform;
	}

	public static double[] getBdvWindowCentre( Bdv bdv )
	{
		int[] bdvWindowDimensions = new int[ 3 ];
		bdvWindowDimensions[ 0 ] = bdv.getBdvHandle().getViewerPanel().getWidth();
		bdvWindowDimensions[ 1 ] = bdv.getBdvHandle().getViewerPanel().getHeight();

		double[] centerBdvWindowTranslation = new double[ 3 ];
		for( int d = 0; d < 3; ++d )
		{
			centerBdvWindowTranslation[ d ] = + bdvWindowDimensions[ d ] / 2.0;
		}
		return centerBdvWindowTranslation;
	}

	public static AffineTransform3D getViewerTransform( Bdv bdv, double[] position, double scale )
	{
		final AffineTransform3D viewerTransform = new AffineTransform3D();

		int[] bdvWindowDimensions = new int[ 3 ];
		bdvWindowDimensions[ 0 ] = bdv.getBdvHandle().getViewerPanel().getWidth();
		bdvWindowDimensions[ 1 ] = bdv.getBdvHandle().getViewerPanel().getHeight();

		double[] translation = new double[ 3 ];
		for( int d = 0; d < 3; ++d )
			translation[ d ] = - position[ d ];

		viewerTransform.setTranslation( translation );
		viewerTransform.scale( scale );

		double[] centerBdvWindowTranslation = new double[ 3 ];
		for( int d = 0; d < 3; ++d )
			centerBdvWindowTranslation[ d ] = + bdvWindowDimensions[ d ] / 2.0;

		viewerTransform.translate( centerBdvWindowTranslation );

		return viewerTransform;
	}

	public static ARGBType getSourceColor( Bdv bdv, int sourceId )
	{
		return bdv.getBdvHandle().getSetupAssignments().getConverterSetups().get( sourceId ).getColor();
	}

	public static double[] getDisplayRange( Bdv bdv, int sourceId )
	{
		final double displayRangeMin = bdv.getBdvHandle().getSetupAssignments()
				.getConverterSetups().get( sourceId ).getDisplayRangeMin();
		final double displayRangeMax = bdv.getBdvHandle().getSetupAssignments()
				.getConverterSetups().get( sourceId ).getDisplayRangeMax();

		return new double[]{ displayRangeMin, displayRangeMax };
	}
	public static void repaint( Bdv bdv )
	{
		bdv.getBdvHandle().getViewerPanel().requestRepaint();
	}


	public static boolean isActive( Bdv bdv, Source source )
	{
		final List< SourceState< ? > > sources
				= bdv.getBdvHandle().getViewerPanel().getState().getSources();

		final List< Integer > visibleSourceIndices = getVisibleSourceIndices( bdv );

		for( Integer i : visibleSourceIndices  )
		{
			final Source< ? > visibleSource = sources.get( i ).getSpimSource();

			if ( visibleSource.equals( source ) ) return true;

			if( visibleSource instanceof TransformedSource )
				if ( ( ( TransformedSource ) visibleSource ).getWrappedSource().equals( source ) )
					return true;
		}

		return false;
	}

	public static boolean isActive( Bdv bdv, int sourceIndex )
	{
		final List< Integer > visibleSourceIndices = getVisibleSourceIndices( bdv );

		if ( visibleSourceIndices.contains( sourceIndex ) ) return true;

		return false;
	}


	public static < R extends RealType< R > & NativeType< R > >
	RandomAccessibleInterval< R > getRAI( Source source, int t, int level )
	{
		if ( source instanceof TransformedSource )
		{
			final Source wrappedSource = ( ( TransformedSource ) source ).getWrappedSource();

			if ( wrappedSource instanceof ARGBConvertedRealSource )
				return ( ( ARGBConvertedRealSource ) wrappedSource ).getWrappedSource( t, level );
		}
		else if ( source instanceof SelectableARGBConvertedRealSource )
			return ( ( SelectableARGBConvertedRealSource ) source ).getWrappedSource( t, level );
		else if ( source.getType() instanceof RealType )
			return source.getSource( t, level );
		else
			return null;

		return null;

	}

	public static ArrayList< double[] > getVoxelSpacings( SpimData spimData, int setupId )
	{
		final VoxelDimensions voxelDimensions =
				spimData.getSequenceDescription().getViewSetupsOrdered().get( setupId ).getVoxelSize();
		final ViewerSetupImgLoader loader =
				( ViewerSetupImgLoader ) spimData.getSequenceDescription().getImgLoader().getSetupImgLoader( setupId );
		final double[][] resolutions = loader.getMipmapResolutions();

		final ArrayList< double[] > voxelSpacings = new ArrayList<>();

		for ( int level = 0; level < resolutions.length; level++ )
		{
			final double[] voxelSpacing = new double[ 3 ];
			for ( int d = 0; d < 3; d++ )
				voxelSpacing[ d ] = voxelDimensions.dimension( d ) * resolutions[ level ][ d ];
			voxelSpacings.add( voxelSpacing );
		}

		return voxelSpacings;
	}

	public static < R extends RealType< R > & NativeType< R > >
	void removeSource( BdvHandle bdv, BdvStackSource< R > bdvStackSource )
	{
		for ( SourceAndConverter< R > sourceAndConverter : bdvStackSource.getSources() )
		{
			final int sourceIndex = BdvUtils.getSourceIndex( bdv, sourceAndConverter.getSpimSource() );
			final ConverterSetup converterSetup = bdv.getSetupAssignments().getConverterSetups().get( sourceIndex );
			bdv.getSetupAssignments().removeSetup( converterSetup );

			bdv.getViewerPanel().removeSource( sourceAndConverter.getSpimSource() );
		}
	}

	public static void centerBdvWindowLocation( BdvHandle bdv )
	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		final ViewerFrame viewerFrame = getViewerFrame( bdv );

		viewerFrame.setLocation(
				screenSize.width / 2 - viewerFrame.getWidth() / 2,
				0 + 50 );
	}

	public static ViewerFrame getViewerFrame( BdvHandle bdv )
	{
		return ( ViewerFrame ) bdv.getViewerPanel().getParent()
				.getParent().getParent().getParent();
	}

	public static < R extends RealType< R > & NativeType< R > >
	RandomAccessibleIntervalSource4D< R > createSourceFrom2DFrameList(
			ArrayList< RandomAccessibleInterval< R > > frames2D,
			String name )
	{
		RandomAccessibleIntervalSource4D< R > source =
				new RandomAccessibleIntervalSource4D(
						Views.permute(
								Views.addDimension(
										Views.stack( frames2D ), 0, 0 ),
								2, 3 ),
						Util.getTypeFromInterval( Views.stack( frames2D ) ),
						name );

		return source;
	}

	public static boolean isSourceIntersectingCurrentView( BdvHandle bdv, int sourceIndex )
	{
		final Interval interval = getSourceGlobalBoundingInterval( bdv, sourceIndex );

		final Interval viewerInterval =
				Intervals.smallestContainingInterval(
						getViewerGlobalBoundingInterval( bdv ) );

		final boolean intersects = ! Intervals.isEmpty(
				Intervals.intersect( interval, viewerInterval ) );

		return intersects;
	}

	public static List< Integer > getSourceIndiciesVisibleInCurrentViewerWindow( BdvHandle bdvHandle, boolean checkIntersectionWithViewerWindowOnlyIn2D )
	{
		final List< Integer > visibleSourceIndices = bdvHandle.getViewerPanel().getState().getVisibleSourceIndices();

		final ArrayList< Integer > visibleInCurrentViewSourceIndices = new ArrayList<>();
		for ( int sourceIndex : visibleSourceIndices )
		{
			if ( checkIntersectionWithViewerWindowOnlyIn2D )
			{
				if ( BdvUtils.isSourceIntersectingCurrentViewIn2D( bdvHandle, sourceIndex ) )
				{
					visibleInCurrentViewSourceIndices.add( sourceIndex );
				}
				else if ( !BdvUtils.isSourceIntersectingCurrentView( bdvHandle, sourceIndex ) )
				{
					visibleInCurrentViewSourceIndices.add( sourceIndex );
				}
			}
		}
		return visibleInCurrentViewSourceIndices;
	}

	public static boolean isSourceIntersectingCurrentViewIn2D( BdvHandle bdv, int sourceIndex )
	{
		final Interval interval = getSourceGlobalBoundingInterval( bdv, sourceIndex );

		final Interval viewerInterval =
				Intervals.smallestContainingInterval(
						getViewerGlobalBoundingInterval( bdv ) );

		final boolean intersects = ! Intervals.isEmpty(
				intersect2D( interval, viewerInterval ) );

		return intersects;
	}

	public static void moveBdvViewToAxialZeroPosition( BdvHandle bdvHandle )
	{
		final AffineTransform3D viewerTransform = new AffineTransform3D();
		bdvHandle.getViewerPanel()
				.getState().getViewerTransform( viewerTransform );
		final double[] translation = viewerTransform.getTranslation();
		translation[ 2 ] = 0;
		viewerTransform.setTranslation( translation );
		bdvHandle.getViewerPanel().setCurrentViewerTransform( viewerTransform );
	}

	public static FinalInterval intersect2D( final Interval intervalA, final Interval intervalB )
	{
		assert intervalA.numDimensions() == intervalB.numDimensions();

		final long[] min = new long[ 2 ];
		final long[] max = new long[ 2 ];
		for ( int d = 0; d < 2; ++d )
		{
			min[ d ] = Math.max( intervalA.min( d ), intervalB.min( d ) );
			max[ d ] = Math.min( intervalA.max( d ), intervalB.max( d ) );
		}
		return new FinalInterval( min, max );
	}

	public static SpimData openSpimData( String path )
	{
		try
		{
			SpimData spimData = new XmlIoSpimData().load( path);
			return spimData;
		}
		catch ( SpimDataException e )
		{
			System.out.println( path );
			e.printStackTrace();
			return null;
		}
	}

	public static FinalRealInterval getRealIntervalOfCurrentSource( BdvHandle bdvHandle )
	{
		final int currentSourceIndex = bdvHandle.getViewerPanel().getState().getCurrentSource();
		final Source< ? > currentSource = getSource( bdvHandle, currentSourceIndex );
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		currentSource.getSourceTransform( 0, 0, affineTransform3D );
		return affineTransform3D.estimateBounds( currentSource.getSource( 0, 0 ) );
	}

	public static VoxelDimensions getVoxelDimensionsOfCurrentSource( BdvHandle bdvHandle )
	{
		final int currentSourceIndex = bdvHandle.getViewerPanel().getState().getCurrentSource();
		return BdvUtils.getVoxelDimensions( bdvHandle, currentSourceIndex );
	}

	public static FinalRealInterval getRealIntervalOfVisibleSources( BdvHandle bdvHandle )
	{
		final List< Integer > visibleSourceIndices = bdvHandle.getViewerPanel().getState().getVisibleSourceIndices();

		FinalRealInterval union = Intervals.createMinMaxReal( 0,0,0,0,0,0 );

		for ( int sourceIndex : visibleSourceIndices )
		{
			final Source< ? > currentSource = getSource( bdvHandle, sourceIndex );
			final AffineTransform3D affineTransform3D = new AffineTransform3D();
			currentSource.getSourceTransform( 0, 0, affineTransform3D );
			final FinalRealInterval bounds = affineTransform3D.estimateBounds( currentSource.getSource( 0, 0 ) );
			union = Intervals.union( union, bounds );
		}

		return union;
	}

	public static void initBrightness(
			final BdvHandle bdvHandle,
			final double cumulativeMinCutoff,
			final double cumulativeMaxCutoff,
			int sourceIndex )
	{
		final ViewerState state = bdvHandle.getViewerPanel().getState();
		final SetupAssignments setupAssignments = bdvHandle.getSetupAssignments();

		final Source< ? > source = state.getSources().get( sourceIndex ).getSpimSource();
		final int timepoint = state.getCurrentTimepoint();
		if ( !source.isPresent( timepoint ) )
			return;
		if ( !UnsignedShortType.class.isInstance( source.getType() ) )
			return;
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< UnsignedShortType > img = ( RandomAccessibleInterval< UnsignedShortType > ) source.getSource( timepoint, source.getNumMipmapLevels() - 1 );
		final long z = ( img.min( 2 ) + img.max( 2 ) + 1 ) / 2;

		final int numBins = 6535;
		final Histogram1d< UnsignedShortType > histogram = new Histogram1d<>( Views.iterable( Views.hyperSlice( img, 2, z ) ), new Real1dBinMapper< UnsignedShortType >( 0, 65535, numBins, false ) );
		final DiscreteFrequencyDistribution dfd = histogram.dfd();
		final long[] bin = new long[] { 0 };
		double cumulative = 0;
		int i = 0;
		for ( ; i < numBins && cumulative < cumulativeMinCutoff; ++i )
		{
			bin[ 0 ] = i;
			cumulative += dfd.relativeFrequency( bin );
		}
		final int min = i * 65535 / numBins;
		for ( ; i < numBins && cumulative < cumulativeMaxCutoff; ++i )
		{
			bin[ 0 ] = i;
			cumulative += dfd.relativeFrequency( bin );
		}
		final int max = i * 65535 / numBins;
		final MinMaxGroup minmax = setupAssignments.getMinMaxGroups().get( sourceIndex );
		minmax.getMinBoundedValue().setCurrentValue( min );
		minmax.getMaxBoundedValue().setCurrentValue( max );
	}

	public static String getGlobalMousePositionString( BdvHandle bdv )
	{
		final RealPoint globalMouseCoordinates = getGlobalMouseCoordinates( bdv );
		final String string = globalMouseCoordinates.toString();
		string.replace( "(", "" ).replace( ")", "" );
		return string;
	}

	public static String getBdvViewerTransformString( BdvHandle bdv )
	{
		final AffineTransform3D view = new AffineTransform3D();
		bdv.getViewerPanel().getState().getViewerTransform( view );

		return view.toString().replace( "3d-affine: (", "" ).replace( ")", "" );
	}
}
