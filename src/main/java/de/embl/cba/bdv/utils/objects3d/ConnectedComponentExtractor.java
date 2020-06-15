package de.embl.cba.bdv.utils.objects3d;

import bdv.viewer.Source;
import de.embl.cba.bdv.utils.BdvUtils;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.neighborhood.DiamondShape;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayList;

import static de.embl.cba.bdv.utils.BdvUtils.getRAI;

public class ConnectedComponentExtractor< R extends RealType< R > & NativeType< R > >
{
	private final Source source;
	final RealPoint xyz;
	final int t;

	private RandomAccessibleInterval< BitType > connectedComponentMask;
	private ArrayList< double[] > calibrations;
	private long executionTimesMillis;

	private int numMipmapLevels;
	private double seedValue;

	public ConnectedComponentExtractor( Source source, RealPoint xyz, int t )
	{
		this.source = source;
		this.xyz = xyz;
		this.t = t;

		this.calibrations = new ArrayList<>(  );

		if ( this.source == null )
		{
			System.out.println("ERROR: no argbconversion sources found in bdv");
			return;
		}

		numMipmapLevels = this.source.getNumMipmapLevels();

		for ( int level = 0; level < numMipmapLevels; ++level )
		{
			calibrations.add( BdvUtils.getCalibration( this.source, level ) );
		}
	}

	public RandomAccessibleInterval< BitType > getConnectedComponentMask( int level )
	{
		final long currentTimeMillis = System.currentTimeMillis();

		final long[] positionInSourceStack = BdvUtils.getPositionInSource( source, xyz, t, level );

		final RandomAccessibleInterval< R > rai = getRAI( source, t, level );

		final FloodFill floodFill = new FloodFill(
						rai,
						new DiamondShape( 1 ),
						1000 * 1000 * 1000L );

		floodFill.run( positionInSourceStack );

		seedValue = floodFill.getSeedValue();

		connectedComponentMask = floodFill.getCroppedRegionMask();

		executionTimesMillis = System.currentTimeMillis() - currentTimeMillis;

		return connectedComponentMask;
	}

	public ArrayList< double[] > getSourceCalibrations( )
	{
		return calibrations;
	}

	public long getExecutionTimeMillis( )
	{
		return executionTimesMillis;
	}

	public double getSeedValue()
	{
		return seedValue;
	}

}
