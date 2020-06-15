package de.embl.cba.tables.table;

import java.util.List;

public class RowListTableModel< T >
{
	private final List< T > rows;

	public RowListTableModel( List< T > rows )
	{
		this.rows = rows;
	}

	public List< T > getRows()
	{
		return rows;
	}


}
