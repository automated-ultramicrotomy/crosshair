package de.embl.schwab.crosshair.points.overlays;

import de.embl.schwab.crosshair.points.PointsToFitPlaneDisplay;

import java.awt.*;

/**
 * Class for points to fit plane overlay in the 2D BigDataViewer window
 * There is one of these per plane displayed in Crosshair
 */
public class PointsToFitPlane2dOverlay extends PointOverlay2d {

    private final PointsToFitPlaneDisplay pointsToFitPlaneDisplay;
    private final Color colPoint = new Color( 51, 255, 51);;

    /**
     * Create a points to fit plane overlay
     * @param pointsToFitPlaneDisplay points to fit plane display
     */
    public PointsToFitPlane2dOverlay( PointsToFitPlaneDisplay pointsToFitPlaneDisplay ) {
        this.pointsToFitPlaneDisplay = pointsToFitPlaneDisplay;
    }

    /**
     * Draw points to BigDataViewer window
     * @param g graphics 2D
     */
    @Override
    protected void draw( Graphics2D g ) {
        drawPoints( pointsToFitPlaneDisplay.getPointsToFitPlane(), colPoint, g );
    }
}
