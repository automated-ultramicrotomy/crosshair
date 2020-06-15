package de.embl.cba.bdv.utils.export;

import bdv.export.ProgressWriter;
import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.state.SourceState;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.bdv.utils.RandomAccessibleIntervalUtils;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.FileSaver;
import net.imglib2.*;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.*;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static bdv.viewer.Interpolation.NLINEAR;

public class BdvRealSourceToVoxelImageExporter< T extends RealType< T > & NativeType< T > >
{
	private final List< SourceState< ? > > sacs;
	private final List< Integer > sourceIndices;
	private final RealInterval interval;
	private final int tMin;
	private final int tMax;
	private final Interpolation interpolation;
	private final double[] outputVoxelSpacings;
	private final ExportModality exportModality;
	private final ProgressWriter progress;
	private AffineTransform3D resolutionTransform;
	protected AffineTransform3D pixelRenderToPhysical;
	private String outputUnit;
	private FinalInterval outputVoxelInterval;
	private final ExportDataType exportDataType;
	private int numThreads;
	private String outputDirectory;

	public enum ExportModality
	{
		ShowImages,
		SaveAsTiffVolumes
	}

	public enum ExportDataType
	{
		UnsignedByte,
		UnsignedShort,
		Float
	}

	public static class Dialog
	{
		private static final String[] XYZ = new String[]{ "X", "Y", "Z" };

		public static double[] outputVoxelSpacings = new double[]{ 1.0, 1.0, 1.0 };
		public static BdvRealSourceToVoxelImageExporter.ExportModality exportModality = BdvRealSourceToVoxelImageExporter.ExportModality.ShowImages;
		public static BdvRealSourceToVoxelImageExporter.ExportDataType exportDataType = BdvRealSourceToVoxelImageExporter.ExportDataType.UnsignedShort;
		public static Interpolation interpolation = NLINEAR;

		public static int numProcessingThreads = Runtime.getRuntime().availableProcessors();

		public static boolean showDialog()
		{
			final GenericDialog gd = new GenericDialog( "Export to voxel images" );

			for ( int d = 0; d < 3; d++ )
				gd.addNumericField( "Output Voxel Spacing " + XYZ[ d ], outputVoxelSpacings[ d ], 3 );

			String[] exportModalities = getNames( BdvRealSourceToVoxelImageExporter.ExportModality.class );
			gd.addChoice( "Export Modality", exportModalities, exportModality.toString() );

			final String[] exportDataTypes = getNames( BdvRealSourceToVoxelImageExporter.ExportDataType.class );
			gd.addChoice( "Export Data Type", exportDataTypes, exportDataType.toString() );

			final String[] interpolations = getNames( Interpolation.class );
			gd.addChoice( "Interpolation", interpolations, interpolation.toString() );

			gd.addNumericField( "Number of processing threads", numProcessingThreads, 0 );

			gd.showDialog();
			if ( gd.wasCanceled() ) return false;

			for ( int d = 0; d < 3; d++ )
				outputVoxelSpacings[ d ] = gd.getNextNumber();

			exportModality = BdvRealSourceToVoxelImageExporter.ExportModality.valueOf( gd.getNextChoice() );
			exportDataType = BdvRealSourceToVoxelImageExporter.ExportDataType.valueOf( gd.getNextChoice() );
			interpolation = Interpolation.valueOf( gd.getNextChoice() );
			numProcessingThreads = ( int ) gd.getNextNumber();

			return true;
		}

		private static String[] getNames( Class< ? extends Enum< ? > > e )
		{
			return Arrays.stream( e.getEnumConstants() ).map( Enum::name ).toArray( String[]::new );
		}
	}

	public BdvRealSourceToVoxelImageExporter(
			BdvHandle bdv,
			List< Integer > sourceIndices,
			final RealInterval interval,
			final int tMin,
			final int tMax,
			final Interpolation interpolation,
			final double[] outputVoxelSpacings,
			final ExportModality exportModality,
			final ExportDataType exportDataType,
			int numThreads,
			final ProgressWriter progress )
	{
		this.sacs = bdv.getViewerPanel().getState().getSources();
		this.sourceIndices = sourceIndices;
		this.interval = interval;
		this.tMin = tMin;
		this.tMax = tMax;
		this.interpolation = interpolation;
		this.outputVoxelSpacings = outputVoxelSpacings;
		this.exportModality = exportModality;
		this.exportDataType = exportDataType;
		this.numThreads = numThreads;
//		this.baseType = baseType;
		this.progress = progress;
	}

