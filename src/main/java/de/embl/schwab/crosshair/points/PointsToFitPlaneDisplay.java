package de.embl.schwab.crosshair.points;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import de.embl.schwab.crosshair.points.overlays.Point3dOverlay;
import de.embl.schwab.crosshair.points.overlays.PointsToFitPlane2dOverlay;
import ij3d.Content;
import net.imglib2.RealPoint;

import java.util.ArrayList;

import static de.embl.schwab.crosshair.points.PointHelper.getCurrentMousePosition;
import static de.embl.schwab.crosshair.points.PointHelper.getMatchingPointWithinDistance;

/**
 * Class to control 2D and 3D display of points used to fit a specific plane
 * Each plane will get its own PointsToFitPlaneDisplay.
 */
public class PointsToFitPlaneDisplay {

    private final ArrayList<RealPoint> pointsToFitPlane; // points used to fit this plane
    private final Bdv bdv;
    private final Point3dOverlay point3dOverlay;
    private final PointsToFitPlane2dOverlay point2dOverlay;
    private final String sourceName;

    /**
     * Create a points to fit plane display (starting with no points)
     * @param name Plane name
     * @param bdv BigDataViewer window to show 2D points on
     * @param imageContent image content (displayed in 3D viewer) to show 3D points on
     */
    public PointsToFitPlaneDisplay( String name, Bdv bdv, Content imageContent ) {
        this( new ArrayList<>(), name, bdv, imageContent );
    }

    /**
     * Create a points to fit plane display (starting with list of points)
     * @param pointsToFitPlane List of points to fit plane
     * @param name Plane name
     * @param bdv BigDataViewer window to show 2D points on
     * @param imageContent image content (displayed in 3D viewer) to show 3D points on
     */
    public PointsToFitPlaneDisplay( ArrayList<RealPoint> pointsToFitPlane, String name, Bdv bdv, Content imageContent ) {
        this.pointsToFitPlane = pointsToFitPlane;
        this.point2dOverlay = new PointsToFitPlane2dOverlay( this );
        this.point3dOverlay = new Point3dOverlay( imageContent );
        this.bdv = bdv;
        this.sourceName = name + "-points_to_fit_plane";
        BdvFunctions.showOverlay( point2dOverlay, sourceName,
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

    public String getSourceName() {
        return sourceName;
    }

    /**
     * Add point at current mouse position in BigDataViewer window. If there's already a point there, then
     * remove it instead.
     */
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
        point3dOverlay.removePoints( pointsToFitPlane );
        pointsToFitPlane.clear();
        bdv.getBdvHandle().getViewerPanel().requestRepaint();
    }
}
