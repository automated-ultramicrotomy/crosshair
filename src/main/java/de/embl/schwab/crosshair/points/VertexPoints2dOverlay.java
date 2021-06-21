package de.embl.schwab.crosshair.points;

import de.embl.schwab.crosshair.plane.BlockPlane;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class VertexPoints2dOverlay extends PointOverlay2d {

    private List<? extends RealLocalizable> vertexPoints;
    private Map<String, RealPoint> selectedPoint;
    private Map<String, RealPoint> namedVertices;

    private Color colVertex = new Color(0, 255, 255);
    private Color colSelected = new Color(153, 0, 76);

    private boolean isInVertexMode;



    public VertexPoints2dOverlay( BlockPlane blockPlane ) {

    }

    @Override
    protected void draw(Graphics2D g) {

    }
}
