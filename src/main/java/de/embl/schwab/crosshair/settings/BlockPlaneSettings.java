package de.embl.schwab.crosshair.settings;

import de.embl.schwab.crosshair.plane.BlockPlane;
import de.embl.schwab.crosshair.points.VertexPoint;
import net.imglib2.RealPoint;
import org.jogamp.vecmath.Color3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to hold all settings related to block planes (e.g. display settings, normal, point...)
 */
public class BlockPlaneSettings extends PlaneSettings {

    public ArrayList<RealPoint> vertices; // all vertex points placed on the block plane
    public Map<VertexPoint, RealPoint> assignedVertices; // the subset of assigned vertices e.g. top left, top right...

    public BlockPlaneSettings() {
        super();
        this.color = new Color3f(0, 0, 1);
        this.vertices = new ArrayList<>();
        this.assignedVertices = new HashMap<>();
    }

    public BlockPlaneSettings( BlockPlane blockPlane ) {
        super( blockPlane );
        this.vertices = blockPlane.getVertexDisplay().getVertices();
        this.assignedVertices = blockPlane.getVertexDisplay().getAssignedVertices();
    }

    public BlockPlaneSettings( PlaneSettings planeSettings ) {
        this.vertices = new ArrayList<>();
        this.assignedVertices = new HashMap<>();

        this.name = planeSettings.name;
        this.normal = planeSettings.normal;
        this.point = planeSettings.point;
        this.color = planeSettings.color;
        this.transparency = planeSettings.transparency;
        this.isVisible = planeSettings.isVisible;
        this.pointsToFitPlane = planeSettings.pointsToFitPlane;
        this.distanceBetweenPlanesThreshold = planeSettings.distanceBetweenPlanesThreshold;
    }

    @Override
    public boolean equals(Object obj) {
        boolean isEqual = super.equals(obj);
        if (!isEqual) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final BlockPlaneSettings other = (BlockPlaneSettings) obj;
        if (!this.vertices.equals(other.vertices)) {
            return false;
        }
        if (!this.assignedVertices.equals(other.assignedVertices)) {
            return false;
        }

        return true;
    }
}
