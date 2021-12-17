package de.embl.schwab.crosshair.utils;

import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import org.apache.commons.math3.util.Precision;

import java.util.ArrayList;
import java.util.stream.DoubleStream;

import static de.embl.cba.bdv.utils.BdvUtils.*;
import static de.embl.schwab.crosshair.utils.GeometryUtils.findPerpendicularVector;

public class BdvUtils {

    public static void flipCurrentView( Bdv bdv ) {
        double[] currentNormalVector = getCurrentViewNormalVector( bdv );
        for ( int i=0; i<currentNormalVector.length; i++) {
            currentNormalVector[i] = currentNormalVector[i]*-1;
        }
        levelCurrentView( bdv, currentNormalVector );
    }

    // from https://github.com/tischi/imagej-utils/blob/f9b84aae6b3bd922ed723fd6b24ac510f86af8eb/src/main/java/de/embl/cba/bdv/utils/BdvUtils.java#L422
    // TODO - I change the duration of the move - ask T for an option to do this, then can remove and use one from imagej-utils
    public static void levelCurrentView( Bdv bdv, double[] targetNormalVector )
    {

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
}
