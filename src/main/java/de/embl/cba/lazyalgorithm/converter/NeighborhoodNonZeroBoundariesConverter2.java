package de.embl.cba.lazyalgorithm.converter;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;

/**
 * This version of NeighborhoodNonZeroBoundariesConverter
 * might be faster, but one does need to know the RAI
 * on which to compute on upon time of construction.
 *
 * @param <R>
 */
public class NeighborhoodNonZeroBoundariesConverter2< R extends RealType< R > >
		implements Converter< Neighborhood< R >, R >
{
	private final RandomAccessibleInterval< R > rai;

	public NeighborhoodNonZeroBoundariesConverter2( RandomAccessibleInterval< R > rai )
	{
		this.rai = rai;
	}

	@Override
	public void convert( Neighborhood< R > neighborhood, R output )
	{
		final double centerValue = getCenterValue( neighborhood );

		if ( centerValue == 0 )
		{
			output.setZero();
			return;
		}

		for ( R value : neighborhood )
		{
			if ( value.getRealDouble() != centerValue )
			{
				output.setReal( centerValue );
				return;
			}
		}

		output.setZero();
		return;
	}

	private double getCenterValue( Neighborhood< R > neighborhood )
	{
		long[] centrePosition = new long[ neighborhood.numDimensions() ];
		neighborhood.localize( centrePosition );

		final RandomAccess< R > randomAccess = rai.randomAccess();
		randomAccess.setPosition( centrePosition );
		return randomAccess.get().getRealDouble();
	}
}
