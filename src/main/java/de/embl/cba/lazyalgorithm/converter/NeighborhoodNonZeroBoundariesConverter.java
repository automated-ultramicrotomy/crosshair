package de.embl.cba.lazyalgorithm.converter;

import net.imglib2.Cursor;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayList;
import java.util.Arrays;

public class NeighborhoodNonZeroBoundariesConverter< R extends RealType< R > >
		implements Converter< Neighborhood< R >, R >
{
	@Override
	public void convert( Neighborhood< R > neighborhood, R output )
	{
		long[] centrePosition = new long[ neighborhood.numDimensions() ];
		neighborhood.localize( centrePosition );

		final Cursor< R > cursor = neighborhood.localizingCursor();

		long[] position = new long[ neighborhood.numDimensions() ];
		final ArrayList< Double > values = new ArrayList<>();

		double centreValue = 0;

		while ( cursor.hasNext() )
		{
			final double value = cursor.next().getRealDouble();
			cursor.localize( position );
			if ( Arrays.equals( centrePosition, position ) )
				centreValue = value;
			values.add( value );
		}

		if ( centreValue == 0 )
		{
			output.setZero();
			return;
		}

		for ( double value : values )
		{
			if ( value != centreValue )
			{
				output.setReal( centreValue );
				return;
			}
		}

		output.setZero();
		return;
	}
}
