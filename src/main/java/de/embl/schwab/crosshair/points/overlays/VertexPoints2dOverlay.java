package de.embl.schwab.crosshair.points.overlays;

import de.embl.schwab.crosshair.points.VertexDisplay;
import de.embl.schwab.crosshair.points.VertexPoint;
import net.imglib2.RealPoint;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VertexPoints2dOverlay extends PointOverlay2d {

    private final VertexDisplay vertexDisplay;

    private final Color colVertex = new Color(0, 255, 255);
    private final Color colSelected = new Color(153, 0, 76);

    public VertexPoints2dOverlay( VertexDisplay vertexDisplay ) {
        this.vertexDisplay = vertexDisplay;
    }

    @Override
    protected void draw(Graphics2D g) {
        if ( vertexDisplay.isVertexSelected() ) {
            ArrayList<RealPoint> selectedVertices = new ArrayList<>();
            selectedVertices.add( vertexDisplay.getSelectedVertex() );
            drawPoints( selectedVertices, colSelected, g );
        }

        drawPoints( vertexDisplay.getVerticesExceptForSelected(), colVertex, g );

        Map<String, RealPoint> pointLabelToPoint = new HashMap<>();
        for ( VertexPoint vertexPoint: vertexDisplay.getAssignedVertices().keySet() ) {
            pointLabelToPoint.put( vertexPoint.toString(), vertexDisplay.getAssignedVertices().get( vertexPoint ) );
        }
        drawTextOnPoints( pointLabelToPoint, colVertex, g );
    }
}
