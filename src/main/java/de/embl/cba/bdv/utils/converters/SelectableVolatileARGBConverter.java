/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package de.embl.cba.bdv.utils.converters;

import bdv.viewer.TimePointListener;
import de.embl.cba.bdv.utils.selection.Segment;
import de.embl.cba.tables.color.LazyLabelsARGBConverter;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;

import java.util.HashSet;
import java.util.Set;

public class SelectableVolatileARGBConverter
		implements
		Converter< RealType, VolatileARGBType >,
		TimePointListener
{
	public static final ARGBType COLOR_SELECTED = new ARGBType( ARGBType.rgba( 255, 255, 0, 255 ) );
	public static final int BACKGROUND = 0;
	private Converter< RealType, VolatileARGBType > wrappedConverter;
	private double brightnessNotSelected;
	private SelectionMode selectionMode;
	private ARGBType selectionColor;
	private int currentTimePoint;
	private Set< ? extends Segment > selected;

	public enum SelectionMode
	{
		DimNotSelected,
		OnlyShowSelected,
		ColorSelectedBrightYellow,
		ColorSelectedBrightYellowAndDimNotSelected;
	}

	public SelectableVolatileARGBConverter()
	{
		this( new RandomARGBConverter() );
	}

	public SelectableVolatileARGBConverter(
			Converter< RealType, VolatileARGBType > realARGBConverter )
	{
		wrappedConverter = realARGBConverter;
		currentTimePoint = 0;
		selected = new HashSet<>( );
		setSelectionMode( SelectionMode.DimNotSelected );
	}

	@Override
	public void convert( final RealType input, final VolatileARGBType output )
	{
		if ( input instanceof Volatile )
		{
			if ( ! ( ( Volatile ) input ).isValid() )
			{
				output.setValid( false );
				return;
			}
		}

		setOutputColor( input, output);

		output.setValid( true );
	}

	private void setOutputColor( final RealType input,
								 final VolatileARGBType output )
	{

		if ( input.getRealDouble() == BACKGROUND )
		{
			output.set( 0 );
			return;
		}

		wrappedConverter.convert( input, output );

		final boolean selected = isSelected( input );

		if ( ! selected )
		{
			output.get().mul( brightnessNotSelected );
		}
		else if ( selected && selectionColor != null )
		{
			output.set( selectionColor.get() );
		}

	}

	private boolean isSelected( final RealType input )
	{
		final double label = input.getRealDouble();

		if ( selected.size() == 0 ) return true; // TODO: Does this make sense?

		for ( Segment segment : selected )
		{
			if ( segment.timePoint() == currentTimePoint
					&& segment.label() == label )
			{
				return true;
			}
		}

		return false;
	}

	public void setSelected( Set< ? extends Segment > selected )
	{
		this.selected = selected;
	}

	public void setBrightnessNotSelectedValues( final double brightnessNotSelected )
	{
		this.brightnessNotSelected = brightnessNotSelected;
	}

	public void setWrappedConverter( Converter< RealType, VolatileARGBType > converter )
	{
		this.wrappedConverter = converter;
	}

	public Converter< RealType, VolatileARGBType > getWrappedConverter()
	{
		return wrappedConverter;
	}

	public SelectionMode getSelectionMode()
	{
		return selectionMode;
	}

	public void setSelectionMode( SelectionMode selectionMode )
	{
		this.selectionMode = selectionMode;

		switch ( selectionMode )
		{
			case DimNotSelected:
				brightnessNotSelected = 0.2;
				selectionColor = null;
				break;
			case OnlyShowSelected:
				brightnessNotSelected = 0.0;
				selectionColor = null;
				break;
			case ColorSelectedBrightYellow:
				brightnessNotSelected = 1.0;
				selectionColor = COLOR_SELECTED;
				break;
			case ColorSelectedBrightYellowAndDimNotSelected:
				brightnessNotSelected = 0.2;
				selectionColor = COLOR_SELECTED;
				break;
		}

	}

	@Override
	public void timePointChanged( int timePointIndex )
	{
		currentTimePoint = timePointIndex;
	}
}
