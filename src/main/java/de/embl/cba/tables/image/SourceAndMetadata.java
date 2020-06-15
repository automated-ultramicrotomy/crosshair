package de.embl.cba.tables.image;

import bdv.viewer.Source;
import de.embl.cba.bdv.utils.sources.Metadata;
import net.imglib2.type.numeric.RealType;

public class SourceAndMetadata< R extends RealType< R > >
{
	private final Source< R > source;
	private final Metadata metadata;

	public SourceAndMetadata( Source< R > source, Metadata metadata )
	{
		this.source = source;
		this.metadata = metadata;
	}

	public Source< R > source()
	{
		return source;
	}

	public Metadata metadata()
	{
		return metadata;
	}

}
