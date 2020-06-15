package de.embl.cba.tables.color;

import net.imglib2.type.numeric.ARGBType;

import java.awt.*;

public abstract class ColorUtils
{
	public static Color getColor( ARGBType argbType )
	{
		final int colorIndex = argbType.get();

		return new Color(
				ARGBType.red( colorIndex ),
				ARGBType.green( colorIndex ),
				ARGBType.blue( colorIndex ),
				ARGBType.alpha( colorIndex ));
	}

	public static ARGBType getARGBType( Color color )
	{
		return new ARGBType( ARGBType.rgba( color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() ) );
	}

	public static Color getColor( String name ) {
		try {
			return (Color)Color.class.getField(name.toUpperCase()).get(null);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			return null;
		}
	}

	public static ARGBType getARGBType( String name ) {
		final Color color = getColor( name );
		if ( color == null ) return null;
		else return getARGBType( color );
	}
}
