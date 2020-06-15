package de.embl.cba.bdv.utils.converters;

import de.embl.cba.bdv.utils.lut.Luts;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RandomARGBConverter implements Converter< RealType, VolatileARGBType >
{
	final static public double goldenRatio = 1.0 / ( 0.5 * Math.sqrt( 5 ) + 0.5 );
	long seed;
	byte[][] lut;
	Map< Double, Integer > doubleToARGBIndex = new ConcurrentHashMap<>(  );

	public RandomARGBConverter( )
	{
		this.lut = Luts.GLASBEY;
		this.seed = 50;
	}

	public RandomARGBConverter( byte[][] lut )
	{
		this.lut = lut;
		this.seed = 50;
	}

	public double createRandom( double x )
	{
		double random = ( x * seed ) * goldenRatio;
		random = random - ( long ) Math.floor( random );
		return random;
	}

	public void setLut( byte[][] lut )
	{
		this.lut = lut;
	}

	public long getSeed()
	{
		return seed;
	}

	public void setSeed( long seed )
	{
		this.seed = seed;
	}


	@Override
	public void convert( RealType realType, VolatileARGBType volatileARGBType )
	{
		final double realDouble = realType.getRealDouble();

		if ( ! doubleToARGBIndex.containsKey( realDouble ) )
		{
			final double random = createRandom( realDouble );
			final int argbIndex = Luts.getARGBIndex( ( byte ) ( 255.0 * random ), lut );
			doubleToARGBIndex.put( realDouble, argbIndex );
		}

		volatileARGBType.get().set( doubleToARGBIndex.get( realDouble ) );
	}
}
