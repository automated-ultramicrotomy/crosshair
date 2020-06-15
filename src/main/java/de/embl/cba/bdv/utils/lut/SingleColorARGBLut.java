package de.embl.cba.bdv.utils.lut;

public class SingleColorARGBLut implements ARGBLut
{
	private final int r;
	private final int g;
	private final int b;

	public SingleColorARGBLut( int r, int g, int b )
	{
		this.r = r;
		this.g = g;
		this.b = b;
	}

	@Override
	public int getARGB( double x )
	{
		return Luts.getARGBIndex( r, g, b, x );
	}
}
