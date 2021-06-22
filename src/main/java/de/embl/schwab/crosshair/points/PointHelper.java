package de.embl.schwab.crosshair.points;

import bdv.util.BdvHandle;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij3d.Content;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import vib.BenesNamedPoint;
import vib.PointList;

import java.util.ArrayList;
import java.util.Iterator;

public class PointHelper {

    public static RealPoint getCurrentMousePosition ( BdvHandle bdvHandle ) {
        RealPoint point = new RealPoint(3);
        bdvHandle.getViewerPanel().getGlobalMouseCoordinates(point);
        return point;
    }

    public static boolean pointIsNearExistingPoint( ArrayList<RealPoint> points, RealPoint point, BdvHandle bdvHandle ) {
        double[] pointViewerCoords = convertToViewerCoordinates( point, bdvHandle );

        for ( int i = 0; i < points.size(); i++ )
        {
            RealPoint currentPoint = points.get(i);
            double[] currentPointViewerCoords = convertToViewerCoordinates( currentPoint, bdvHandle );
            double distance = GeometryUtils.distanceBetweenPoints(pointViewerCoords, currentPointViewerCoords);
            if (distance < 5) {
                return true;
            }
        }

        return false;
    }

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

    public void addOrRemovePointFromPointList( ArrayList<RealPoint> points, RealPoint point, BdvHandle bdvHandle ) {

        // remove point if within a certain distance of an existing point, otherwise add point
        double[] pointViewerCoords = convertToViewerCoordinates( point, bdvHandle );

        boolean distanceMatch = false;
        for ( int i = 0; i < points.size(); i++ )
        {
            RealPoint currentPoint = points.get(i);
            double[] currentPointViewerCoords = convertToViewerCoordinates(currentPoint);
            double distance = GeometryUtils.distanceBetweenPoints(pointViewerCoords, currentPointViewerCoords);
            if (distance < 5) {
                removePointFrom3DViewer(currentPoint);
                // remove matching points from named vertices
                removeMatchingNamedVertices(currentPoint);
                // remove matching points from selected vertices
                removeMatchingSelectdVertices(currentPoint);
                points.remove(i);
                bdvHandle.getViewerPanel().requestRepaint();

                distanceMatch = true;
                break;
            }

        }

        if (!distanceMatch) {
            points.add(point);
            bdvHandle.getViewerPanel().requestRepaint();

            double[] position = new double[3];
            point.localize(position);
            imageContent.getPointList().add("", position[0], position[1], position[2]);
        }

    }

    public static double[] getCurrentPositionViewerCoordinates ( BdvHandle bdvHandle ) {
        RealPoint point = getCurrentMousePosition( bdvHandle );
        return convertToViewerCoordinates( point, bdvHandle );
    }


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
