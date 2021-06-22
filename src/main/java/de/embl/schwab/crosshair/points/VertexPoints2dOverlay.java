package de.embl.schwab.crosshair.points;

import de.embl.schwab.crosshair.plane.BlockPlane;
import net.imglib2.RealPoint;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VertexPoints2dOverlay extends PointOverlay2d {

    private final BlockPlane blockPlane;

    private final Color colVertex = new Color(0, 255, 255);
    private final Color colSelected = new Color(153, 0, 76);

    public VertexPoints2dOverlay( BlockPlane blockPlane ) {
        this.blockPlane = blockPlane;
    }

    @Override
    protected void draw(Graphics2D g) {
        if ( blockPlane.isVertexSelected() ) {
            ArrayList<RealPoint> selectedVertices = new ArrayList<>();
            selectedVertices.add( blockPlane.getSelectedVertex() );
            drawPoints( selectedVertices, colSelected, g );
        }

        drawPoints( blockPlane.getVerticesExceptForSelected(), colVertex, g );

        Map<String, RealPoint> pointLabelToPoint = new HashMap<>();
        for ( VertexPoint vertexPoint: blockPlane.getAssignedVertices().keySet() ) {
            pointLabelToPoint.put( vertexPoint.toString(), blockPlane.getAssignedVertices().get( vertexPoint ) );
        }
        drawTextOnPoints( pointLabelToPoint, colVertex, g );
    }
}
