package de.embl.schwab.crosshair.settings;

import de.embl.schwab.crosshair.plane.Plane;
import net.imglib2.RealPoint;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3d;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Class to hold all settings related to planes (e.g. display settings, normal, point...)
 */
public class PlaneSettings {
    public String name;

    public Vector3d normal;
    public Vector3d point;

    public Color3f color;
    public float transparency;
    public boolean isVisible;

    public ArrayList<RealPoint> pointsToFitPlane; // points used to fit this plane
    public double distanceBetweenPlanesThreshold;  // distance used to be 'on' plane

    public PlaneSettings() {
        this.color = new Color3f(0, 1, 0);
        this.transparency = 0.7f;
        this.isVisible = true;
        this.pointsToFitPlane = new ArrayList<>();
        this.distanceBetweenPlanesThreshold = 1E-10;
    }

    public PlaneSettings( Plane plane ) {
        this.name = plane.getName();
        this.normal = plane.getNormal();
        this.point = plane.getPoint();
        this.color = plane.getColor();
        this.transparency = plane.getTransparency();
        this.isVisible = plane.isVisible();
        this.pointsToFitPlane = plane.getPointsToFitPlaneDisplay().getPointsToFitPlane();
        this.distanceBetweenPlanesThreshold = plane.getDistanceBetweenPlanesThreshold();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final PlaneSettings other = (PlaneSettings) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!this.normal.equals(other.normal)) {
            return false;
        }
        if (!this.point.equals(other.point)) {
            return false;
        }
        if (!this.color.equals(other.color)) {
            return false;
        }
        if (this.transparency != other.transparency) {
            return false;
        }
        if (this.isVisible != other.isVisible) {
            return false;
        }
        if (!this.pointsToFitPlane.equals(other.pointsToFitPlane)) {
            return false;
        }
        if (this.distanceBetweenPlanesThreshold != other.distanceBetweenPlanesThreshold) {
            return false;
        }

        return true;
    }
}
