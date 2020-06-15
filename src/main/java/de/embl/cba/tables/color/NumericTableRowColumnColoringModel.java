package de.embl.cba.tables.color;

import de.embl.cba.bdv.utils.lut.ARGBLut;
import de.embl.cba.tables.Utils;
import de.embl.cba.tables.color.AbstractColoringModel;
import de.embl.cba.tables.color.ColumnColoringModel;
import de.embl.cba.tables.color.NumericColoringModel;
import de.embl.cba.tables.tablerow.TableRow;
import net.imglib2.type.numeric.ARGBType;


// TODO: extract abstract class NumericFeatureColoringModel
public class NumericTableRowColumnColoringModel< T extends TableRow >
		extends AbstractColoringModel< T > implements NumericColoringModel< T >, ColumnColoringModel
{
	private final String columnName;
	private final ARGBLut lut;
	private double[] lutMinMax;
	private double[] lutRange;
	// TODO: also capture this with  inputToFixedColor logic
	private final boolean isZeroTransparent;

	public NumericTableRowColumnColoringModel(
			String columnName,
			ARGBLut lut,
			double[] lutMinMax,
			double[] lutRange,
			boolean isZeroTransparent )
	{
		this.columnName = columnName;
		this.lut = lut;
		this.lutMinMax = lutMinMax;
		this.lutRange = lutRange;
		this.isZeroTransparent = isZeroTransparent;
	}

	@Override
	public void convert( T tableRow, ARGBType output )
	{
		final String cell = tableRow.getCell( columnName );
		final Double value = Utils.parseDouble( cell );
		setColorLinearly( value, output );
	}

	@Override
	public double getMin()
	{
		return lutMinMax[ 0 ];
	}


	@Override
	public double getMax()
	{
		return lutMinMax[ 1 ];
	}


	@Override
	public void setMin( double min )
	{
		this.lutMinMax[ 0 ] = min;
		notifyColoringListeners();
	}

	@Override
	public void setMax( double max )
	{
		this.lutMinMax[ 1 ] = max;
		notifyColoringListeners();
	}

	private void setColorLinearly( Double value, ARGBType output )
	{
		if ( isZeroTransparent )
		{
			if ( value == 0 )
			{
				output.set( ARGBType.rgba( 0, 0, 0, 0 ) );
				return;
			}
		}

		if ( value.isNaN() )
		{
			output.set( ARGBType.rgba( 0, 0, 0, 0 ) );
			return;
		}

		double normalisedValue = computeLinearNormalisedValue( value );
		final int colorIndex = lut.getARGB( normalisedValue );
		output.set( colorIndex );
	}

	private double computeLinearNormalisedValue( double value )
	{
		double normalisedValue = 0;
		if ( lutMinMax[ 1 ] == lutMinMax[ 0 ] )
		{
			if ( lutMinMax[ 1 ] == lutRange[ 0 ] )
				normalisedValue = 1.0;
			else if ( lutMinMax[ 1 ] == lutRange[ 1 ] )
				normalisedValue = 0.0;
		}
		else
		{
			normalisedValue =
					Math.max(
							Math.min(
									( value - lutMinMax[ 0 ] )
											/ ( lutMinMax[ 1 ] - lutMinMax[ 0 ] ), 1.0 ), 0.0 );
		}
		return normalisedValue;
	}

	@Override
	public String getColumnName()
	{
		return columnName;
	}
}
