package de.embl.schwab.crosshair.points;

import de.embl.schwab.crosshair.plane.Plane;
import net.imglib2.RealLocalizable;

import java.awt.*;
import java.util.List;

public class PointsToFitPlane2dOverlay extends PointOverlay2d {

    private Plane plane;

    private Color colPoint = new Color( 51, 255, 51);;

    private boolean showPoints;
    private boolean isInPointMode;

    public PointsToFitPlane2dOverlay( Plane plane ) {
        this.plane = plane;
    }

    @Override
    protected void draw( Graphics2D g ) {
        drawPoints( plane.getPointsToFitPlane(), colPoint, g );
    }
}
