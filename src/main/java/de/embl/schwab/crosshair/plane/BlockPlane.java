package de.embl.schwab.crosshair.plane;

import de.embl.schwab.crosshair.points.PointsToFitPlaneDisplay;
import de.embl.schwab.crosshair.points.VertexDisplay;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import ij3d.Content;
import org.jogamp.vecmath.Vector3d;

/**
 * Class to represent the block face (block plane)
 */
public class BlockPlane extends Plane {

    private final VertexDisplay vertexDisplay;

    /**
     * Create a block plane
     * @param settings block plane settings
     * @param centroid Centroid of block plane mesh
     * @param mesh 3D custom triangle mesh of block plane
     * @param pointsToFitPlaneDisplay points to fit plane display
     * @param vertexDisplay vertex display
     */
    public BlockPlane(BlockPlaneSettings settings, Vector3d centroid, Content mesh,
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
