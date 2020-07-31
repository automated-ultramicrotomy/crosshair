package de.embl.cba.crosshair.utils;

import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import bdv.viewer.animate.AbstractTransformAnimator;
import bdv.viewer.animate.SimilarityTransformAnimator;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

import java.util.ArrayList;
import java.util.stream.DoubleStream;

import static de.embl.cba.crosshair.utils.GeometryUtils.quaternionToAffineTransform3D;

public class BdvUtils {

    // from MOBIE
    public static void moveToPosition(Bdv bdv, double[] xyz, long durationMillis )
    {

        final AffineTransform3D currentViewerTransform = new AffineTransform3D();
        bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentViewerTransform );

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

        newViewerTransform.translate( getBdvWindowCentre( bdv ) );

        if ( durationMillis <= 0 )
        {
            bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( newViewerTransform );
            return;
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
            bdv.getBdvHandle().getViewerPanel().transformChanged( currentViewerTransform );
        }
    }


    // from MOBIE
    public static void levelCurrentView( Bdv bdv, double[] targetNormalVector )
    {

        double[] currentNormalVector = BdvUtils.getCurrentViewNormalVector( bdv );

        AffineTransform3D currentViewerTransform = new AffineTransform3D();
        bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentViewerTransform );

        LinAlgHelpers.normalize( targetNormalVector ); // just to be sure.

        // determine rotation axis
        double[] rotationAxis = new double[ 3 ];
        LinAlgHelpers.cross( currentNormalVector, targetNormalVector, rotationAxis );
        if ( LinAlgHelpers.length( rotationAxis ) > 0 ) LinAlgHelpers.normalize( rotationAxis );

        // The rotation axis is in the coordinate system of the original data set => transform to viewer coordinate system
        double[] qCurrentRotation = new double[ 4 ];
        Affine3DHelpers.extractRotation( currentViewerTransform, qCurrentRotation );
        final AffineTransform3D currentRotation = quaternionToAffineTransform3D( qCurrentRotation );

        double[] rotationAxisInViewerSystem = new double[ 3 ];
        currentRotation.apply( rotationAxis, rotationAxisInViewerSystem );

        // determine rotation angle
        double angle = - Math.acos( LinAlgHelpers.dot( currentNormalVector, targetNormalVector ) );

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

    // from Christian Tischer BdvUtils
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

    // from Christian Tischer BdvUtils
    public static double[] getCurrentViewNormalVector( Bdv bdv )
    {
        AffineTransform3D currentViewerTransform = new AffineTransform3D();
        bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentViewerTransform );

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

    // from Christian Tischer BdvUtils
    public static void changeBdvViewerTransform(
            Bdv bdv,
            ArrayList< AffineTransform3D > transforms,
            long duration)
    {

        AffineTransform3D currentTransform = new AffineTransform3D();
        bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentTransform );

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
}
