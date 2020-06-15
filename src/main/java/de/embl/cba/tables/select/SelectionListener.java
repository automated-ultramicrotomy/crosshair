package de.embl.cba.tables.select;

import de.embl.cba.tables.select.SelectionModel;

/**
 * Interface for listeners of a {@link SelectionModel}.
 *
 */
public interface SelectionListener< T >
{
	/**
	 * Notifies when the select has changed.
	 */
	void selectionChanged();

	/**
	 * Notifies when a focus event happened.
	 * Focus events do not necessarily enter the select at all..
	 */
	void focusEvent( T selection );

}