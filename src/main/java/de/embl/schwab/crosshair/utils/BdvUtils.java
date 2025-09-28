/*
 * A number of functions in this file are modified from the imagej-utils repository -
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

import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import bdv.viewer.Source;
import bdv.viewer.animate.AbstractTransformAnimator;
import bdv.viewer.animate.SimilarityTransformAnimator;
import ij.ImagePlus;
import ij.Prefs;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.*;
import net.imglib2.converter.RealUnsignedByteConverter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.Views;
import org.apache.commons.math3.util.Precision;
import org.slf4j.LoggerFactory;
import net.imglib2.type.NativeType;
import net.imglib2.img.AbstractImg;
import net.imglib2.util.Intervals;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.util.Util;
import net.imglib2.algorithm.util.Grids;
import java.util.ArrayList;
import java.util.stream.DoubleStream;
import java.util.Arrays;
import java.util.stream.Collectors;

import static de.embl.schwab.crosshair.utils.GeometryUtils.findPerpendicularVector;

public class BdvUtils {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BdvUtils.class);

    /**
     * Flip the orientation of the current view in BigDataViewer (bdv)
     * @param bdv bdv window
     */
    public static void flipCurrentView( Bdv bdv ) {
        double[] currentNormalVector = getCurrentViewNormalVector( bdv );
        for ( int i=0; i<currentNormalVector.length; i++) {
            currentNormalVector[i] = currentNormalVector[i]*-1;
        }
        levelCurrentView( bdv, currentNormalVector );
    }

    /**
     * Level the current BigDataViewer view to match the given target normal.
     * from https://github.com/tischi/imagej-utils/blob/f9b84aae6b3bd922ed723fd6b24ac510f86af8eb/src/main/java/de/embl/cba/bdv/utils/BdvUtils.java#L422
     *
     * @param bdv BigDataViewer window
     * @param targetNormalVector target normal vector
     */
    public static void levelCurrentView( Bdv bdv, double[] targetNormalVector )
    {
        // TODO - I changed the duration of the move - ask T for an option to do this, then can remove and use one from
        //  imagej-utils
        double epsilon = 0.000000001; // epsilon for double comparisons
        double[] currentNormalVector = getCurrentViewNormalVector( bdv );

        AffineTransform3D currentViewerTransform = new AffineTransform3D();
        bdv.getBdvHandle().getViewerPanel().state().getViewerTransform( currentViewerTransform );

        LinAlgHelpers.normalize( targetNormalVector ); // just to be sure.

        // determine rotation angle
        double angle = - Math.acos( LinAlgHelpers.dot( currentNormalVector, targetNormalVector ) );

        // determine rotation axis
        double[] rotationAxis = new double[ 3 ];
        LinAlgHelpers.cross( currentNormalVector, targetNormalVector, rotationAxis );
        if ( !Precision.equals( LinAlgHelpers.length( rotationAxis ), 0, epsilon ) ) {
            LinAlgHelpers.normalize( rotationAxis );
        } else {
            if ( angle == 0 ){
                // already at that normal
                return;
            } else {
                // find a possible rotation axis when vectors are at 180 degrees to each other
                rotationAxis = findPerpendicularVector( currentNormalVector );
            }
        }

        // The rotation axis is in the coordinate system of the original data set => transform to viewer coordinate system
        double[] qCurrentRotation = new double[ 4 ];
        Affine3DHelpers.extractRotation( currentViewerTransform, qCurrentRotation );
        final AffineTransform3D currentRotation = quaternionToAffineTransform3D( qCurrentRotation );

        double[] rotationAxisInViewerSystem = new double[ 3 ];
        currentRotation.apply( rotationAxis, rotationAxisInViewerSystem );

        // construct rotation of angle around axis
        double[] rotationQuaternion = new double[ 4 ];
        LinAlgHelpers.quaternionFromAngleAxis( rotationAxisInViewerSystem, angle, rotationQuaternion );
        final AffineTransform3D rotation = quaternionToAffineTransform3D( rotationQuaternion );

        // apply transformation (rotating around current viewer centre position)
        final AffineTransform3D translateCenterToOrigin = new AffineTransform3D();
        translateCenterToOrigin.translate( DoubleStream.of( getBdvWindowCentre( bdv )).map(x -> -x ).toArray() );

        final AffineTransform3D translateCenterBack = new AffineTransform3D();
        translateCenterBack.translate( getBdvWindowCentre( bdv ) );

        ArrayList< AffineTransform3D > viewerTransforms = new ArrayList<>(  );

        viewerTransforms.add( currentViewerTransform.copy()
                .preConcatenate( translateCenterToOrigin )
                .preConcatenate( rotation )
                .preConcatenate( translateCenterBack ) );

        changeBdvViewerTransform( bdv, viewerTransforms, 500 );

    }

    /**
     * Based on addSourceToUniverse from imagej-utils UniverseUtils, modifying it to
     * only add the image source to the 3D viewer - no transparency / colour adjustments.
     * @param universe universe of the 3D viewer
     * @param source image source
     * @param maxNumVoxels maximum number of voxels
     * @param displayType e.g. Content.VOLUME, Content.SURFACE...
     * @param min minimum pixel value in image
     * @param max maximum pixel value in image
     * @return image content displayed in the 3D viewer
     */
    public static < R extends RealType< R >> Content addSourceToUniverse(
            Image3DUniverse universe,
            Source< ? > source,
            long maxNumVoxels,
            int displayType,
            int min,
            int max )
    {
        final Integer level = getLevel( source, maxNumVoxels );
        logVoxelSpacing( source, getVoxelSpacings( source ).get( level ) );

        if ( level == null )
        {
            logger.warn( "Image is too large to be displayed in 3D." );
            return null;
        }

        final ImagePlus wrap = getImagePlus( source, min, max, level );
        return universe.addContent( wrap, displayType );
    }

    private static < R extends RealType< R > > ImagePlus getImagePlus( Source< ? > source, int min, int max, Integer level )
    {
        RandomAccessibleInterval< ? extends RealType< ? > > rai
                = getRealTypeNonVolatileRandomAccessibleInterval( source, 0, level );

        rai = copyVolumeRaiMultiThreaded( ( RandomAccessibleInterval ) rai, Prefs.getThreads() -1  ); // TODO: make multi-threading configurable.

        rai = Views.permute( Views.addDimension( rai, 0, 0 ), 2, 3 );

        final ImagePlus wrap = ImageJFunctions.wrapUnsignedByte(
                ( RandomAccessibleInterval ) rai,
                new RealUnsignedByteConverter< R >( min, max ),
                source.getName() );

        final double[] voxelSpacing = getVoxelSpacings( source ).get( level );
        wrap.getCalibration().pixelWidth = voxelSpacing[ 0 ];
        wrap.getCalibration().pixelHeight = voxelSpacing[ 1 ];
        wrap.getCalibration().pixelDepth = voxelSpacing[ 2 ];

        return wrap;
    }

    public static void logVoxelSpacing( Source< ? > source, double[] voxelSpacings )
    {
        String message = "3D View: Fetching source " + source.getName() + " at resolution " +
                Arrays.stream( voxelSpacings ).mapToObj( x -> "" + x ).collect( Collectors.joining( " ," ) ) +
                " micrometer...";
        logger.info(message);
    }

    public static ArrayList< double[] > getVoxelSpacings( Source< ? > labelsSource )
    {
        final ArrayList< double[] > voxelSpacings = new ArrayList<>();
        final int numMipmapLevels = labelsSource.getNumMipmapLevels();
        for ( int level = 0; level < numMipmapLevels; ++level )
            voxelSpacings.add( BdvUtils.getCalibration( labelsSource, level ) );

        return voxelSpacings;
    }

    public static double[] getCalibration( Source source, int level )
    {
        final AffineTransform3D sourceTransform = new AffineTransform3D();
        source.getSourceTransform( 0, level, sourceTransform );

        final double[] calibration = getScale( sourceTransform );
        return calibration;
    }

    public static double[] getScale( AffineTransform3D sourceTransform )
    {
        // https://math.stackexchange.com/questions/237369/given-this-transformation-matrix-how-do-i-decompose-it-into-translation-rotati

        final double[] calibration = new double[ 3 ];
        for ( int d = 0; d < 3; ++d )
        {
            final double[] vector = new double[ 3 ];
            for ( int i = 0; i < 3 ; i++ )
            {
                vector[ i ] = sourceTransform.get( d, i );
            }

            calibration[ d ] = LinAlgHelpers.length( vector );
        }
        return calibration;
    }

    public static < R extends RealType< R > & NativeType< R > >
    RandomAccessibleInterval< R > copyVolumeRaiMultiThreaded( RandomAccessibleInterval< R > volume, int numThreads ) {
        final int dimensionX = ( int ) volume.dimension( 0 );
        final int dimensionY = ( int ) volume.dimension( 1 );
        final int dimensionZ = ( int ) volume.dimension( 2 );

        final long numElements =
                AbstractImg.numElements( Intervals.dimensionsAsLongArray( volume ) );

        RandomAccessibleInterval< R > copy;

        if ( numElements < Integer.MAX_VALUE - 1 )
        {
            copy = new ArrayImgFactory( Util.getTypeFromInterval( volume ) ).create( volume );
        }
        else
        {
            int cellSizeZ = (int) ( ( Integer.MAX_VALUE - 1 )
                    / ( volume.dimension( 0  ) * volume.dimension( 1 ) ) );

            final int[] cellSize = {
                    dimensionX,
                    dimensionY,
                    cellSizeZ };

            copy = new CellImgFactory( Util.getTypeFromInterval( volume ), cellSize ).create( volume );
        }

        final int[] blockSize = {
                dimensionX,
                dimensionY,
                ( int ) Math.ceil( 1.0 * dimensionZ / numThreads ) };

        Grids.collectAllContainedIntervals(
                        Intervals.dimensionsAsLongArray( volume ) , blockSize )
                .parallelStream().forEach(
                        interval -> copy( volume, Views.interval( copy, interval )));

        return copy;
    }

    private static < T extends Type< T >> void copy(final RandomAccessible< T > source,
                                                    final IterableInterval< T > target )
    {
        // create a cursor that automatically localizes itself on every move
        Cursor< T > targetCursor = target.localizingCursor();
        RandomAccess< T > sourceRandomAccess = source.randomAccess();

        // iterate over the input cursor
        while ( targetCursor.hasNext() )
        {
            // move input cursor forward
            targetCursor.fwd();

            // set the output cursor to the position of the input cursor
            sourceRandomAccess.setPosition( targetCursor );

            // set the value of this pixel of the output image, every Type supports T.set( T type )
            targetCursor.get().set( sourceRandomAccess.get() );
        }
    }

    public static Integer getLevel( Source< ? > source, long maxNumVoxels )
    {
        final ArrayList< double[] > voxelSpacings = getVoxelSpacings( source );

        for ( int level = 0; level < voxelSpacings.size(); level++ )
        {
            final long numElements = Intervals.numElements( source.getSource( 0, level ) );

            if ( numElements <= maxNumVoxels )
                return level;
        }
        return null;
    }

    public static double[] getCurrentViewNormalVector( Bdv bdv )
    {
        AffineTransform3D currentViewerTransform = new AffineTransform3D();
        bdv.getBdvHandle().getViewerPanel().state().getViewerTransform( currentViewerTransform );

        final double[] viewerC = new double[]{ 0, 0, 0 };
        final double[] viewerX = new double[]{ 1, 0, 0 };
        final double[] viewerY = new double[]{ 0, 1, 0 };

        final double[] dataC = new double[ 3 ];
        final double[] dataX = new double[ 3 ];
        final double[] dataY = new double[ 3 ];

        final double[] dataV1 = new double[ 3 ];
        final double[] dataV2 = new double[ 3 ];
        final double[] currentNormalVector = new double[ 3 ];

        currentViewerTransform.inverse().apply( viewerC, dataC );
        currentViewerTransform.inverse().apply( viewerX, dataX );
        currentViewerTransform.inverse().apply( viewerY, dataY );

        LinAlgHelpers.subtract( dataX, dataC, dataV1 );
        LinAlgHelpers.subtract( dataY, dataC, dataV2 );

        LinAlgHelpers.cross( dataV1, dataV2, currentNormalVector );

        LinAlgHelpers.normalize( currentNormalVector );

        return currentNormalVector;
    }

    public static AffineTransform3D quaternionToAffineTransform3D( double[] rotationQuaternion )
    {
        double[][] rotationMatrix = new double[ 3 ][ 3 ];
        LinAlgHelpers.quaternionToR( rotationQuaternion, rotationMatrix );
        return matrixAsAffineTransform3D( rotationMatrix );
    }

    public static AffineTransform3D matrixAsAffineTransform3D( double[][] rotationMatrix )
    {
        final AffineTransform3D rotation = new AffineTransform3D();
        for ( int row = 0; row < 3; ++row )
            for ( int col = 0; col < 3; ++ col)
                rotation.set( rotationMatrix[ row ][ col ], row, col);
        return rotation;
    }

    public static double[] getBdvWindowCentre( Bdv bdv )
    {
        int[] bdvWindowDimensions = new int[ 3 ];
        bdvWindowDimensions[ 0 ] = bdv.getBdvHandle().getViewerPanel().getWidth();
        bdvWindowDimensions[ 1 ] = bdv.getBdvHandle().getViewerPanel().getHeight();

        double[] centerBdvWindowTranslation = new double[ 3 ];
        for( int d = 0; d < 3; ++d )
        {
            centerBdvWindowTranslation[ d ] = + bdvWindowDimensions[ d ] / 2.0;
        }
        return centerBdvWindowTranslation;
    }

    public static double[] getBdvWindowCenter( Bdv bdv )
    {
        final double[] centre = new double[ 3 ];

        centre[ 0 ] = bdv.getBdvHandle().getViewerPanel().getDisplay().getWidth() / 2.0;
        centre[ 1 ] = bdv.getBdvHandle().getViewerPanel().getDisplay().getHeight() / 2.0;

        return centre;
    }

    public static void changeBdvViewerTransform(
            Bdv bdv,
            ArrayList< AffineTransform3D > transforms,
            long duration)
    {

        AffineTransform3D currentTransform = new AffineTransform3D();
        bdv.getBdvHandle().getViewerPanel().state().getViewerTransform( currentTransform );

        ArrayList< SimilarityTransformAnimator > animators = new ArrayList<>(  );

        final SimilarityTransformAnimator firstAnimator =
                new SimilarityTransformAnimator(
                        currentTransform.copy(),
                        transforms.get( 0 ).copy(),
                        0 ,
                        0,
                        duration );

        animators.add( firstAnimator );

        for ( int i = 1; i < transforms.size(); i++ )
        {
            final SimilarityTransformAnimator animator =
                    new SimilarityTransformAnimator(
                            transforms.get( i - 1 ).copy(),
                            transforms.get( i ).copy(),
                            0 ,
                            0,
                            duration );

            animators.add( animator );
        }


        AbstractTransformAnimator transformAnimator = new ConcatenatedTransformAnimator( duration, animators );

        bdv.getBdvHandle().getViewerPanel().setTransformAnimator( transformAnimator );
        //bdv.getBdvHandle().getViewerPanel().transformChanged( currentTransform.copy() );

    }

    public static void moveToPosition( Bdv bdv, double[] xyz, int t, long durationMillis )
    {
        if ( t != bdv.getBdvHandle().getViewerPanel().state().getCurrentTimepoint() )
        {
            bdv.getBdvHandle().getViewerPanel().state().setCurrentTimepoint( t );
            durationMillis = 0; // otherwise there can be hickups when changing both the viewer transform and the timepoint
        }

        final AffineTransform3D currentViewerTransform = new AffineTransform3D();
        bdv.getBdvHandle().getViewerPanel().state().getViewerTransform( currentViewerTransform );

        AffineTransform3D newViewerTransform = currentViewerTransform.copy();

        // ViewerTransform
        // applyInverse: coordinates in viewer => coordinates in image
        // apply: coordinates in image => coordinates in viewer

        final double[] locationOfTargetCoordinatesInCurrentViewer = new double[ 3 ];
        currentViewerTransform.apply( xyz, locationOfTargetCoordinatesInCurrentViewer );

        for ( int d = 0; d < 3; d++ )
        {
            locationOfTargetCoordinatesInCurrentViewer[ d ] *= -1;
        }

        newViewerTransform.translate( locationOfTargetCoordinatesInCurrentViewer );

        newViewerTransform.translate( getBdvWindowCenter( bdv ) );

        if ( durationMillis <= 0 )
        {
            bdv.getBdvHandle().getViewerPanel().state().setViewerTransform(  newViewerTransform );
        }
        else
        {
            final SimilarityTransformAnimator similarityTransformAnimator =
                    new SimilarityTransformAnimator(
                            currentViewerTransform,
                            newViewerTransform,
                            0,
                            0,
                            durationMillis );

            bdv.getBdvHandle().getViewerPanel().setTransformAnimator( similarityTransformAnimator );
        }
    }

    public static RandomAccessibleInterval< ? extends RealType< ? > >
    getRealTypeNonVolatileRandomAccessibleInterval( Source source, int t, int level )
    {
        if ( source instanceof LazySpimSource ) {
            return ((LazySpimSource) source).getNonVolatileSource(t, level);
        } else {
            throw new UnsupportedOperationException("Unsupported source type");
        }
    }

}
