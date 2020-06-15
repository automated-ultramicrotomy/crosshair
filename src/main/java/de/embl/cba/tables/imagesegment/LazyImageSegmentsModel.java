package de.embl.cba.tables.imagesegment;

import de.embl.cba.tables.imagesegment.DefaultImageSegment;
import de.embl.cba.tables.imagesegment.ImageSegmentsModel;
import de.embl.cba.tables.imagesegment.LabelFrameAndImage;

import java.util.HashMap;
import java.util.Map;

public class LazyImageSegmentsModel
		implements ImageSegmentsModel< DefaultImageSegment >
{
	private final Map< LabelFrameAndImage, DefaultImageSegment > keyToSegment;
	private String modelName;

	public LazyImageSegmentsModel( String modelName )
	{
		this.modelName = modelName;
		keyToSegment = new HashMap<>(  );
	}

	@Override
	public DefaultImageSegment getImageSegment( LabelFrameAndImage labelFrameAndImage )
	{
		if ( ! keyToSegment.keySet().contains( labelFrameAndImage ) )
		{
			addSegment( labelFrameAndImage );
		}

		return keyToSegment.get( labelFrameAndImage );

	}

	@Override
	public String getName()
	{
		return modelName;
	}

	private synchronized void addSegment( LabelFrameAndImage labelFrameAndImage )
	{
		final DefaultImageSegment imageSegment = new DefaultImageSegment(
				labelFrameAndImage.getImage(),
				labelFrameAndImage.getLabel(),
				labelFrameAndImage.getFrame(),
				0,0,0,
				null );

		keyToSegment.put( labelFrameAndImage, imageSegment );
	}

}
