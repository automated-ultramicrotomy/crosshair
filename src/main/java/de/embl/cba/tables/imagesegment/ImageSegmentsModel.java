package de.embl.cba.tables.imagesegment;

import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.imagesegment.LabelFrameAndImage;

public interface ImageSegmentsModel < T extends ImageSegment >
{
	T getImageSegment( LabelFrameAndImage labelFrameAndImage );

	String getName();
}
