package de.embl.cba.bdv.utils.selection;

import net.imglib2.FinalInterval;
import net.imglib2.RealInterval;

public interface Segment
{
	String imageId();

	double label();

	int timePoint();

	double[] position();

	FinalInterval boundingBox();
}
