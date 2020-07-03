package de.embl.cba.targeting;

import bdv.util.BdvOverlay;
import net.imglib2.RealLocalizable;
import net.imglib2.ops.parse.token.Real;
import net.imglib2.realtransform.AffineTransform3D;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class PointsOverlaySizeChange extends BdvOverlay {
    // same as https://github.com/bigdataviewer/bigdataviewer-vistools/blob/master/src/main/java/bdv/util/PointsOverlay.java
    // but sets size to zero after certain distance
    // could make it nicer like in teh bdv workshop, where they make the size taper off in a sphere
        private List< ? extends RealLocalizable> points;
        private List<? extends RealLocalizable> vertex_points;
        private RealLocalizable selected_point;

        private Color col_point;
        private Color col_vertex;
        private Color col_selected;

        public < T extends RealLocalizable > void setPoints(final List< T > points , final List <T> vertex_points,
                                                            final RealLocalizable selected_point)
        {
            this.points = points;
            this.vertex_points = vertex_points;
            this.selected_point = selected_point;
        }

        @Override
        protected void draw( final Graphics2D graphics )
        {
            if ( points == null & vertex_points == null)
                return;

            col_point = new Color( 51, 255, 51);
            col_vertex = new Color(0, 255, 255);
            col_selected = new Color(153, 0, 76);

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
                graphics.setColor( getColor( gPos, col_point) );
                graphics.fillOval( x, y, w, w );
            }

            double[] lPos_selected = new double[3];
            selected_point.localize(lPos_selected);

            for ( final RealLocalizable p : vertex_points )
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
                if (Arrays.equals(lPos, lPos_selected)) {
                    graphics.setColor(getColor(gPos, col_selected));
                } else {
                    graphics.setColor(getColor(gPos, col_vertex));
                }
                graphics.fillOval( x, y, w, w );
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
