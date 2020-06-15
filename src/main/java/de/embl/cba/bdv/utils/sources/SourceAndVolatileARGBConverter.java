package de.embl.cba.bdv.utils.sources;

import bdv.viewer.Source;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;

public class SourceAndVolatileARGBConverter< R extends RealType< R > >
{
	/**
	 * provides image data for all timepoints of one view.
	 */
	protected final Source< R > spimSource;

	/**
	 * converts {@link #spimSource} type T to VolatileARGBType for display
	 */
	protected final Converter< R, VolatileARGBType > converter;
	private final VolatileARGBType argbConvertedOutOfBoundsValue;

	public SourceAndVolatileARGBConverter(
			final Source< R > spimSource,
			final Converter< R, VolatileARGBType > converter,
			VolatileARGBType outOfBoundsValue )
	{
		this.spimSource = spimSource;
		this.converter = converter;
		this.argbConvertedOutOfBoundsValue = outOfBoundsValue;
	}

	public SourceAndVolatileARGBConverter(
			final Source< R > source,
			final Converter< R, VolatileARGBType > converter )
	{
		this( source, converter, new VolatileARGBType( 0 ) );
	}

	/**
	 * Get the {@link Source} (provides image data for all timepoints of one
	 * angle).
	 */
	public Source< R > getSpimSource()
	{
		return spimSource;
	}

	/**
	 * Get the {@link Converter} (converts source type T to ARGBType for
	 * display).
	 */
	public Converter< R, VolatileARGBType > getConverter()
	{
		return converter;
	}

}
