package de.embl.schwab.crosshair.utils;

import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import de.embl.schwab.crosshair.plane.Plane;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import org.apache.commons.math3.util.Precision;
import org.scijava.vecmath.Vector3d;

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

    public static void shiftCurrentView( Bdv bdv, Plane plane, Vector3d blockNormal ) {
        // shift view to a set number of microns below given plane (parallel to plane, but in the same general
        // direction as block normal - i.e, moving further into the block, away from the surface)
        int nMicrons = 3;

        final AffineTransform3D transform = new AffineTransform3D();
        bdv.getBdvHandle().getViewerPanel().state().getViewerTransform( transform );
        ArrayList<Vector3d> planeDefinition = GeometryUtils.getPlaneDefinitionFromViewTransform(transform);

        Vector3d currentPlaneNormal = planeDefinition.get(0);
        boolean normalsParallel = GeometryUtils.checkVectorsParallel( plane.getNormal(), currentPlaneNormal );

        double[] targetNormal = new double[3];
        plane.getNormal().get( targetNormal );

        double[] targetCentroid = new double[3];
        plane.getCentroid().get( targetCentroid );

        Vector3d shiftedCentroid = new Vector3d( plane.getCentroid() );
        Vector3d shiftedNormal = new Vector3d( plane.getNormal() );
        shiftedNormal.normalize();
        shiftedNormal.scale(nMicrons);

        // shift in same general direction as given block normal
        if ( blockNormal.dot( plane.getNormal() ) < 0 ) {
            shiftedNormal.scale(-1);
        }

        shiftedCentroid.add( shiftedNormal );
        double[] shiftedCentroidArray = new double[3];
        shiftedCentroid.get(shiftedCentroidArray);

        moveToPosition(bdv, shiftedCentroidArray, 0, 0);
        if (!normalsParallel) {
            BdvUtils.levelCurrentView(bdv, targetNormal);
        }

        Vector3d difference = new Vector3d(shiftedCentroid);
        difference.sub(plane.getCentroid());
        IJ.log("moved " + difference.length());
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
