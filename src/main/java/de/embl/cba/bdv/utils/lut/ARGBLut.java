package de.embl.cba.bdv.utils.lut;

public interface ARGBLut
{
	/**
	 *
	 * @param x
	 * 			value between zero and one to specify the color
	 * @return ARGB color index
	 */
	int getARGB( double x );
}
