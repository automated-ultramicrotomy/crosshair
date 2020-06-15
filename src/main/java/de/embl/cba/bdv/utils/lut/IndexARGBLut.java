package de.embl.cba.bdv.utils.lut;

public interface IndexARGBLut extends ARGBLut
{
	/**
	 *
	 * @param x
	 * 			value to specify the color
	 * @return ARGB color index
	 */
	int getARGB( int x );
}
