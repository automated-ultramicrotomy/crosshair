/*
 * LazySpimSource is adapted from the imagej-utils repository -
 * https://github.com/embl-cba/imagej-utils - released under a BSD 2-Clause license given below:
 *
 * Copyright (c) 2018 - 2024, EMBL
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */

package de.embl.schwab.crosshair.utils;

import bdv.BigDataViewer;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.BasicMultiResolutionImgLoader;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class LazySpimSource< T extends NumericType< T > > implements Source< T >
{
    private static final Logger logger = LoggerFactory.getLogger(LazySpimSource.class);

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
        spimData = openSpimData( path );
        converterSetups = new ArrayList<>();
        sources = new ArrayList<>();
        BigDataViewer.initSetups( spimData, converterSetups, sources );
    }

    private SpimData openSpimData( String path )
    {
        try
        {
            InputStream stream = new FileInputStream( new File( path ) );
            SpimData spimData = new CustomXmlIoSpimData().loadFromStream( stream, path );
            return spimData;
        }
        catch (SpimDataException | IOException e )
        {
            logger.error("Couldn't load data from path", e);
            return null;
        }
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
        return wrappedSource().isPresent( t );
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
