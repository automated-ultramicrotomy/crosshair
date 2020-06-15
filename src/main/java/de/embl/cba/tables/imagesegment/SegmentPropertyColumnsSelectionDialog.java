package de.embl.cba.tables.imagesegment;

import de.embl.cba.tables.imagesegment.SegmentProperty;
import ij.Prefs;
import ij.gui.GenericDialog;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class SegmentPropertyColumnsSelectionDialog
{
	public static final String NO_COLUMN_SELECTED = "None";
	public static final String IMAGE_SEGMENT_COORDINATE_COLUMN_PREFIX = "SegmentPropertyColumn.";

	private String[] columnChoices;
	private final GenericDialog gd;

	public SegmentPropertyColumnsSelectionDialog( Collection< String > columns )
	{
		setColumnChoices( columns );

		gd = new GenericDialog( "Image Segments Properties Columns Selection" );

		addColumnSelectionUIs();
	}

	private void addColumnSelectionUIs()
	{
		for ( SegmentProperty coordinate : SegmentProperty.values() )
		{
			final String previousChoice =
					Prefs.get( getKey( coordinate ), columnChoices[ 0 ] );
			gd.addChoice( coordinate.toString(), columnChoices, previousChoice );
		}
	}

	private Map< SegmentProperty, String > collectChoices()
	{
		final HashMap< SegmentProperty, String > coordinateToColumnName = new HashMap<>();

		for ( SegmentProperty coordinate : SegmentProperty.values() )
		{
			final String columnName = gd.getNextChoice();
			coordinateToColumnName.put( coordinate, columnName );
			Prefs.set( getKey( coordinate ), columnName );
		}

		Prefs.savePreferences();

		return coordinateToColumnName;
	}

	private String getKey( SegmentProperty coordinate )
	{
		return IMAGE_SEGMENT_COORDINATE_COLUMN_PREFIX + coordinate.toString();
	}

	private void setColumnChoices( Collection< String > columns )
	{
		final int numColumns = columns.size();

		columnChoices = new String[ numColumns + 1 ];

		columnChoices[ 0 ] = NO_COLUMN_SELECTED;

		int i = 1;
		for ( String column : columns )
			columnChoices[ i++ ] = column;
	}


	public Map< SegmentProperty, String > fetchUserInput()
	{
		gd.showDialog();

		if ( gd.wasCanceled() ) return null;

		final Map< SegmentProperty, String > coordinateToColumn = collectChoices();

		return coordinateToColumn;
	}


}
