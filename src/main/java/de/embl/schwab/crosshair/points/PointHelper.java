package de.embl.schwab.crosshair.points;

import bdv.util.BdvHandle;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.ArrayList;

public class PointHelper {

    /**
     * Get current position of mouse in BigDataViewer window (in global coordinates)
     * @param bdvHandle BigDataViewer handle
     * @return mouse position
     */
    public static RealPoint getCurrentMousePosition ( BdvHandle bdvHandle ) {
        RealPoint point = new RealPoint(3);
        bdvHandle.getViewerPanel().getGlobalMouseCoordinates(point);
        return point;
    }

    /**
     * Get current position of mouse in BigDataViewer window (in viewer coordinates)
     * @param bdvHandle BigDataViewer handle
     * @return mouse position
     */
    public static double[] getCurrentPositionViewerCoordinates ( BdvHandle bdvHandle ) {
        RealPoint point = getCurrentMousePosition( bdvHandle );
        return convertToViewerCoordinates( point, bdvHandle );
    }

    /**
     * Check if the given 'point' is within a small distance of any of the 'points'. If so, return the first matching
     * point (distance between points is checked in viewer coordinates of Bdv)
     * @param points Points to match to
     * @param point Point
     * @param bdvHandle BigDataViewer handle
     * @return Matching point (if any) or null
     */
    public static RealPoint getMatchingPointWithinDistance( ArrayList<RealPoint> points, RealPoint point, BdvHandle bdvHandle ) {
        double[] pointViewerCoords = convertToViewerCoordinates( point, bdvHandle );

        for ( int i = 0; i < points.size(); i++ )
        {
            RealPoint currentPoint = points.get(i);
            double[] currentPointViewerCoords = convertToViewerCoordinates( currentPoint, bdvHandle );
            double distance = GeometryUtils.distanceBetweenPoints(pointViewerCoords, currentPointViewerCoords);
            if (distance < 5) {
                return currentPoint;
            }
        }

        return null;
    }

    /**
     * Convert global coordinates to viewer coordinates (for the given BigDataViewer window)
     * @param point Point to convert
     * @param bdvHandle BigDataViewer handle
     * @return Point in viewer coordinates
     */
    public static double[] convertToViewerCoordinates ( RealPoint point, BdvHandle bdvHandle ) {
        final AffineTransform3D transform = new AffineTransform3D();
        bdvHandle.getViewerPanel().state().getViewerTransform( transform );

        final double[] lPos = new double[ 3 ];
        final double[] gPos = new double[ 3 ];
        // get point position (in microns etc)
        point.localize(lPos);
        // get point position in viewer (I guess in pixel units?), so gpos[2] is the distance in pixels
        // from the current view plane
        transform.apply(lPos, gPos);

        return gPos;
    }

}
