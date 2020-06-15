package de.embl.cba.bdv.utils.sources;

import bdv.BigDataViewer;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.generic.sequence.BasicMultiResolutionImgLoader;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

import java.util.ArrayList;
import java.util.List;

public class LazySpimSource< T extends NumericType< T > > implements Source< T >
{
	private final String path;
	private final String name;
	private Source< T > source;
	private Source< T > volatileSource;
	private SpimData spimData;
	private List< ConverterSetup > converterSetups;
	private List< SourceAndConverter< ? > > sources;

	public LazySpimSource( String name, String path )
	{
		this.name = name;
		this.path = path;
	}

	private Source< T > wrappedVolatileSource()
	{
		if ( spimData == null ) initSpimData();

		if ( volatileSource == null )
			volatileSource = ( Source< T > ) sources.get( 0 ).asVolatile().getSpimSource();

		if ( source == null )
			source = ( Source< T > ) sources.get( 0 ).getSpimSource();

		return volatileSource;
	}

	private Source< T > wrappedSource()
	{
		if ( spimData == null ) initSpimData();

		if ( source == null )
			source = ( Source< T > ) sources.get( 0 ).getSpimSource();

		return source;
	}

	private void initSpimData()
	{
		spimData = BdvUtils.openSpimData( path );
		converterSetups = new ArrayList<>();
		sources = new ArrayList<>();
		BigDataViewer.initSetups( spimData, converterSetups, sources );
	}

	public RandomAccessibleInterval< T > getNonVolatileSource( int t, int level )
	{
		if ( spimData == null ) initSpimData();

		final BasicMultiResolutionImgLoader imgLoader = ( BasicMultiResolutionImgLoader ) spimData.getSequenceDescription().getImgLoader();

		return ( RandomAccessibleInterval ) imgLoader.getSetupImgLoader( 0 ).getImage( t, level );
	}

	public RealRandomAccessible< T > getInterpolatedNonVolatileSource( int t, int level, Interpolation interpolation )
	{
		return wrappedSource().getInterpolatedSource( t, level, interpolation );
	}

	@Override
	public boolean isPresent( int t )
	{
		if ( t == 0 ) return true;
		return false;
	}

	@Override
	public RandomAccessibleInterval< T > getSource( int t, int level )
	{
		return wrappedVolatileSource().getSource( t, level );
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( int t, int level, Interpolation method )
	{
		return wrappedVolatileSource().getInterpolatedSource( t, level, method );
	}

	@Override
	public void getSourceTransform( int t, int level, AffineTransform3D transform )
	{
		wrappedVolatileSource().getSourceTransform( t, level, transform  );
	}

	@Override
	public T getType()
	{
		return wrappedVolatileSource().getType();
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return wrappedVolatileSource().getVoxelDimensions();
	}

	@Override
	public int getNumMipmapLevels()
	{
		return wrappedVolatileSource().getNumMipmapLevels();
	}
}
