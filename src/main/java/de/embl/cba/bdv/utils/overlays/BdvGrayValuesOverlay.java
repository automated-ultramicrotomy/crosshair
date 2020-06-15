package de.embl.cba.bdv.utils.overlays;

import bdv.util.*;
import de.embl.cba.bdv.utils.BdvUtils;
import net.imglib2.RealPoint;
import net.imglib2.type.numeric.ARGBType;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Map;

public class BdvGrayValuesOverlay extends BdvOverlay implements MouseMotionListener
{
	final Bdv bdv;
	final int fontSize;

	ArrayList< Double > values;
	ArrayList< ARGBType > colors;
	private final BdvOverlaySource< BdvGrayValuesOverlay > bdvOverlaySource;

	public BdvGrayValuesOverlay( Bdv bdv, int fontSize )
	{
		super();

		this.bdv = bdv;

		bdv.getBdvHandle().getViewerPanel()
				.getDisplay().addMouseMotionListener( this );

		this.fontSize = fontSize;

		values = new ArrayList<>(  );
		colors = new ArrayList<>(  );

		bdvOverlaySource = BdvFunctions.showOverlay( this,
			"gray values - overlay",
			BdvOptions.options().addTo( bdv ) );

	}

	public BdvOverlaySource< BdvGrayValuesOverlay > getBdvOverlaySource()
	{
		return bdvOverlaySource;
	}

	public void setValuesAndColors(
			ArrayList< Double > values, ArrayList< ARGBType > colors )
	{
		this.values = values;
		this.colors = colors;
	}

	@Override
	protected void draw( final Graphics2D g )
	{

		int[] stringPosition = getTextPosition();

		for ( int i = 0; i < values.size(); ++i )
		{
			final int colorIndex = colors.get( i ).get();
			g.setColor( new Color(
					ARGBType.red( colorIndex ),
					ARGBType.green( colorIndex ),
					ARGBType.blue( colorIndex ) )  );
			g.setFont( new Font("TimesRoman", Font.PLAIN, fontSize ) );
			g.drawString( "" + values.get( i ),
					stringPosition[ 0 ],
					stringPosition[ 1 ] + fontSize * i + 5);
		}

	}

	private int[] getTextPosition()
	{
		final int height = bdv.getBdvHandle().getViewerPanel().getDisplay().getHeight();

		return new int[]{ 10, height - ( values.size() + 1 ) * fontSize  };
	}

	@Override
	public void mouseDragged( MouseEvent e )
	{

	}

	@Override
	public void mouseMoved( MouseEvent e )
	{
		final RealPoint realPoint = new RealPoint( 3 );
		bdv.getBdvHandle().getViewerPanel().getGlobalMouseCoordinates( realPoint );

		final int currentTimepoint =
				bdv.getBdvHandle().getViewerPanel().getState().getCurrentTimepoint();

		final Map< Integer, Double > pixelValuesOfActiveSources =
				BdvUtils.getPixelValuesOfActiveSources(
						bdv, realPoint, currentTimepoint );

		ArrayList< Double > values = new ArrayList<>(  );
		ArrayList< ARGBType > colors = new ArrayList<>(  );

		for ( int sourceId : pixelValuesOfActiveSources.keySet() )
		{
			values.add( pixelValuesOfActiveSources.get( sourceId ) );
			final ARGBType color = BdvUtils.getSourceColor( bdv, sourceId );
			final int colorIndex = color.get();
			if ( colorIndex == 0 )
			{
				colors.add( new ARGBType(
						ARGBType.rgba( 255, 255, 255, 255 ) ) );
			}
			else
			{
				colors.add( color );
			}
		}

		setValuesAndColors( values, colors );
	}
}
