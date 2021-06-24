package de.embl.schwab.crosshair.points.overlays;

import de.embl.schwab.crosshair.points.PointsToFitPlaneDisplay;

import java.awt.*;

public class PointsToFitPlane2dOverlay extends PointOverlay2d {

    private final PointsToFitPlaneDisplay pointsToFitPlaneDisplay;
    private final Color colPoint = new Color( 51, 255, 51);;

    public PointsToFitPlane2dOverlay( PointsToFitPlaneDisplay pointsToFitPlaneDisplay ) {
        this.pointsToFitPlaneDisplay = pointsToFitPlaneDisplay;
    }

    @Override
    protected void draw( Graphics2D g ) {
        drawPoints( pointsToFitPlaneDisplay.getPointsToFitPlane(), colPoint, g );
    }
}
