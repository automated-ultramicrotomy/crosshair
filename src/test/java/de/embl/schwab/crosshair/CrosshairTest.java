package de.embl.schwab.crosshair;

import de.embl.cba.bdv.utils.sources.LazySpimSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CrosshairTest {

    private Crosshair crosshair;

    @AfterEach
    void tearDown() {
        crosshair.getUniverse().close();
        crosshair.getBdvHandle().close();
    }

    @Test
    void openCrosshairFromBdv() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        File xray = new File(classLoader.getResource("exampleBlock.xml").getFile());

        final LazySpimSource imageSource = new LazySpimSource("raw", xray.getAbsolutePath());
        crosshair = new Crosshair(imageSource);

        // Check successfully created crosshair controls, bdv window + 3D viewer
        assertNotNull(crosshair.getCrosshairFrame());
        assertNotNull(crosshair.getBdvHandle());
        assertNotNull(crosshair.getUniverse());

        // Check all windows on screen + not overlapping
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Window[] windows = new Window[]{
                crosshair.getCrosshairFrame(),
                SwingUtilities.getWindowAncestor(crosshair.getBdvHandle().getViewerPanel()),
                crosshair.getUniverse().getWindow()
        };
        int[] xMins = new int[windows.length];
        int[] xMaxes = new int[windows.length];

        for (int i = 0; i < windows.length; i++) {
            Window window = windows[i];

            // Check height within screen limits
            assertTrue(window.getLocationOnScreen().y >= 0,
                    "window " + i + "'s y min is off screen");
            assertTrue(window.getLocationOnScreen().y + window.getHeight() <= screenSize.height,
                    "window " + i + "'s y max is off screen");

            xMins[i] = window.getLocationOnScreen().x;
            xMaxes[i] = window.getLocationOnScreen().x + window.getWidth();

            // check width within screen limits
            assertTrue(xMins[i] >= 0, "window " + i + "'s x min is off screen");
            assertTrue(xMaxes[i] <= screenSize.width, "window " + i + "'s x max is off screen");
        }

        // Check windows don't overlap
        System.out.println(screenSize);
        System.out.println(Arrays.toString(xMins));
        System.out.println(Arrays.toString(xMaxes));
        assertTrue(xMaxes[0] <= xMins[1], "window 0's right edge overlaps with window 1's left");
        assertTrue(xMaxes[1] <= xMins[2], "window 1's right edge overlaps with window 2's left");
    }

}