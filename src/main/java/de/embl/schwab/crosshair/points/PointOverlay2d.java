package de.embl.schwab.crosshair.points;

import bdv.util.BdvOverlay;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class PointOverlay2d extends BdvOverlay {
    // same as https://github.com/bigdataviewer/bigdataviewer-vistools/blob/master/src/main/java/bdv/util/PointsOverlay.java
    // but sets size to zero after certain distance
    // could make it nicer like in the bdv workshop, where they make the size taper off in a sphere

        private boolean showPoints;

        public boolean checkPointsVisible () {
            return showPoints;
        }

        public void toggleShowPoints () {
            if (showPoints) {
                showPoints = false;
            } else {
                showPoints = true;
            }
        }

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

        // @Override
        // protected void draw( final Graphics2D graphics )
        // {
        //     if ( (pointsToFitPlane == null & vertexPoints == null) | !showPoints )
        //         return;
        //
        //     final AffineTransform3D transform = new AffineTransform3D();
        //     getCurrentTransform3D( transform );
        //     final double[] lPos = new double[ 3 ];
        //     final double[] gPos = new double[ 3 ];
        //     for ( final RealLocalizable p : pointsToFitPlane)
        //     {
        //         // get point position (in microns etc)
        //         p.localize( lPos );
        //         // get point position in viewer (I guess in pixel units?), so gpos[2] is the distance in pixels
        //         // from the current view plane
        //         transform.apply( lPos, gPos );
        //         final double size = getPointSize( gPos );
        //         final int x = ( int ) ( gPos[ 0 ] - 0.5 * size );
        //         final int y = ( int ) ( gPos[ 1 ] - 0.5 * size );
        //         final int w = ( int ) size;
        //         graphics.setColor( colPoint );
        //         graphics.fillOval( x, y, w, w );
        //     }
        //
        //     double[] lposSelected = new double[3];
        //     if (selectedPoint.containsKey("selected")) {
        //         selectedPoint.get("selected").localize(lposSelected);
        //     } else {
        //         lposSelected = null;
        //     }
        //
        //     for ( final RealLocalizable p : vertexPoints)
        //     {
        //         // get point position (in microns etc)
        //         p.localize( lPos );
        //         // get point position in viewer (I guess in pixel units?), so gpos[2] is the distance in pixels
        //         // from the current view plane
        //         transform.apply( lPos, gPos );
        //         final double size = getPointSize( gPos );
        //         final int x = ( int ) ( gPos[ 0 ] - 0.5 * size );
        //         final int y = ( int ) ( gPos[ 1 ] - 0.5 * size );
        //         final int w = ( int ) size;
        //         if (lposSelected != null & Arrays.equals(lPos, lposSelected)) {
        //             graphics.setColor(colSelected);
        //         } else {
        //             graphics.setColor(colVertex);
        //         }
        //         graphics.fillOval( x, y, w, w );
        //
        //     }
        //
        //     // add text for labelled vertices
        //     for (String key : namedVertices.keySet()) {
        //         RealPoint keyPoint = namedVertices.get(key);
        //         keyPoint.localize(lPos);
        //         transform.apply( lPos, gPos );
        //         graphics.setColor(colVertex);
        //         if (Math.abs(gPos[2]) < 5) {
        //             graphics.drawString(key, (int) gPos[0], (int) gPos[1]);
        //         }
        //     }
        //
        //     // add text for any active modes
        //     graphics.setColor(colModeText);
        //
        //
        //     if ( isInPointMode ) {
        //         String text = "Point Mode";
        //         drawModeText(graphics, text);
        //     } else if ( isInVertexMode ) {
        //         String text = "Vertex Mode";
        //         drawModeText(graphics, text);
        //     }
        //
        // }

        private double getPointSize (final double[] gPos)
        {
            if ( Math.abs( gPos[ 2 ] ) < 5 )
                return 5.0;
            else
                return 0.0;
        }

        private void drawModeText (Graphics2D graphics, String text) {
            graphics.setFont( new Font( "Monospaced", Font.PLAIN, 16 ) );
            int text_width = graphics.getFontMetrics().stringWidth(text);
            graphics.drawString( text, (int) graphics.getClipBounds().getWidth() - text_width - 10, (int)graphics.getClipBounds().getHeight() - 16 );
        }

}
