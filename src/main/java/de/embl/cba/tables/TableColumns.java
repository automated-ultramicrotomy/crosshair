package de.embl.cba.tables;

import de.embl.cba.tables.Tables;
import de.embl.cba.tables.Utils;
import ij.measure.ResultsTable;

import javax.activation.UnsupportedDataTypeException;
import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.*;

public class TableColumns
{
	public static Map< String, List< String > >
			columnsFromImageJ1ResultsTable(
			ResultsTable resultsTable )
	{
		List< String > columnNames = Arrays.asList( resultsTable.getHeadings() );
		final int numRows = resultsTable.size();

		final Map< String, List< String > > columnNamesToValues
				= new LinkedHashMap<>();

		for ( String columnName : columnNames )
		{
			System.out.println( "Parsing column: " + columnName );

			final double[] columnValues = getColumnValues( resultsTable, columnName );

			final List< String > list = new ArrayList<>( );
			for ( int row = 0; row < numRows; ++row )
				list.add( "" + columnValues[ row ] );

			columnNamesToValues.put( columnName, list );
		}

		return columnNamesToValues;
	}

	private static double[] getColumnValues( ResultsTable table, String heading )
	{
		String[] allHeaders = table.getHeadings();

		// Check if column header corresponds to row label header
		boolean hasRowLabels = hasRowLabelColumn(table);
		if (hasRowLabels && heading.equals(allHeaders[0]))
		{
			// need to parse row label column
			int nr = table.size();
			double[] values = new double[nr];
			for (int r = 0; r < nr; r++)
			{
				String label = table.getLabel(r);
				values[r] = Utils.parseDouble(label);
			}
			return values;
		}

		// determine index of column
		int index = table.getColumnIndex(heading);
		if ( index == ResultsTable.COLUMN_NOT_FOUND )
		{
			throw new RuntimeException("Unable to find column index from header: " + heading);
		}
		return table.getColumnAsDoubles(index);
	}

	private static final boolean hasRowLabelColumn( ResultsTable table )
	{
		return table.getLastColumn() == (table.getHeadings().length-2);
	}

	public static Map< String, List< String > >
	stringColumnsFromTableFile( final String path )
	{
		return stringColumnsFromTableFile( path, null );
	}

	public static Map< String, List< String > > stringColumnsFromTableFile(
			final String path,
			String delim )
	{
		final List< String > rowsInTableIncludingHeader = Tables.readRows( path );

		delim = Tables.autoDelim( delim, rowsInTableIncludingHeader );

		List< String > columnNames = Tables.getColumnNames( rowsInTableIncludingHeader, delim );

		final Map< String, List< String > > columnNameToStrings = new LinkedHashMap<>();

		final int numColumns = columnNames.size();

		for ( int columnIndex = 0; columnIndex < numColumns; columnIndex++ )
		{
			final String columnName = columnNames.get( columnIndex );
			columnNameToStrings.put( columnName, new ArrayList<>( ) );
		}

		final int numRows = rowsInTableIncludingHeader.size() - 1;

		final long start = System.currentTimeMillis();

		for ( int row = 1; row <= numRows; ++row )
		{
			final String[] split = rowsInTableIncludingHeader.get( row ).split( delim );
			for ( int columnIndex = 0; columnIndex < numColumns; columnIndex++ )
			{
				columnNameToStrings.get( columnNames.get( columnIndex ) ).add( split[ columnIndex ].replace( "\"", "" ) );
			}
		}

		// System.out.println( ( System.currentTimeMillis() - start ) / 1000.0 ) ;

		return columnNameToStrings;
	}

	public static Map< String, List< String > >
	orderedStringColumnsFromTableFile(
			final String path,
			String delim,
			String mergeByColumnName,
			ArrayList< Double > mergeByColumnValues )
	{
		final List< String > rowsInTableIncludingHeader = Tables.readRows( path );

		delim = Tables.autoDelim( delim, rowsInTableIncludingHeader );

		List< String > columnNames = Tables.getColumnNames( rowsInTableIncludingHeader, delim );

		final Map< String, List< String > > columnNameToStrings = new LinkedHashMap<>();

		int mergeByColumnIndex = -1;

		final int numRowsTargetTable = mergeByColumnValues.size();
		final int numColumns = columnNames.size();

		for ( int columnIndex = 0; columnIndex < numColumns; columnIndex++ )
		{
			final String[] split = rowsInTableIncludingHeader.get( 1 ).split( delim );
			final String firstCell = split[ columnIndex ];

			String defaultValue = "None"; // for text
			if ( Tables.isNumeric( firstCell ) )
				defaultValue = "NaN"; // for numbers

			final ArrayList< String > values = new ArrayList< >( Collections.nCopies( numRowsTargetTable, defaultValue ));

			final String columnName = columnNames.get( columnIndex );
			columnNameToStrings.put( columnName, values );
			if ( columnName.equals( mergeByColumnName ) )
				mergeByColumnIndex = columnIndex;
		}

		if ( mergeByColumnIndex == -1 )
			throw new UnsupportedOperationException( "Column by which to merge not found: " + mergeByColumnName );

//		final long start = System.currentTimeMillis();
		final int numRowsSourceTable = rowsInTableIncludingHeader.size() - 1;

		// TODO: code looks inefficient...
		for ( int rowIndex = 0; rowIndex < numRowsSourceTable; ++rowIndex )
		{
			final String[] split = rowsInTableIncludingHeader.get( rowIndex + 1 ).split( delim );
			final String cell = split[ mergeByColumnIndex ];

			final Double orderValue = Utils.parseDouble( cell );
			final int targetRowIndex = mergeByColumnValues.indexOf( orderValue );

			for ( int columnIndex = 0; columnIndex < numColumns; columnIndex++ )
			{
				final String columName = columnNames.get( columnIndex );
				columnNameToStrings.get( columName ).set( targetRowIndex, split[ columnIndex ].replace( "\"", "" ) );
			}
		}

//		System.out.println( ( System.currentTimeMillis() - start ) / 1000.0 ) ;

		return columnNameToStrings;
	}

