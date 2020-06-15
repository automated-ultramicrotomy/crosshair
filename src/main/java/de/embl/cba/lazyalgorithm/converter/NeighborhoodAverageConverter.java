package de.embl.cba.lazyalgorithm.converter;

import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;

public class NeighborhoodAverageConverter < R extends RealType< R > >
		implements Converter< Neighborhood< R >, R >
{
	@Override
	public void convert( Neighborhood< R > neighborhood, R output )
	{
		double sum = 0;

		for ( R value : neighborhood )
			sum += value.getRealDouble();

		output.setReal( sum / neighborhood.size() );
	}
}
