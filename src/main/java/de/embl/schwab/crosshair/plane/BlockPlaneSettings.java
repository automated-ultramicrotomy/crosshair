package de.embl.schwab.crosshair.plane;

import de.embl.schwab.crosshair.points.VertexPoint;
import net.imglib2.RealPoint;
import org.scijava.vecmath.Color3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
}
