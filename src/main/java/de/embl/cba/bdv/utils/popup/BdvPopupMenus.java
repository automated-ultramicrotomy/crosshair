package de.embl.cba.bdv.utils.popup;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.capture.ViewCaptureDialog;
import de.embl.cba.swing.PopupMenu;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import java.util.concurrent.ConcurrentHashMap;

public abstract class BdvPopupMenus
{
	private static ConcurrentHashMap< BdvHandle, PopupMenu > bdvToPopup = new ConcurrentHashMap<>( );

	public static synchronized void addAction( BdvHandle bdvHandle, String actionName, ClickBehaviour clickBehaviour )
	{
		ensurePopupMenuExist( bdvHandle );
		bdvToPopup.get( bdvHandle ).addPopupAction( actionName, clickBehaviour );
	}

	public static synchronized void addAction( BdvHandle bdvHandle, String actionName, Runnable runnable )
	{
		ensurePopupMenuExist( bdvHandle );
		bdvToPopup.get( bdvHandle ).addPopupAction( actionName, runnable );
	}

	public static synchronized void addScreenshotAction( BdvHandle bdvHandle )
	{
		ensurePopupMenuExist( bdvHandle );
		bdvToPopup.get( bdvHandle ).addPopupAction( "Make Screenshot...", new ViewCaptureDialog( bdvHandle ) );
	}

	private static void ensurePopupMenuExist( BdvHandle bdvHandle )
	{
		if ( ! bdvToPopup.containsKey( bdvHandle ) )
		{
			bdvToPopup.put( bdvHandle, createPopupMenu( bdvHandle ) );
		}
	}

	private static PopupMenu createPopupMenu( BdvHandle bdvHandle )
	{
		final PopupMenu popupMenu = new PopupMenu();
		final Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdvHandle.getTriggerbindings(), "popup menu" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> popupMenu.show( bdvHandle.getViewerPanel().getDisplay(), x, y ), "show popup menu", "button3", "shift P" ) ;
		return popupMenu;
	}
}
