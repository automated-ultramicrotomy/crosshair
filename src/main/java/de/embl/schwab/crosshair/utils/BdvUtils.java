package de.embl.schwab.crosshair.utils;

import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import bdv.viewer.Source;
import de.embl.cba.tables.Utils;
import de.embl.cba.util.CopyUtils;
import ij.ImagePlus;
import ij.Prefs;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealUnsignedByteConverter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.Views;
import org.apache.commons.math3.util.Precision;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.stream.DoubleStream;

import static de.embl.cba.bdv.utils.BdvUtils.*;
import static de.embl.cba.tables.Utils.getVoxelSpacings;
import static de.embl.cba.tables.ij3d.UniverseUtils.logVoxelSpacing;
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
        final Integer level = Utils.getLevel( source, maxNumVoxels );
        if ( level == null )
        {
            throw new UnsupportedOperationException(
                    "Image is too large to be displayed in 3D - for hdf5/n5/zarr, try adding a lower resolution level."
            );
        }

        logVoxelSpacing( source, getVoxelSpacings( source ).get( level ) );
        final ImagePlus wrap = getImagePlus( source, min, max, level );
        return universe.addContent( wrap, displayType );
    }

    private static < R extends RealType< R > > ImagePlus getImagePlus( Source< ? > source, int min, int max, Integer level )
    {
        RandomAccessibleInterval< ? extends RealType< ? > > rai
                = de.embl.cba.bdv.utils.BdvUtils.getRealTypeNonVolatileRandomAccessibleInterval( source, 0, level );

        rai = CopyUtils.copyVolumeRaiMultiThreaded( ( RandomAccessibleInterval ) rai, Prefs.getThreads() -1  ); // TODO: make multi-threading configurable.

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
}
