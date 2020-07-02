package de.embl.cba.targeting;

import bdv.util.BdvOverlay;
import net.imglib2.RealLocalizable;
import net.imglib2.realtransform.AffineTransform3D;

import java.awt.*;
import java.util.List;

public class PointsOverlaySizeChange extends BdvOverlay {
    // same as https://github.com/bigdataviewer/bigdataviewer-vistools/blob/master/src/main/java/bdv/util/PointsOverlay.java
    // but sets size to zero after certain distance
    // could make it nicer like in teh bdv workshop, where they make the size taper off in a sphere
        private List< ? extends RealLocalizable> points;

        private Color col;

        public < T extends RealLocalizable > void setPoints( final List< T > points )
        {
            this.points = points;
        }

        @Override
        protected void draw( final Graphics2D graphics )
        {
            if ( points == null )
                return;

            col = new Color( info.getColor().get() );

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
                graphics.setColor( getColor( gPos ) );
                graphics.fillOval( x, y, w, w );
            }
        }

        /** screen pixels [x,y,z] **/
        private Color getColor( final double[] gPos )
        {
            int alpha = 255 - ( int ) Math.round( Math.abs( gPos[ 2 ] ) );

            if ( alpha < 64 )
                alpha = 64;

            return new Color( col.getRed(), col.getGreen(), col.getBlue(), alpha );
        }

        private double getPointSize (final double[] gPos)
        {
            if ( Math.abs( gPos[ 2 ] ) < 3 )
                return 5.0;
            else
                return 0.0;
        }

}