	public static Map< String, List< ? > >
	asTypedColumns( Map< String, List< String > > columnToStringValues )
			throws UnsupportedDataTypeException
	{
		final Set< String > columnNames = columnToStringValues.keySet();

		final LinkedHashMap< String, List< ? > > columnToValues = new LinkedHashMap<>();

		for ( String columnName : columnNames )
		{
			final List< ? > values = asTypedList( columnToStringValues.get( columnName ) );
			columnToValues.put( columnName, values );
		}

		return columnToValues;
	}

	public static List< ? > asTypedList( List< String > strings )
			throws UnsupportedDataTypeException
	{
		final Class columnType = getColumnType( strings.get( 0 ) );

		int numRows = strings.size();

		if ( columnType == Double.class )
		{
			final ArrayList< Double > doubles = new ArrayList<>( numRows );

			for ( int row = 0; row < numRows; ++row )
				doubles.add( Utils.parseDouble( strings.get( row ) ) );

			return doubles;
		}
		else if ( columnType == Integer.class ) // cast to Double anyway...
		{
			final ArrayList< Double > doubles = new ArrayList<>( numRows );

			for ( int row = 0; row < numRows; ++row )
				doubles.add( Utils.parseDouble( strings.get( row ) ) );

			return doubles;
		}
		else if ( columnType == String.class )
		{
			return strings;
		}
		else
		{
			throw new UnsupportedDataTypeException("");
		}
	}

	public static Object[] asTypedArray( List< String > strings ) throws UnsupportedDataTypeException
	{
		final Class columnType = getColumnType( strings.get( 0 ) );

		int numRows = strings.size();

		if ( columnType == Double.class )
		{
			return toDoubles( strings, numRows );
		}
		else if ( columnType == Integer.class ) // cast to Double anyway...
		{
			return toDoubles( strings, numRows );
		}
		else if ( columnType == String.class )
		{
			final String[] stringsArray = new String[ strings.size() ];
			strings.toArray( stringsArray );
			return stringsArray;
		}
		else
		{
			throw new UnsupportedDataTypeException("");
		}
	}

	public static Object[] toDoubles( List< String > strings, int numRows )
	{
		final Double[] doubles = new Double[ numRows ];

		for ( int row = 0; row < numRows; ++row )
			doubles[ row ] =  Utils.parseDouble( strings.get( row ) );

		return doubles;
	}

	private static Class getColumnType( String cell )
	{
		try
		{
			Utils.parseDouble( cell );
			return Double.class;
		}
		catch ( Exception e2 )
		{
			return String.class;
		}
	}

	public static Map< String, List< String > > addLabelImageIdColumn(
			Map< String, List< String > > columns,
			String columnNameLabelImageId,
			String labelImageId )
	{
		final int numRows = columns.values().iterator().next().size();

		final List< String > labelImageIdColumn = new ArrayList<>();

		for ( int row = 0; row < numRows; row++ )
			labelImageIdColumn.add( labelImageId );

		columns.put( columnNameLabelImageId, labelImageIdColumn );

		return columns;
	}

	public static ArrayList< Double > getNumericColumnAsDoubleList( JTable table, String columnName )
	{
		final int objectLabelColumnIndex = table.getColumnModel().getColumnIndex( columnName );

		final TableModel model = table.getModel();
		final int numRows = model.getRowCount();
		final ArrayList< Double > orderColumn = new ArrayList<>();
		for ( int rowIndex = 0; rowIndex < numRows; ++rowIndex )
			orderColumn.add( Utils.parseDouble( model.getValueAt( rowIndex, objectLabelColumnIndex ).toString() ) );
		return orderColumn;
	}

	public static Map< String, List< String > > openAndOrderNewColumns( JTable table, String mergeByColumnName, String newTablePath )
	{
		// TODO: this assumes that the ordering column is numeric; is this needed?
		final ArrayList< Double > orderColumn = getNumericColumnAsDoubleList(
				table,
				mergeByColumnName );

		final Map< String, List< String > > columNameToValues =
				orderedStringColumnsFromTableFile(
						newTablePath,
						null,
						mergeByColumnName,
						orderColumn );

		return columNameToValues;
	}
}
