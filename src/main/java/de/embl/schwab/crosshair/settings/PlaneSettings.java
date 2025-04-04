package de.embl.schwab.crosshair.settings;

import de.embl.schwab.crosshair.plane.Plane;
import net.imagej.units.DefaultUnitService;
import net.imglib2.RealPoint;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Vector3d;

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
    // distance used to be 'on' plane (in the units of the input image e.g. microns)
    public double distanceBetweenPlanesThreshold;

    /**
     * Create plane settings with default values
     * @param unit unit of distance used by the input image to Crosshair e.g. microns
     */
    public PlaneSettings( String unit ) {
        this.color = new Color3f(0, 1, 0);
        this.transparency = 0.7f;
        this.isVisible = true;
        this.pointsToFitPlane = new ArrayList<>();
        this.distanceBetweenPlanesThreshold = getDefaultDistanceBetweenPlanesThreshold(unit);
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

    private double getDefaultDistanceBetweenPlanesThreshold(String unit) {
        double defaultDistanceMicrons = 1E-10;

        // Convert the default micron distance into the unit used for the input image.
        // This is necessary as say you have two identical images, one using microns (e.g. 1 micron voxel size) and
        // one nanometres (e.g. 1000 nm voxel size). Without conversion a distance threshold of 0.1 would be much
        // stricter for the nm image (0.1nm) than for the micron image (0.1 microns) even though their voxels are the
        // exact same size.

        DefaultUnitService unitService = new DefaultUnitService();
        return unitService.value(defaultDistanceMicrons, "microns", unit);
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
