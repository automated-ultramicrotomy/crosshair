package de.embl.cba.tables.color;

import de.embl.cba.bdv.utils.lut.ARGBLut;
import de.embl.cba.bdv.utils.lut.IndexARGBLut;
import de.embl.cba.tables.color.AbstractColoringModel;
import de.embl.cba.tables.color.CategoryColoringModel;
import net.imglib2.type.numeric.ARGBType;

import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.bdv.utils.converters.RandomARGBConverter.goldenRatio;

public class LazyCategoryColoringModel< T > extends AbstractColoringModel< T > implements CategoryColoringModel< T >
{
	private Map< T, ARGBType > inputToColorMap;
	private IndexARGBLut argbLut;
	private int randomSeed;

	/**
	 * Colors are lazily assigned to input elements,
	 * using the given {@code argbLut}.
	 *
	 * TODO: better to use here a "generating LUT" rather than a 0...1 LUT
	 *
	 * @param argbLut
	 */
	public LazyCategoryColoringModel( IndexARGBLut argbLut )
	{
		super();
		this.argbLut = argbLut;
		this.inputToColorMap = new HashMap<>(  );
		this.randomSeed = 42;
	}

	@Override
	public void convert( T input, ARGBType output )
	{
		if( ! inputToColorMap.keySet().contains( input ) )
		{
			//final double random = createRandom( inputToColorMap.size() + 1 );
			inputToColorMap.put( input, new ARGBType( argbLut.getARGB( inputToColorMap.size() + 1  ) ) );
		}

		output.set( inputToColorMap.get( input ).get() );
	}

	private double createRandom( double x )
	{
		double random = ( x * randomSeed ) * goldenRatio;
		random = random - ( long ) Math.floor( random );
		return random;
	}

	@Override
	public void incRandomSeed( )
	{
		inputToColorMap.clear();
		this.randomSeed++;

		notifyColoringListeners();
	}
}