	public void export()
	{
		setOutputPixelInterval();
		setRenderResolutionTransform();
		setOutputVoxelUnit();

		final int numVolumes = sourceIndices.size() * ( tMax - tMin + 1 );
		int iVolume = 0;

		long exportStartTimeMillis = System.currentTimeMillis();

		for ( int i : sourceIndices )
		{
			final Source< ? > source = sacs.get( i ).getSpimSource();
			ArrayList< RandomAccessibleInterval< T > > timepoints = new ArrayList<>();

			for ( int t = tMin; t <= tMax; ++t )
			{
				long volumeStartTimeMillis = System.currentTimeMillis();

				String name = source.getName();
				if ( tMax - tMin > 0 )
					name += "--T" + String.format( "%1$05d", t );
				Logger.log( "Exporting: " + name  );

				final int level = BdvUtils.getLevel( source, outputVoxelSpacings );
				Logger.log( "Sampling from resolution level: " + level  );

				RandomAccessibleInterval< T > rai = getCroppedAndRasteredRAI( source, t, level );

				switch ( exportModality )
				{
					case ShowImages:
						timepoints.add( rai );
						break;
					case SaveAsTiffVolumes:
						ImagePlus imagePlusVolume = asImagePlus( rai, name );
						Logger.log( "Loading and converting to output voxel space done in [ms]: " + ( System.currentTimeMillis() - volumeStartTimeMillis ));
						final String path = outputDirectory + File.separator + name + ".tif";
						Logger.log( "Save as Tiff to path: " + path ) ;
						final FileSaver fileSaver = new FileSaver( imagePlusVolume );
						fileSaver.saveAsTiff( path );
						// try to free memory
						rai = null;
						imagePlusVolume = null;
						System.gc();
						break;
					default:
						break;
				}

				progress.setProgress( 1.0 * ++iVolume  / numVolumes );
				Logger.log( "Export done in [ms]: " + ( System.currentTimeMillis() - volumeStartTimeMillis ));
				Logger.progress( "Export", exportStartTimeMillis, iVolume, numVolumes );
			} // time

			switch ( exportModality )
			{
				case ShowImages:
					if ( timepoints.size() > 0 )
					{
						final ImagePlus imagePlusMovie = asImagePlus( timepoints, source.getName() );
						imagePlusMovie.show();
					}
					break;
				default:
					break;
			}

			// try to free memory
			timepoints = null;
			System.gc();
		}

		Logger.log( "Export is done." );
	}

	public ImagePlus asImagePlus( ArrayList< RandomAccessibleInterval< T > > raiXYZTimePoints, String name )
	{
		final RandomAccessibleInterval< T > raiXYZT = Views.stack( raiXYZTimePoints );
		final RandomAccessibleInterval< T > raiXYZTC = Views.addDimension( raiXYZT, 0, 0 );
		final RandomAccessibleInterval< T > raiXYZCT = Views.permute( raiXYZTC, 3, 4 );
		final RandomAccessibleInterval< T > raiXYCZT = Views.permute( raiXYZCT, 2, 3 );

		ImagePlus imagePlus = asImagePlus( raiXYCZT, name, exportDataType );

		return imagePlus;
	}

	public void setOutputDirectory( String outputDirectory )
	{
		this.outputDirectory = outputDirectory;
	}

