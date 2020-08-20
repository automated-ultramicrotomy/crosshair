package de.embl.schwab.crosshair.bdv;

import bdv.util.BdvOverlay;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PointsOverlaySizeChange extends BdvOverlay {
    // same as https://github.com/bigdataviewer/bigdataviewer-vistools/blob/master/src/main/java/bdv/util/PointsOverlay.java
    // but sets size to zero after certain distance
    // could make it nicer like in the bdv workshop, where they make the size taper off in a sphere
        private List< ? extends RealLocalizable> points;
        private List<? extends RealLocalizable> vertexPoints;
        private Map<String, RealPoint> selectedPoint;
        private Map<String, RealPoint> namedVertices;

        private Color colPoint;
        private Color colVertex;
        private Color colSelected;

        private boolean showPoints;

        public < T extends RealLocalizable > void setPoints(final List< T > points , final List <T> vertexPoints,
                                                            final Map<String, RealPoint> selectedPoint,
                                                            final Map<String, RealPoint> namedVertices)
        {
            this.points = points;
            this.vertexPoints = vertexPoints;
            this.selectedPoint = selectedPoint;
            this.namedVertices = namedVertices;
            this.showPoints = true;
        }

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

        @Override
        protected void draw( final Graphics2D graphics )
        {
            if ( (points == null & vertexPoints == null) | !showPoints )
                return;

            colPoint = new Color( 51, 255, 51);
            colVertex = new Color(0, 255, 255);
            colSelected = new Color(153, 0, 76);

            final AffineTransform3D transform = new AffineTransform3D();
            getCurrentTransform3D( transform );
            final double[] lPos = new double[ 3 ];
            final double[] gPos = new double[ 3 ];
            for ( final RealLocalizable p : points )
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
                graphics.setColor( getColor( gPos, colPoint) );
                graphics.fillOval( x, y, w, w );
            }

            double[] lposSelected = new double[3];
            if (selectedPoint.containsKey("selected")) {
                selectedPoint.get("selected").localize(lposSelected);
            } else {
                lposSelected = null;
            }

            for ( final RealLocalizable p : vertexPoints)
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
                if (lposSelected != null & Arrays.equals(lPos, lposSelected)) {
                    graphics.setColor(getColor(gPos, colSelected));
                } else {
                    graphics.setColor(getColor(gPos, colVertex));
                }
                graphics.fillOval( x, y, w, w );

            }

            // add text for labelled vertices
            for (String key : namedVertices.keySet()) {
                RealPoint keyPoint = namedVertices.get(key);
                keyPoint.localize(lPos);
                transform.apply( lPos, gPos );
                graphics.setColor(getColor(gPos, colVertex));
                if (Math.abs(gPos[2]) < 5) {
                    graphics.drawString(key, (int) gPos[0], (int) gPos[1]);
                }
            }



        }

        /** screen pixels [x,y,z] **/
        private Color getColor( final double[] gPos, Color col )
        {
            int alpha = 255 - ( int ) Math.round( Math.abs( gPos[ 2 ] ) );

            if ( alpha < 64 )
                alpha = 64;

            return new Color( col.getRed(), col.getGreen(), col.getBlue(), alpha );
        }

        private double getPointSize (final double[] gPos)
        {
            if ( Math.abs( gPos[ 2 ] ) < 5 )
                return 5.0;
            else
                return 0.0;
        }

}
