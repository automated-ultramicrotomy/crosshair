package de.embl.cba.lazyalgorithm.converter;

import net.imglib2.Volatile;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;

public class VolatileNeighborhoodAverageConverter< R extends RealType< R > >
		implements Converter< Neighborhood< Volatile< R > >, Volatile< R > >
{
	@Override
	public void convert(
			Neighborhood<  Volatile< R > > neighborhood,
			Volatile< R > output )
	{
		for ( Volatile< R > value : neighborhood )
			if ( ! value.isValid() )
			{
				output.setValid( false );
				return;
			}

		double sum = 0;

		for ( Volatile< R > value : neighborhood )
			sum += value.get().getRealDouble();

		output.get().setReal( sum / neighborhood.size() );
		output.setValid( true );
	}
}
