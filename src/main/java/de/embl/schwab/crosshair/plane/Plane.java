package de.embl.schwab.crosshair.plane;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import de.embl.schwab.crosshair.points.PointsToFitPlaneDisplay;
import de.embl.schwab.crosshair.points.overlays.Point3dOverlay;
import de.embl.schwab.crosshair.points.overlays.PointsToFitPlane2dOverlay;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij3d.Content;
import net.imglib2.RealPoint;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3d;

import java.awt.*;
import java.util.ArrayList;

import static de.embl.schwab.crosshair.points.PointHelper.*;

public class Plane {

    private String name;

    private Vector3d normal;
    private Vector3d point;

    private transient Vector3d centroid;

    private Color3f color;
    private float transparency;
    private boolean isVisible;

    // visualisation
    private transient Content mesh; // the 3d custom triangle mesh representing the plane
    private transient PointsToFitPlaneDisplay pointsToFitPlaneDisplay;

    private double distanceBetweenPlanesThreshold = 1E-10; // distance used to be 'on' plane

    public Plane( PlaneSettings planeSettings, Vector3d centroid, Content mesh, PointsToFitPlaneDisplay pointsToFitPlaneDisplay ) {
        this.name = planeSettings.name;
        this.normal = planeSettings.normal;
        this.point = planeSettings.point;
        this.centroid = centroid;

        this.transparency = planeSettings.transparency;
        this.isVisible = planeSettings.isVisible;
        this.mesh = mesh;
        this.color = planeSettings.color;

        this.pointsToFitPlaneDisplay = pointsToFitPlaneDisplay;
    }

    public void updatePlaneOrientation(Vector3d normal, Vector3d point, Vector3d centroid, Content mesh ) {
        this.normal = normal;
        this.point = point;
        this.centroid = centroid;
        this.mesh = mesh;
    }

    public String getName() {
        return name;
    }

    public Boolean isVisible() { return this.isVisible; }

    public void setVisible(Boolean visible) {
        this.isVisible = visible;
        if ( mesh != null ) {
            mesh.setVisible(visible);
        }
    }

    public void toggleVisible() {
        setVisible( !isVisible );
    }

    public Color3f getColor() {
        return this.color;
    }

    public void setColor( Color color ) {
        this.color = new Color3f(color);
        if ( mesh != null ) {
            // make copy of colour to assign (using original interferes with changing colour later)
            mesh.setColor( this.color );
        }
    }

    public Float getTransparency() {
        return this.transparency;
    }

    public void setTransparency( Float transparency ) {
        this.transparency = transparency;
        if ( mesh != null ) {
            mesh.setTransparency(transparency);
        }
    }

    public Vector3d getCentroid() {
        return centroid;
    }

    public Vector3d getNormal() {
        return normal;
    }

    public Vector3d getPoint() {
        return point;
    }

    public PointsToFitPlaneDisplay getPointsToFitPlaneDisplay() {
        return pointsToFitPlaneDisplay;
    }

    // public ArrayList<RealPoint> getPointsToFitPlane() {
    //     return pointsToFitPlane;
    // }

    public double getDistanceBetweenPlanesThreshold() {
        return distanceBetweenPlanesThreshold;
    }

    // public void addOrRemoveCurrentPositionFromPointsToFitPlane() {
    //     RealPoint point = getCurrentMousePosition( bdv.getBdvHandle() );
    //
    //     // remove point if within a certain distance of an existing point, otherwise add point
    //     RealPoint matchingPointWithinDistance = getMatchingPointWithinDistance( pointsToFitPlane, point, bdv.getBdvHandle());
    //
    //     if ( matchingPointWithinDistance != null ) {
    //         removePointToFitPlane( matchingPointWithinDistance );
    //     } else {
    //         addPointToFitPlane( point );
    //     }
    // }
    //
    // public void addPointToFitPlane( RealPoint point ) {
    //     pointsToFitPlane.add( point );
    //     point3dOverlay.addPoint( point );
    //     bdv.getBdvHandle().getViewerPanel().requestRepaint();
    // }
    //
    // public void removePointToFitPlane( RealPoint point ) {
    //     point3dOverlay.removePoint( point );
    //     pointsToFitPlane.remove( point );
    //     bdv.getBdvHandle().getViewerPanel().requestRepaint();
    // }
    //
    // public void removeAllPointsToFitPlane() {
    //     for ( RealPoint point : pointsToFitPlane ) {
    //         point3dOverlay.removePoint(point);
    //     }
    //     pointsToFitPlane.clear();
    //     bdv.getBdvHandle().getViewerPanel().requestRepaint();
    // }

    public void setDistanceBetweenPlanesThreshold( double distanceBetweenPlanesThreshold ) {
        this.distanceBetweenPlanesThreshold = distanceBetweenPlanesThreshold;
    }

    public boolean isPointOnPlane( RealPoint point ) {

        double[] position = new double[3];
        point.localize( position );

        double distanceToPlane = GeometryUtils.distanceFromPointToPlane( new Vector3d( position ),
                getNormal(), getPoint() );

        // units may want to be more or less strict
        if (distanceToPlane < distanceBetweenPlanesThreshold) {
            return true;
        } else {
            return false;
        }
    }

    // public PointsToFitPlane2dOverlay getPointsToFitPlane2dOverlay() {
    //     return pointsToFitPlane2dOverlay;
    // }

    public PlaneSettings getSettings() {
        return new PlaneSettings( this );
    }
}
