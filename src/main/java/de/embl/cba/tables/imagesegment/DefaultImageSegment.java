package de.embl.cba.tables.imagesegment;

import de.embl.cba.tables.imagesegment.ImageSegment;
import net.imglib2.FinalRealInterval;

public class DefaultImageSegment implements ImageSegment
{
	private final double[] position;
	private final String imageId;
	private final double labelId;
	private final int timePoint;

	private float[] mesh;
	private FinalRealInterval boundingBox;

	public DefaultImageSegment(
			String imageId,
			double labelId,
			int timePoint,
			double x,
			double y,
			double z,
			FinalRealInterval boundingBox )
	{
		this.imageId = imageId;
		this.labelId = labelId;
		this.timePoint = timePoint;
		this.position = new double[]{ x, y, z };
		this.boundingBox = boundingBox;
	}

	@Override
	public String imageId()
	{
		return imageId;
	}

	@Override
	public double labelId()
	{
		return labelId;
	}

	@Override
	public int timePoint()
	{
		return timePoint;
	}

	@Override
	public FinalRealInterval boundingBox()
	{
		return boundingBox;
	}

	@Override
	public void setBoundingBox( FinalRealInterval boundingBox )
	{
		this.boundingBox = boundingBox;
	}

	@Override
	public float[] getMesh()
	{
		return mesh;
	}

	@Override
	public void setMesh( float[] mesh )
	{
		this.mesh = mesh;
	}

	@Override
	public void localize( float[] position )
	{
		for ( int d = 0; d < position.length; d++ )
		{
			position[ d ] = (float) this.position[ d ];
		}
	}

	@Override
	public void localize( double[] position )
	{
		for ( int d = 0; d < position.length; d++ )
		{
			position[ d ] = this.position[ d ];
		}
	}

	@Override
	public float getFloatPosition( int d )
	{
		return (float) position[ d ];
	}

	@Override
	public double getDoublePosition( int d )
	{
		return position[ d ];
	}

	@Override
	public int numDimensions()
	{
		return position.length;
	}

}
