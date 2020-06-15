package de.embl.cba.tables.imagesegment;

import de.embl.cba.tables.imagesegment.ImageSegment;

import java.util.Objects;

public class LabelFrameAndImage
{
	private final String image;
	private final double label;
	private final int frame;

	public LabelFrameAndImage( double label, int frame, String image )
	{
		this.label = label;
		this.frame = frame;
		this.image = image;

	}

	public LabelFrameAndImage( ImageSegment imageSegment )
	{
		this.image = imageSegment.imageId();
		this.label = imageSegment.labelId();
		this.frame = imageSegment.timePoint();
	}

	@Override
	public boolean equals( Object o )
	{
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;
		de.embl.cba.tables.imagesegment.LabelFrameAndImage that = ( de.embl.cba.tables.imagesegment.LabelFrameAndImage ) o;
		return Double.compare( that.label, label ) == 0 &&
				frame == that.frame &&
				Objects.equals( image, that.image );
	}

	@Override
	public int hashCode()
	{
		return Objects.hash( label, image, frame );
	}

	public String getImage()
	{
		return image;
	}

	public double getLabel()
	{
		return label;
	}

	public int getFrame()
	{
		return frame;
	}
}
