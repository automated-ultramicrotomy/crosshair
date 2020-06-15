package de.embl.cba.tables.color;

import de.embl.cba.tables.select.SelectionModel;

/**
 * Interface for listeners of a {@link SelectionModel}.
 *
 */
public interface ColoringListener
{
	/**
	 * Notifies when the color has changed.
	 */
	public void coloringChanged();

}