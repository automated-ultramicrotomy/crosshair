package de.embl.schwab.crosshair.points.overlays;

import bdv.util.BdvOverlay;
import net.imglib2.RealLocalizable;
import net.imglib2.realtransform.AffineTransform3D;

import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Class for point overlay in the 2D BigDataViewer window
 * same as https://github.com/bigdataviewer/bigdataviewer-vistools/blob/master/src/main/java/bdv/util/PointsOverlay.java
 * but sets size to zero after certain distance
 */
public abstract class PointOverlay2d extends BdvOverlay {
    // TODO- could make it nicer like in the bdv workshop, where they make the size taper off in a sphere

    private boolean showPoints = true;

    public boolean checkPointsVisible () {
        return showPoints;
    }

    /**
     * Toggle visibility of points
     */
    public void toggleShowPoints () {
        if (showPoints) {
            showPoints = false;
        } else {
            showPoints = true;
        }
    }

    /**
     * Draw points in the BigDataViewer window
     * @param points 3D points to draw
     * @param color colour of points
     * @param graphics graphics 2D
     */
    protected void drawPoints( List< ? extends RealLocalizable> points, Color color, final Graphics2D graphics ) {

        if ( !showPoints ) {
            return;
        }

        final AffineTransform3D transform = new AffineTransform3D();
        getCurrentTransform3D( transform );
        final double[] lPos = new double[ 3 ];
        final double[] gPos = new double[ 3 ];
        for ( final RealLocalizable p : points)
        {
            // get point position (in microns etc)
            p.localize( lPos );
            // get point position in viewer (I guess in pixel units?), so gpos[2] is the distance in pixels
            // from the current view plane
            transform.apply( lPos, gPos );
            final double size = getPointSize( gPos );
            final int x = ( int ) ( gPos[ 0 ] - 0.5 * size );
            final int y = ( int ) ( gPos[ 1 ] - 0.5 * size );
            final int w = ( int ) size;
            graphics.setColor( color );
            graphics.fillOval( x, y, w, w );
        }
    }

    /**
     * Draw text next to points in the BigDataViewer window
     * @param pointLabelToPoint map of text labels to 3D points
     * @param color colour of text
     * @param graphics graphics 2D
     */
    protected void drawTextOnPoints( Map< String,  ? extends RealLocalizable> pointLabelToPoint,
                                     Color color, final Graphics2D graphics ) {

        if ( !showPoints ) {
            return;
        }

        graphics.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
        // add text for labelled vertices
        final AffineTransform3D transform = new AffineTransform3D();
        getCurrentTransform3D( transform );
        final double[] lPos = new double[ 3 ];
        final double[] gPos = new double[ 3 ];

        for ( String label : pointLabelToPoint.keySet() ) {
            RealLocalizable point = pointLabelToPoint.get( label );
            point.localize(lPos);
            transform.apply( lPos, gPos );
            graphics.setColor( color );
            if (Math.abs(gPos[2]) < 5) {
                graphics.drawString(label, (int) gPos[0], (int) gPos[1]);
            }
        }
    }

    private double getPointSize (final double[] gPos)
    {
        if ( Math.abs( gPos[ 2 ] ) < 5 )
            return 5.0;
        else
            return 0.0;
    }

}
