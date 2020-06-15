package de.embl.cba.tables;

import de.embl.cba.tables.Utils;
import de.embl.cba.tables.imagesegment.ColumnBasedTableRowImageSegment;
import de.embl.cba.tables.tablerow.TableRow;
import org.fife.rsta.ac.js.Logger;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.*;

public abstract class TableRows
{
	public static < T extends TableRow >
	void addColumn( List< T > tableRows, String columnName, Object[] values )
	{
		if ( tableRows.get( 0 ) instanceof ColumnBasedTableRowImageSegment )
		{
			final Map< String, List< String > > columns
					= ( ( ColumnBasedTableRowImageSegment ) tableRows.get( 0 ) ).getColumns();

			final ArrayList< String > strings = new ArrayList<>();
			for ( int i = 0; i < values.length; i++ )
				strings.add( values[ i ].toString() );

			columns.put( columnName, strings );
		}
		else
		{
			throw new UnsupportedOperationException(
					"TableRow class not supported yet: " + tableRows.get( 0 ).getClass());
		}
	}

	public static < T extends TableRow >
	void addColumn( List< T > tableRows, String columnName, Object value )
	{
		final Object[] values = new Object[ tableRows.size() ];
		Arrays.fill( values, value );

		addColumn( tableRows, columnName, values );
	}

	@Deprecated
	public static < T extends TableRow >
	void assignValues(
			final String column,
			final Set< T > rows,
			final String value,
			JTable table )
	{
		for ( T row : rows )
			assignValue( column, row, value, table );
	}

	/**
	 * Write the values both in the TableRows and JTable
	 *
	 * TODO: this should not have to do it also in the table model.
	 * somehow there should be a notification that the values in the table row have changed.
	 * and then the TableView should take care of this!!
	 *
	 * @param column
	 * @param row
	 * @param attribute
	 * @param table
	 */
	@Deprecated
	public static  < T extends TableRow >
	void assignValue( String column,
					  T row,
					  String attribute,
					  JTable table )
	{
		// TODO: this should happen inside TableView
		final TableModel model = table.getModel();
		final int columnIndex = table.getColumnModel().getColumnIndex( column );

		final Object valueToBeReplaced = model.getValueAt(
				row.rowIndex(),
				columnIndex
		);

		if ( valueToBeReplaced.getClass().equals( Double.class ) )
		{
			try
			{
				final double number = Utils.parseDouble( attribute );

				model.setValueAt(
						number,
						row.rowIndex(),
						columnIndex );

				row.setCell( column, attribute );
			}
			catch ( Exception e )
			{
				Logger.logError( "Entered value must be numeric for column: "
						+ column );
			}
		}
		else
		{
			model.setValueAt(
					attribute,
					row.rowIndex(),
					columnIndex );

			row.setCell( column, attribute );
		}
	}

	/**
	 * Write the values both in the TableRows and JTable
	 *
	 * TODO: this should not have to do it also in the table model.
	 * somehow there should be a notification that the values in the table row have changed.
	 * and then the TableView should take care of this!!
	 *
	 * @param column
	 * @param row
	 * @param attribute
	 * @param table
	 * @param rowIndex
	 */
	public static < T extends TableRow >
	void setTableCell(
			int rowIndex,
			String column,
			String value,
			JTable table )
	{
		final TableModel model = table.getModel();
		final int columnIndex = table.getColumnModel().getColumnIndex( column );

		final Object valueToBeReplaced = model.getValueAt( rowIndex, columnIndex );

		if ( valueToBeReplaced.getClass().equals( Double.class ) )
		{
			try
			{
				final double number = Utils.parseDouble( value );

				model.setValueAt(
						number,
						rowIndex,
						columnIndex );
			}
			catch ( Exception e )
			{
				Logger.logError( "Entered value must be numeric for column: " + column );
			}
		}
		else
		{
			model.setValueAt(
					value,
					rowIndex,
					columnIndex );
		}
	}
}
