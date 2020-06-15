package de.embl.cba.tables.color;

import de.embl.cba.tables.select.SelectionModel;
import net.imglib2.type.numeric.ARGBType;

import java.util.Arrays;
import java.util.List;

public class SelectionColoringModel < T > extends AbstractColoringModel< T >
{
	ColoringModel< T > coloringModel;
	SelectionModel< T > selectionModel;

	private SelectionColoringMode selectionColoringMode;
	private ARGBType selectionColor;
	private double brightnessNotSelected;

	public static final ARGBType YELLOW =
			new ARGBType( ARGBType.rgba( 255, 255, 0, 255 ) );
	private final List< SelectionColoringMode > selectionColoringModes;

	public enum SelectionColoringMode
	{
		OnlyShowSelected,
		SelectionColor,
		SelectionColorAndDimNotSelected,
		DimNotSelected
	}

	public SelectionColoringModel(
			ColoringModel< T > coloringModel,
			SelectionModel< T > selectionModel )
	{
		setColoringModel( coloringModel );
		this.selectionModel = selectionModel;
		this.selectionColoringModes = Arrays.asList( SelectionColoringMode.values() );

		this.selectionColor = YELLOW;
		this.brightnessNotSelected = 0.1;
		this.selectionColoringMode = SelectionColoringMode.DimNotSelected;
	}

	@Override
	public void convert( T input, ARGBType output )
	{
		coloringModel.convert( input, output );

		if ( selectionModel.isEmpty() ) return;

		final boolean isSelected = selectionModel.isSelected( input );

		switch ( selectionColoringMode )
		{
			case DimNotSelected:

				if ( ! isSelected )
					dim( output, brightnessNotSelected );
				break;

			case OnlyShowSelected:

				if ( ! isSelected )
					dim( output, 0.0 );
				break;

			case SelectionColor:

				if ( isSelected )
					output.set( selectionColor );
				break;

			case SelectionColorAndDimNotSelected:

				if ( isSelected )
					output.set( selectionColor );
				else
					dim( output, brightnessNotSelected );
				break;

			default:
				break;
		}

	}

	public void dim( ARGBType output, double brightnessNotSelected )
	{
		final int colorIndex = output.get();
		output.set( ARGBType.rgba(
				ARGBType.red( colorIndex ),
				ARGBType.green( colorIndex ),
				ARGBType.blue( colorIndex ),
				brightnessNotSelected * 255 )  );
	}

	public void setSelectionColoringMode( SelectionColoringMode selectionColoringMode )
	{
		this.selectionColoringMode = selectionColoringMode;

		switch ( selectionColoringMode )
		{
			case DimNotSelected:
				brightnessNotSelected = 0.2;
				selectionColor = null;
				break;
			case OnlyShowSelected:
				brightnessNotSelected = 0.0;
				selectionColor = null;
				break;
			case SelectionColor:
				brightnessNotSelected = 1.0;
				selectionColor = YELLOW;
				break;
			case SelectionColorAndDimNotSelected:
				brightnessNotSelected = 0.2;
				selectionColor = YELLOW;
				break;
		}
		notifyColoringListeners();
	}

	public SelectionColoringMode getSelectionColoringMode()
	{
		return selectionColoringMode;
	}

	public void setSelectionColor( ARGBType selectionColor )
	{
		this.selectionColor = selectionColor;
		notifyColoringListeners();
	}

	public void setColoringModel( ColoringModel< T > coloringModel )
	{
		this.coloringModel = coloringModel;
		notifyColoringListeners();

		// chain event notification
		coloringModel.listeners().add( () -> de.embl.cba.tables.color.SelectionColoringModel.this.notifyColoringListeners() );
	}

	public ColoringModel< T > getColoringModel()
	{
		return coloringModel;
	}

	public void iterateSelectionMode()
	{
		final int selectionModeIndex = selectionColoringModes.indexOf( selectionColoringMode );

		if ( selectionModeIndex < selectionColoringModes.size() - 1 )
		{
			setSelectionColoringMode( selectionColoringModes.get( selectionModeIndex + 1 ) );
		}
		else
		{
			setSelectionColoringMode( selectionColoringModes.get( 0 ) );
		}
	}
}
