package de.embl.cba.tables.tablerow;

import de.embl.cba.tables.select.Listeners;

import java.util.Set;

public interface TableRow
{
	Listeners< TableRowListener > listeners();

	String getCell( String columnName );

	void setCell( String columnName, String value );

	Set< String > getColumnNames();

	/**
	 * The index of the row in the underlying table.
	 * TODO: Maybe this is not needed...
	 *
	 * @return row index
	 */
	int rowIndex();
}
