package de.embl.cba.bdv.utils.converters;

import de.embl.cba.bdv.utils.lut.Luts;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;

import java.util.function.Function;

public class MappingLinearARGBConverter extends LinearARGBConverter
{
	final Function< Double, Double > mappingFn;

	public MappingLinearARGBConverter( double min, double max, Function< Double, Double > mappingFn )
	{
		this( min, max, Luts.GRAYSCALE, mappingFn );
	}

	public MappingLinearARGBConverter(
			double min,
			double max,
			byte[][] lut,
			Function< Double, Double > mappingFn )
	{
		super( min, max, lut );
		this.mappingFn = mappingFn;
	}

	@Override
	public void convert( RealType realType, VolatileARGBType volatileARGBType )
	{
		final Double mappedValue = mappingFn.apply( realType.getRealDouble() );

		if ( mappedValue == null )
		{
			volatileARGBType.set( 0 );
			return;
		}

		final byte lutIndex = computeLutIndex( mappedValue );

		volatileARGBType.set( Luts.getARGBIndex( lutIndex, lut ) );
	}

	public Function< Double, Double > getMappingFn()
	{
		return mappingFn;
	}

}
