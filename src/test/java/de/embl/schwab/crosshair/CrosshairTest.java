package de.embl.schwab.crosshair;

import de.embl.cba.bdv.utils.sources.LazySpimSource;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class CrosshairTest {

    @Test
    void openCrosshairFromBdv() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        File xray = new File(classLoader.getResource("exampleBlock.xml").getFile());

        final LazySpimSource imageSource = new LazySpimSource("raw", xray.getAbsolutePath());
        Crosshair crosshair = new Crosshair(imageSource);

        // Check successfully created a bdv window + 3D viewer
        assertNotNull(crosshair.getBdvHandle());
        assertNotNull(crosshair.getUniverse());
    }

}