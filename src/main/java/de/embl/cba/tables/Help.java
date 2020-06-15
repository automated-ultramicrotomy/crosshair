package de.embl.cba.tables;

import bdv.tools.HelpDialog;

public class Help
{
	public static void showSegmentationImageHelp()
	{
		HelpDialog helpDialog = new HelpDialog( null, de.embl.cba.tables.Help.class.getResource( "/SegmentationImageActionsHelp.html" ) );
		helpDialog.setVisible( true );
	}

	public static void showMultiImageSetNavigationHelp()
	{
		HelpDialog helpDialog = new HelpDialog( null, de.embl.cba.tables.Help.class.getResource( "/MultiImageSetNavigationHelp.html" ) );
		helpDialog.setVisible( true );
	}
}
