package de.embl.schwab.crosshair.points;

import de.embl.schwab.crosshair.plane.Plane;

import java.awt.*;

public class PointsToFitPlane2dOverlay extends PointOverlay2d {

    private final Plane plane;
    private final Color colPoint = new Color( 51, 255, 51);;

    public PointsToFitPlane2dOverlay( Plane plane ) {
        this.plane = plane;
    }

    @Override
    protected void draw( Graphics2D g ) {
        drawPoints( plane.getPointsToFitPlane(), colPoint, g );
    }
}