	private RandomAccessibleInterval< T > getCroppedAndRasteredRAI( Source< ? > source, int t, int level )
	{
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( t, level, sourceTransform );

		final AffineTransform3D inputVoxelToOuputVoxelTransform =
				sourceTransform.copy().preConcatenate( resolutionTransform.inverse() );

		Interval inputVoxelInterval =
				Intervals.smallestContainingInterval(
						inputVoxelToOuputVoxelTransform.inverse().estimateBounds( outputVoxelInterval ) );

		inputVoxelInterval = Intervals.intersect( inputVoxelInterval, source.getSource( t, level )  );

		RandomAccessibleInterval< T > rai = ( RandomAccessibleInterval ) Views.interval( source.getSource( t, level ), inputVoxelInterval );

		// force from disk into RAM (is this neccessary ?)
		rai = RandomAccessibleIntervalUtils.copyVolumeRAI( rai, 1 );

		final RandomAccessible< T > ra = Views.extendBorder( rai );

		final RealRandomAccessible realRA;
		switch ( interpolation )
		{
			case NLINEAR:
				realRA = Views.interpolate( ra, new ClampingNLinearInterpolatorFactory<>() );
				break;
			case NEARESTNEIGHBOR:
				realRA = Views.interpolate( ra, new NearestNeighborInterpolatorFactory<>() );
				break;
			default:
				realRA = Views.interpolate( ra, new ClampingNLinearInterpolatorFactory<>() );
				break;
		}

		RandomAccessible< T > outputVoxelRA = RealViews.transform( realRA, inputVoxelToOuputVoxelTransform );

		RandomAccessibleInterval< T > outputRAI = Views.interval( outputVoxelRA, outputVoxelInterval );
		// force into RAM
		outputRAI = RandomAccessibleIntervalUtils.copyVolumeRAI( outputRAI, numThreads );

		return outputRAI;
	}

	private ImagePlus asImagePlus(
			RandomAccessibleInterval< T > raiXYZ,
			String name )
	{
		final RandomAccessibleInterval< T > raiXYZC = Views.addDimension( raiXYZ, 0, 0 );
		final RandomAccessibleInterval< T > raiXYCZ = Views.permute( raiXYZC, 2, 3 );
		RandomAccessibleInterval< T > zeroMin = Views.zeroMin( raiXYCZ );

		ImagePlus imagePlus = asImagePlus( zeroMin, name, exportDataType );

		setCalibration( imagePlus );

		return imagePlus;
	}

	private void setCalibration( ImagePlus imagePlus )
	{
		imagePlus.getCalibration().setUnit( outputUnit );
		imagePlus.getCalibration().pixelWidth = outputVoxelSpacings[ 0 ];
		imagePlus.getCalibration().pixelHeight = outputVoxelSpacings[ 1 ];
		imagePlus.getCalibration().pixelDepth = outputVoxelSpacings[ 2 ];
	}

	private ImagePlus asImagePlus( RandomAccessibleInterval< T > zeroMin, String name, ExportDataType exportDataType )
	{
		ImagePlus imagePlus;
		switch ( exportDataType )
		{
			case UnsignedByte:
				imagePlus = ImageJFunctions.wrapUnsignedByte( zeroMin, name );
				break;
			case UnsignedShort:
				imagePlus = ImageJFunctions.wrapUnsignedShort( zeroMin, name );
				break;
			case Float:
				imagePlus = ImageJFunctions.wrapFloat( zeroMin, name );
				break;
			default:
				imagePlus = ImageJFunctions.wrapFloat( zeroMin, name );
				break;
		}
		return imagePlus;
	}

	private void setOutputVoxelUnit()
	{
		outputUnit = sacs.get( 0 ).getSpimSource().getVoxelDimensions().unit();
	}

	private void setOutputPixelInterval( )
	{
//		double[] inputres = new double[ 3 ];
//		VoxelDimensions voxdims = source.getVoxelDimensions();
//		voxdims.dimensions( inputres );
//
		final int numDimensions = 3;

		long[] min = new long[ numDimensions ];
		long[] max = new long[ numDimensions ];
		for( int d = 0; d < numDimensions; d++ )
		{
			min[ d ] = (long) Math.ceil( interval.realMin( d ) / outputVoxelSpacings[ d ] );
			max[ d ] = (long) Math.ceil( interval.realMax( d ) / outputVoxelSpacings[ d ] );
		}

		outputVoxelInterval = new FinalInterval( min, max );
	}

	private void setRenderResolutionTransform( )
	{
		resolutionTransform = new AffineTransform3D();
		for( int i = 0; i < outputVoxelSpacings.length; i++ )
			resolutionTransform.set( outputVoxelSpacings[ i ], i, i );
	}
}
