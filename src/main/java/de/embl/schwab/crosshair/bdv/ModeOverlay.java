package de.embl.schwab.crosshair.bdv;

import bdv.util.BdvOverlay;
import de.embl.schwab.crosshair.plane.PlaneManager;

import java.awt.*;

/**
 * Class to display the current Crosshair mode as an overlay on the BigDataViewer window
 */
public class ModeOverlay extends BdvOverlay  {

    private PlaneManager planeManager;
    private Color colModeText = new Color(255, 255, 255);

    /**
     * Create a BigDataViewer overlay for the Crosshair mode text
     * @param planeManager Crosshair plane manager
     */
    public ModeOverlay( PlaneManager planeManager ) {
        this.planeManager = planeManager;
    }

    @Override
    protected void draw( final Graphics2D graphics ) {
        // add text for any active modes
        graphics.setColor(colModeText);

        if ( planeManager.isInPointMode() ) {
            String text = "Point Mode";
            drawModeText(graphics, text);
        } else if ( planeManager.isInVertexMode() ) {
            String text = "Vertex Mode";
            drawModeText(graphics, text);
        }
    }

    private void drawModeText (Graphics2D graphics, String text) {
        graphics.setFont( new Font( "Monospaced", Font.PLAIN, 16 ) );
        int text_width = graphics.getFontMetrics().stringWidth(text);
        graphics.drawString( text, (int) graphics.getClipBounds().getWidth() - text_width - 10,
                (int)graphics.getClipBounds().getHeight() - 16 );
    }
}
