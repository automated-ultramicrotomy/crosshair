package de.embl.cba.tables.image;

import bdv.viewer.Source;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.image.SourceAndMetadata;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;

import java.util.HashMap;
import java.util.Map;

public class DefaultImageSourcesModel implements ImageSourcesModel
{
	private final Map< String, SourceAndMetadata< ? > > nameToSourceAndMetadata;
	private boolean is2D;

	public DefaultImageSourcesModel( boolean is2D )
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

	public < R extends RealType< R > > void addSourceAndMetadata(
			Source< R > source,
			String imageId,
			Metadata.Modality flavor,
			int numSpatialDimensions,
			String segmentsTablePath,
			double displayRangeMax
	)
	{

		final Metadata metadata = new Metadata( imageId );
		metadata.modality = flavor;
		metadata.numSpatialDimensions = numSpatialDimensions;
		metadata.segmentsTablePath = segmentsTablePath;
		metadata.contrastLimits = new double[]{ 0, displayRangeMax };

		nameToSourceAndMetadata.put( imageId, new SourceAndMetadata( source, metadata ) );
	}

	public < R extends RealType< R > > void addSourceAndMetadata(
			Source< R > source,
			String imageId,
			Metadata.Modality flavor,
			int numSpatialDimensions,
			AffineTransform3D transform,
			String segmentsTablePath
	)
	{

		final Metadata metadata = new Metadata( imageId );
		metadata.modality = flavor;
		metadata.numSpatialDimensions = numSpatialDimensions;
//		metadata.sourceTransform = transform;
		metadata.segmentsTablePath = segmentsTablePath;

		nameToSourceAndMetadata.put( imageId, new SourceAndMetadata( source, metadata ) );
	}

	public < R extends RealType< R > > void addSourceAndMetadata(
			String imageId,
			SourceAndMetadata< R > sourceAndMetadata )
	{
		nameToSourceAndMetadata.put( imageId, sourceAndMetadata );
	}

}
