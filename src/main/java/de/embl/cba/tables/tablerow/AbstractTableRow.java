package de.embl.cba.tables.tablerow;

import de.embl.cba.tables.select.Listeners;

public abstract class AbstractTableRow
{
	protected final Listeners.SynchronizedList< TableRowListener > listeners = new Listeners.SynchronizedList<>( );

	public Listeners< TableRowListener > listeners()
	{
		return listeners;
	}

	protected void notifyCellChangedListeners( final String columnName, final String value )
	{
		for ( TableRowListener listener : listeners.list )
			new Thread( () -> listener.cellChanged( columnName, value ) ).start();
	}
}
