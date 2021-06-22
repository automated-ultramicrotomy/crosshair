package de.embl.schwab.crosshair.plane;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import de.embl.schwab.crosshair.points.Point3dOverlay;
import de.embl.schwab.crosshair.points.PointsToFitPlane2dOverlay;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij.IJ;
import ij3d.Content;
import net.imglib2.RealPoint;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3d;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

import static de.embl.schwab.crosshair.points.PointHelper.*;

public class Plane {

    private String name;

    private Vector3d normal;
    private Vector3d point;

    private Vector3d centroid;

    private Color3f color;
    private float transparency;
    private boolean isVisible;

    // 2d visualisation
    protected Bdv bdv;
    private PointsToFitPlane2dOverlay pointsToFitPlane2dOverlay;

    // 3d visualisation
    private Content mesh; // the 3d custom triangle mesh representing the plane
    protected Point3dOverlay point3dOverlay;

    private final ArrayList<RealPoint> pointsToFitPlane; // points used to fit this plane
    private double distanceBetweenPlanesThreshold = 1E-10; // distance used to be 'on' plane

    private ArrayList<JButton> buttonsAffectedByTracking; // these buttons must be disabled when this plane is tracked

    public Plane( String name, Vector3d normal, Vector3d point, Vector3d centroid, Content mesh, Color3f color,
                  float transparency, boolean isVisible, Bdv bdv, Point3dOverlay point3dOverlay ) {
        this.name = name;
        this.normal = normal;
        this.point = point;
        this.centroid = centroid;

        this.transparency = transparency;
        this.isVisible = isVisible;
        this.mesh = mesh;
        this.color = color;

        this.bdv = bdv;
        this.point3dOverlay = point3dOverlay;

        this.buttonsAffectedByTracking = new ArrayList<>();
        this.pointsToFitPlane = new ArrayList<>();
        this.pointsToFitPlane2dOverlay = new PointsToFitPlane2dOverlay( this );
        BdvFunctions.showOverlay( pointsToFitPlane2dOverlay, name + "-points_to_fit_plane",
                Bdv.options().addTo(bdv) );
    }

    public void updatePlane( Vector3d normal, Vector3d point, Vector3d centroid, Content mesh ) {
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

    public ArrayList<JButton> getButtonsAffectedByTracking() {
        return buttonsAffectedByTracking;
    }

    public void addButtonAffectedByTracking(JButton jButton ) {
        buttonsAffectedByTracking.add( jButton );
    }

    public ArrayList<RealPoint> getPointsToFitPlane() {
        return pointsToFitPlane;
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

    public PointsToFitPlane2dOverlay getPointsToFitPlane2dOverlay() {
        return pointsToFitPlane2dOverlay;
    }
}
