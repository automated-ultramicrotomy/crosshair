package de.embl.cba.bdv.utils.lut;

public class LinearARGBLut implements AdjustableARGBLut
{
	double min, max;

	byte[][] lut;

	public LinearARGBLut( double min, double max )
	{
		this.min = min;
		this.max = max;

		this.lut = Luts.GRAYSCALE;
	}

	public LinearARGBLut( byte[][] lut, double min, double max )
	{
		this.lut = lut;
		this.min = min;
		this.max = max;
	}

	@Override
	public int getARGBIndex( double x, double brightness )
	{
		final byte lutIndex = (byte) ( 255.0 * ( x - min ) / ( max - min ) );

		final int argbIndex = Luts.getARGBIndex( lutIndex, lut, brightness );

		return argbIndex;
	}

}
