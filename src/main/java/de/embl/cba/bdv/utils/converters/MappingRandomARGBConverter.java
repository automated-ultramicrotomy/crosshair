package de.embl.cba.bdv.utils.converters;

import de.embl.cba.bdv.utils.lut.Luts;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;

import java.util.ArrayList;
import java.util.function.Function;

public class MappingRandomARGBConverter extends RandomARGBConverter
{
	final Function< Double, ? extends Object > mappingFn;
	final private ArrayList< Object > uniqueObjectsList;

	public MappingRandomARGBConverter( Function< Double, ? extends Object > mappingFn )
	{
		this( mappingFn, Luts.GLASBEY );
	}

	public MappingRandomARGBConverter( Function< Double, ? extends Object > mappingFn, byte[][] lut )
	{
		super( lut );
		this.mappingFn = mappingFn;
		this.uniqueObjectsList = new ArrayList<>(  );
	}

	@Override
	public void convert( RealType realType, VolatileARGBType volatileARGBType )
	{
		Object object = mappingFn.apply( realType.getRealDouble() );

		if ( object == null )
		{
			volatileARGBType.set( 0 );
			return;
		}

		if( ! uniqueObjectsList.contains( object ) ) uniqueObjectsList.add( object );


		final double random = createRandom( uniqueObjectsList.indexOf( object ) + 1 );
		final byte lutIndex = (byte) ( 255.0 * random );
		volatileARGBType.set( Luts.getARGBIndex( lutIndex, lut ) );
	}

	public Function< Double, ? extends Object > getMappingFn()
	{
		return mappingFn;
	}

}
