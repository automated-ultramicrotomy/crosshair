package de.embl.schwab.crosshair.points;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import de.embl.schwab.crosshair.points.overlays.Point3dOverlay;
import de.embl.schwab.crosshair.points.overlays.PointsToFitPlane2dOverlay;
import net.imglib2.RealPoint;

import java.util.ArrayList;

import static de.embl.schwab.crosshair.points.PointHelper.getCurrentMousePosition;
import static de.embl.schwab.crosshair.points.PointHelper.getMatchingPointWithinDistance;

public class PointsToFitPlaneDisplay {

    private final ArrayList<RealPoint> pointsToFitPlane; // points used to fit this plane
    private Bdv bdv;
    private Point3dOverlay point3dOverlay;
    private PointsToFitPlane2dOverlay point2dOverlay;

    public PointsToFitPlaneDisplay( String name, Bdv bdv, Point3dOverlay point3dOverlay ) {
        this( new ArrayList<>(), name, bdv, point3dOverlay );
    }

    public PointsToFitPlaneDisplay( ArrayList<RealPoint> pointsToFitPlane, String name, Bdv bdv, Point3dOverlay point3dOverlay ) {
        this.pointsToFitPlane = pointsToFitPlane;
        this.point2dOverlay = new PointsToFitPlane2dOverlay( this );
        this.point3dOverlay = point3dOverlay;
        this.bdv = bdv;
        BdvFunctions.showOverlay( point2dOverlay, name + "-points_to_fit_plane",
                Bdv.options().addTo(bdv) );

        for ( RealPoint point: pointsToFitPlane ) {
            point3dOverlay.addPoint( point );
        }
    }

    public ArrayList<RealPoint> getPointsToFitPlane() {
        return pointsToFitPlane;
    }

    public PointsToFitPlane2dOverlay getPoint2dOverlay() {
        return point2dOverlay;
    }

    public void addOrRemoveCurrentPositionFromPointsToFitPlane() {
        RealPoint point = getCurrentMousePosition( bdv.getBdvHandle() );

        // remove point if within a certain distance of an existing point, otherwise add point
        RealPoint matchingPointWithinDistance = getMatchingPointWithinDistance( pointsToFitPlane, point, bdv.getBdvHandle());

        if ( matchingPointWithinDistance != null ) {
            removePointToFitPlane( matchingPointWithinDistance );
        } else {
            addPointToFitPlane( point );
        }
    }

    public void addPointToFitPlane( RealPoint point ) {
        pointsToFitPlane.add( point );
        point3dOverlay.addPoint( point );
        bdv.getBdvHandle().getViewerPanel().requestRepaint();
    }

    public void removePointToFitPlane( RealPoint point ) {
        point3dOverlay.removePoint( point );
        pointsToFitPlane.remove( point );
        bdv.getBdvHandle().getViewerPanel().requestRepaint();
    }

    public void removeAllPointsToFitPlane() {
        for ( RealPoint point : pointsToFitPlane ) {
            point3dOverlay.removePoint(point);
        }
        pointsToFitPlane.clear();
        bdv.getBdvHandle().getViewerPanel().requestRepaint();
    }
}
