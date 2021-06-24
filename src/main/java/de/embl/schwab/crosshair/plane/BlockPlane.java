package de.embl.schwab.crosshair.plane;

import de.embl.schwab.crosshair.points.PointsToFitPlaneDisplay;
import de.embl.schwab.crosshair.points.VertexDisplay;
import ij3d.Content;
import org.scijava.vecmath.Vector3d;

public class BlockPlane extends Plane {

    private final VertexDisplay vertexDisplay;

    public BlockPlane( BlockPlaneSettings settings, Vector3d centroid, Content mesh,
                      PointsToFitPlaneDisplay pointsToFitPlaneDisplay, VertexDisplay vertexDisplay ) {

        super( settings, centroid, mesh, pointsToFitPlaneDisplay );
        this.vertexDisplay = vertexDisplay;
    }

    public VertexDisplay getVertexDisplay() {
        return vertexDisplay;
    }

    @Override
    public BlockPlaneSettings getSettings() {
        return new BlockPlaneSettings( this );
    }
}
