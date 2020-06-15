package de.embl.cba.bdv.utils.selection;

public interface BdvLabelSourceSelectionListener
{
	void selectionChanged( double label, int timePoint, boolean selected );
}
