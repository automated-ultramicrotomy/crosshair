package de.embl.cba.bdv.utils.converters;

import de.embl.cba.bdv.utils.lut.Luts;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;

public class LinearARGBConverter< R extends RealType< R >> implements Converter< R, VolatileARGBType >
{
	double min, max;
	byte[][] lut;
	private double scale;

	public LinearARGBConverter( double min, double max )
	{
		this( min, max, Luts.GRAYSCALE );
	}

	public LinearARGBConverter( double min, double max, byte[][] lut )
	{
		this.min = min;
		this.max = max;
		this.lut = lut;
	}

	@Override
	public void convert( R realType, VolatileARGBType volatileARGBType )
	{
		final byte lutIndex = computeLutIndex( realType.getRealDouble() );
		volatileARGBType.set( Luts.getARGBIndex( lutIndex, lut ) );
	}

	public void setMin( double min )
	{
		this.min = min;
	}

	public void setMax( double max )
	{
		this.max = max;
	}

	public double getMin()
	{
		return min;
	}

	public double getMax()
	{
		return max;
	}

	public void setLut( byte[][] lut )
	{
		this.lut = lut;
	}

	public byte computeLutIndex( final Double value )
	{
		return (byte) ( 255.0 * Math.max( Math.min( ( value - min ) / ( max - min ), 1.0 ), 0.0 ) );
	}
}
