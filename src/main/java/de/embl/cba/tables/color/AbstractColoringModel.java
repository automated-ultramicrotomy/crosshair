package de.embl.cba.tables.color;

import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.select.Listeners;
import net.imglib2.type.numeric.ARGBType;

import javax.swing.*;

public abstract class AbstractColoringModel< T > implements ColoringModel< T >
{
	protected final Listeners.SynchronizedList< ColoringListener > listeners
			= new Listeners.SynchronizedList< ColoringListener >(  );

	@Override
	public Listeners< ColoringListener > listeners()
	{
		return listeners;
	}

	@Override
	public void convert( T input, ARGBType output )
	{
		output.set( 0 );
	}

	protected void notifyColoringListeners()
	{
		for ( ColoringListener listener : listeners.list )
		{
			SwingUtilities.invokeLater( () -> listener.coloringChanged() );
		}
	}
}
