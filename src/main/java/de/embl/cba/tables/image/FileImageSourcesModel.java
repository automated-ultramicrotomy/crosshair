package de.embl.cba.tables.image;

import de.embl.cba.bdv.utils.sources.ImagePlusFileSource;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.image.SourceAndMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileImageSourcesModel implements ImageSourcesModel
{
	private final Map< String, SourceAndMetadata< ? > > nameToSourceAndMetadata;
	private final boolean is2D;

	public FileImageSourcesModel( boolean is2D )
	{
		this.is2D = is2D;
		nameToSourceAndMetadata = new HashMap<>();
	}

	@Override
	public Map< String, SourceAndMetadata< ? > > sources()
	{
		return nameToSourceAndMetadata;
	}

	@Override
	public boolean is2D()
	{
		return is2D;
	}

	public void addSourceAndMetadata(
			String imageId,
			String imageDisplayName,
			String imagePath,
			List< String > imageSetIDs,
			Metadata.Modality flavor )
	{
		if ( nameToSourceAndMetadata.containsKey( imageId ) ) return;

		final Metadata metadata = new Metadata( imageId );
		metadata.modality = flavor;
		metadata.imageSetIDs = imageSetIDs;
		metadata.displayName = imageDisplayName;

		if ( imagePath.endsWith( ".xml" ) )
		{
			final LazySpimSource lazySpimSource = new LazySpimSource( imageId, imagePath );
			nameToSourceAndMetadata.put(
					imageId,
					new SourceAndMetadata( lazySpimSource, metadata ) );
		}
		else
		{
			final ImagePlusFileSource imagePlusFileSource =
					new ImagePlusFileSource( metadata, imagePath );

			nameToSourceAndMetadata.put(
					imageId,
					new SourceAndMetadata( imagePlusFileSource, metadata ) );
		}

	}

}
