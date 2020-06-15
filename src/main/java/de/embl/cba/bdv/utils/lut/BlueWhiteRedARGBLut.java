package de.embl.cba.bdv.utils.lut;

import net.imglib2.type.numeric.ARGBType;

public class BlueWhiteRedARGBLut implements ARGBLut
{
	private static int alpha;
	private final int[] indices;
	private final int numColors;

	public BlueWhiteRedARGBLut( int numColors )
	{
		this.numColors = numColors;
		alpha = 255;
		indices = this.blueWhiteRedARGBIndices( numColors );
	}

	public BlueWhiteRedARGBLut( int numColors, int alpha )
	{
		this.numColors = numColors;
		this.alpha = alpha;
		indices = this.blueWhiteRedARGBIndices( numColors );
	}

	@Override
	public int getARGB( double x )
	{
		final int index = ( int ) ( x * ( numColors - 1 ) );
		if ( index < 0 || index > indices.length -1 )
		{
			return indices[ 0 ];
		}
		return indices[ index ];
	}


	/**
	 * Lookup table going from blue to white to red.
	 *
	 *
	 * @param
	 * 		numColors
	 * @return
	 * 		ARGB indices, encoding the colors
	 */
	private final static int[] blueWhiteRedARGBIndices( int numColors )
	{
		int[][] lut = new int[ 3 ][ numColors ];

		int[] blue = new int[]{ 0, 0, 255 };
		int[] white = new int[]{ 255, 255, 255 };
		int[] red = new int[]{ 255, 0, 0 };

		final int middle = numColors / 2;

		for ( int i = 0; i < middle; i++)
		{
			for ( int rgb = 0; rgb < 3; rgb++ )
			{
				lut[ rgb ][ i ] = (int) ( blue[ rgb ] + ( 1.0 * i / middle ) * ( white[ rgb ] - blue[ rgb ] ) );
			}
		}

		for ( int i = middle; i < numColors; i++)
		{
			for ( int rgb = 0; rgb < 3; rgb++ )
			{
				lut[ rgb ][ i ] = ( int ) ( white[ rgb ] + ( 1.0 * ( i - middle ) / middle ) * ( red[ rgb ] - white[ rgb ] ) );
			}
		}

		int[] argbIndices = new int[ numColors ];

		for (int i = 0; i < numColors; i++)
		{
			argbIndices[ i ] = ARGBType.rgba(
					lut[ 0 ][ i ],
					lut[ 1 ][ i ],
					lut[ 2 ][ i ],
					alpha );
		}

		return argbIndices;
	}

}
