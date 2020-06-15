package de.embl.cba.tables.tablerow;

public interface TableRowListener
{
	void cellChanged( String columnName, String value );
}
