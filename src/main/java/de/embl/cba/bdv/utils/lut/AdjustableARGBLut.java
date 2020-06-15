package de.embl.cba.bdv.utils.lut;

public interface AdjustableARGBLut
{
	/**
	 *
	 * @param x
	 * 			value between zero and one to specify the color
	 * @param brightness
	 * 			Value between zero and one to specifiy the brightness of the color
	 * @return ARGB color index
	 */
	int getARGBIndex( double x, double brightness );
}
