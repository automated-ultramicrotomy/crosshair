package de.embl.cba.tables.view;

import java.awt.*;

public abstract class Globals
{
	public static int numberOfHorizontalUIComponents = 3;

	public static int proposedComponentWindowWidth()
	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		return screenSize.width / numberOfHorizontalUIComponents;
	}
}
