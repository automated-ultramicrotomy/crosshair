package de.embl.cba.lazyalgorithm;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.NumericType;

public interface RandomAccessibleIntervalFilter < T extends NumericType< T > >
{
	RandomAccessibleInterval< T > filter( RandomAccessibleInterval< T > input );
}
