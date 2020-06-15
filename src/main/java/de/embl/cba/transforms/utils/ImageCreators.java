package de.embl.cba.transforms.utils;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.AbstractImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;

public abstract class ImageCreators
{
	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > copyAsArrayImg( RandomAccessibleInterval< T > orig )
	{
		RandomAccessibleInterval< T > copy = new ArrayImgFactory( orig.randomAccess().get() ).create( orig );
		copy = Transforms.getWithAdjustedOrigin( orig, copy );
		LoopBuilder.setImages( copy, orig ).forEachPixel( ( c, o ) -> c.set( o ) );

		return copy;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createEmptyArrayImg( RandomAccessibleInterval< T > rai )
	{
		RandomAccessibleInterval< T > newImage = new ArrayImgFactory( rai.randomAccess().get() ).create( rai );
		newImage = Transforms.getWithAdjustedOrigin( rai, newImage );
		return newImage;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createEmptyCellImg( RandomAccessibleInterval< T > volume )
	{
		final int dimensionX = ( int ) volume.dimension( 0 );
		final int dimensionY = ( int ) volume.dimension( 1 );
		final int dimensionZ = ( int ) volume.dimension( 2 );

		int nz = dimensionZ;
		if ( AbstractImg.numElements( Intervals.dimensionsAsLongArray( volume ) ) >  Integer.MAX_VALUE - 1 )
			nz  = ( Integer.MAX_VALUE / 2 ) / ( dimensionX * dimensionY );

		final int[] cellSize = {
				dimensionX,
				dimensionY,
				nz };

		RandomAccessibleInterval< T > newImage = new CellImgFactory<>(
				volume.randomAccess().get(),
				cellSize ).create( volume );

		newImage = Transforms.getWithAdjustedOrigin( volume, newImage );
		return newImage;
	}
}
