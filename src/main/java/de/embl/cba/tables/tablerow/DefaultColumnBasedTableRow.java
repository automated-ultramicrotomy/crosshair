package de.embl.cba.tables.tablerow;

import de.embl.cba.tables.tablerow.ColumnBasedTableRow;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultColumnBasedTableRow implements ColumnBasedTableRow
{
	private final int row;
	private final Map< String, List< String > > columns;

	public DefaultColumnBasedTableRow( int row, Map< String, List< String > > columns )
	{
		this.row = row;
		this.columns = columns;
	}

	public Map< String, List< String > > getColumns()
	{
		return columns;
	}

	@Override
	public String getCell( String columnName )
	{
		return columns.get( columnName ).get( row );
	}

	@Override
	public void setCell( String columnName, String value )
	{
		columns.get( columnName ).set( row, value );
	}

	@Override
	public Set< String > getColumnNames()
	{
		return columns.keySet();
	}

	@Override
	public int rowIndex()
	{
		return row;
	}
}
