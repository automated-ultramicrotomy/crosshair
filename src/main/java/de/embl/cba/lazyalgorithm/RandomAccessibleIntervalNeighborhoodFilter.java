package de.embl.cba.lazyalgorithm;

import de.embl.cba.lazyalgorithm.view.NeighborhoodViews;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;

public class RandomAccessibleIntervalNeighborhoodFilter< R extends RealType< R > >
		implements RandomAccessibleIntervalFilter< R >
{
	private final Converter< Neighborhood< R >, R > neighborhoodConverter;
	private final Shape neighborhoodShape;

	public RandomAccessibleIntervalNeighborhoodFilter(
			Converter< Neighborhood< R >, R > neighborhoodConverter,
			Shape neighborhoodShape )
	{
		this.neighborhoodConverter = neighborhoodConverter;
		this.neighborhoodShape = neighborhoodShape;
	}

	@Override
	public RandomAccessibleInterval< R > filter( RandomAccessibleInterval< R > input )
	{
		return NeighborhoodViews.neighborhoodConvertedView(
				input,
				neighborhoodConverter,
				neighborhoodShape );
	}
}
