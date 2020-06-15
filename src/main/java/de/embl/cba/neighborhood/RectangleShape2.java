package de.embl.cba.neighborhood;

import net.imglib2.*;
import net.imglib2.algorithm.neighborhood.*;

import java.util.Iterator;

/**
 * A factory for Accessibles on rectangular neighboorhoods.
 *
 * @author Tobias Pietzsch
 * @author Jonathan Hale (University of Konstanz)
 * @author Christian Tischer
 */
public class RectangleShape2 implements Shape
{
	final Interval spanInterval;

	final boolean skipCenter;

	/**
	 * @param spanInterval
	 * @param skipCenter
	 */
	public RectangleShape2( final Interval spanInterval, final boolean skipCenter )
	{
		this.spanInterval = spanInterval;
		this.skipCenter = skipCenter;
	}

	/**
	 *
	 * @param span
	 * @param skipCenter
	 */
	public RectangleShape2( final long[] span, final boolean skipCenter )
	{
		this.spanInterval = createSpanInterval( span );
		this.skipCenter = skipCenter;
	}

	private FinalInterval createSpanInterval( long[] span )
	{
		int n = span.length;
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];

		for ( int d = 0; d < n; ++d )
		{
			min[ d ] = - span[ d ] / 2;
			max[ d ] = min[ d ] + span[ d ] - 1;
		}

		return new FinalInterval( min, max );
	}

	@Override
	public < T > RectangleShape2.NeighborhoodsIterableInterval< T >
	neighborhoods( final RandomAccessibleInterval< T > source )
	{
		final RectangleNeighborhoodFactory< T > f =
				skipCenter ?
						RectangleNeighborhoodSkipCenterUnsafe.factory() :
						RectangleNeighborhoodUnsafe.factory();

		return new RectangleShape2.NeighborhoodsIterableInterval< T >(
				source, spanInterval, f );
	}

	@Override
	public < T > RectangleShape2.NeighborhoodsAccessible< T >
	neighborhoodsRandomAccessible( final RandomAccessible< T > source )
	{
		final RectangleNeighborhoodFactory< T > f =
				skipCenter ?
						RectangleNeighborhoodSkipCenterUnsafe.factory() :
						RectangleNeighborhoodUnsafe.factory();

		return new RectangleShape2.NeighborhoodsAccessible( source, spanInterval, f );
	}

	@Override
	public < T > RectangleShape2.NeighborhoodsIterableInterval< T >
	neighborhoodsSafe( final RandomAccessibleInterval< T > source )
	{
		final RectangleNeighborhoodFactory< T > f =
				skipCenter ?
						RectangleNeighborhoodSkipCenter.< T >factory() :
						RectangleNeighborhood.< T >factory();
		return new RectangleShape2.NeighborhoodsIterableInterval< T >( source, spanInterval, f );
	}

	@Override
	public < T > RectangleShape2.NeighborhoodsAccessible< T >
	neighborhoodsRandomAccessibleSafe( final RandomAccessible< T > source )
	{
		final RectangleNeighborhoodFactory< T > f =
				skipCenter ?
				RectangleNeighborhoodSkipCenter.< T >factory() :
				RectangleNeighborhood.< T >factory();
		return new RectangleShape2.NeighborhoodsAccessible< T >( source, spanInterval, f );
	}

	/**
	 * @return {@code true} if {@code skipCenter} was set to true
	 *         during construction, {@code false} otherwise.
	 * @see CenteredRectangleShape#CenteredRectangleShape(int[], boolean)
	 */
	public boolean isSkippingCenter()
	{
		return skipCenter;
	}

	/**
	 * @return The span of this shape.
	 */
	public Interval getSpan()
	{
		return spanInterval;
	}

	@Override
	public String toString()
	{
		return "RectangleShape, span = " + spanInterval;
	}

	public static final class NeighborhoodsIterableInterval< T >
			extends AbstractInterval implements IterableInterval< Neighborhood< T > >
	{
		final RandomAccessibleInterval< T > source;

		final Interval span;

		final RectangleNeighborhoodFactory< T > factory;

		final long size;

		public NeighborhoodsIterableInterval(
				final RandomAccessibleInterval< T > source,
				final Interval span,
				final RectangleNeighborhoodFactory< T > factory )
		{
			super( source );
			this.source = source;
			this.span = span;
			this.factory = factory;
			long s = source.dimension( 0 );
			for ( int d = 1; d < n; ++d )
				s *= source.dimension( d );
			size = s;
		}

		@Override
		public Cursor< Neighborhood< T >> cursor()
		{
			return new RectangleNeighborhoodCursor< T >( source, span, factory );
		}

		@Override
		public long size()
		{
			return size;
		}

		@Override
		public Neighborhood< T > firstElement()
		{
			return cursor().next();
		}

		@Override
		public Object iterationOrder()
		{
			return new FlatIterationOrder( this );
		}

		@Override
		public Iterator< Neighborhood< T >> iterator()
		{
			return cursor();
		}

		@Override
		public Cursor< Neighborhood< T >> localizingCursor()
		{
			return cursor();
		}
	}

	public static final class NeighborhoodsAccessible< T >
			extends AbstractEuclideanSpace implements RandomAccessible< Neighborhood< T > >
	{
		final RandomAccessible< T > source;

		final Interval span;

		final RectangleNeighborhoodFactory< T > factory;

		public NeighborhoodsAccessible(
				final RandomAccessible< T > source,
				final Interval span,
				final RectangleNeighborhoodFactory< T > factory )
		{
			super( source.numDimensions() );
			this.source = source;
			this.span = span;
			this.factory = factory;
		}

		@Override
		public RandomAccess< Neighborhood< T >> randomAccess()
		{
			return new RectangleNeighborhoodRandomAccess< T >( source, span, factory );
		}

		@Override
		public RandomAccess< Neighborhood< T >> randomAccess( final Interval interval )
		{
			return new RectangleNeighborhoodRandomAccess< T >( source, span, factory, interval );
		}

		public RandomAccessible< T > getSource()
		{
			return source;
		}

		public Interval getSpan()
		{
			return span;
		}

		public RectangleNeighborhoodFactory< T > getFactory()
		{
			return factory;
		}
	}

	@Override
	public Interval getStructuringElementBoundingBox( final int numDimensions )
	{
		return spanInterval;
	}
}

