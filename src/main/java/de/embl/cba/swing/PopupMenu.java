package de.embl.cba.swing;

import org.scijava.ui.behaviour.ClickBehaviour;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PopupMenu
{
	private JPopupMenu popup;
	private int x;
	private int y;

	public PopupMenu()
	{
		createPopupMenu();
	}

	private void createPopupMenu()
	{
		popup = new JPopupMenu();
	}

	private void addPopupLine() {
		popup.addSeparator();
	}

	public void addPopupAction( String actionName, ClickBehaviour clickBehaviour ) {

		JMenuItem menuItem = new JMenuItem( actionName );
		menuItem.addActionListener( e -> new Thread( () -> clickBehaviour.click( x, y ) ).start() );
		popup.add( menuItem );
	}

	public void addPopupAction( String actionName, Runnable runnable ) {

		JMenuItem menuItem = new JMenuItem( actionName );
		menuItem.addActionListener( e -> new Thread( () -> runnable.run() ).start() );
		popup.add( menuItem );
	}

	public void show( JComponent display, int x, int y )
	{
		this.x = x;
		this.y = y;
		popup.show( display, x, y );
	}
}
