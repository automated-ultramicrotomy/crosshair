package de.embl.cba.tables.imagesegment;

import net.imglib2.FinalRealInterval;
import net.imglib2.RealLocalizable;

public interface ImageSegment extends RealLocalizable
{
	String imageId();

	double labelId();

	int timePoint();

	FinalRealInterval boundingBox();

	void setBoundingBox( FinalRealInterval boundingBox );

	float[] getMesh();

	void setMesh( float[] mesh );
}
