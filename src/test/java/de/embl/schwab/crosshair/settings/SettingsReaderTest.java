package de.embl.schwab.crosshair.settings;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.embl.schwab.crosshair.BdvAnd3DViewer;
import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.plane.PlaneManager;
import ij3d.Content;
import ij3d.Image3DUniverse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SettingsReaderTest {

    private SettingsReader settingsReader;

    @BeforeEach
    public void setUp() {
        settingsReader = new SettingsReader();
    }


    @Test
    void readSettings() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        File json = new File(classLoader.getResource("exampleBlock.json").getFile());
        Settings settings = settingsReader.readSettings( json.getAbsolutePath() );

        Map<String, PlaneSettings> planeNameToSettings = settings.planeNameToSettings;
        Map< String, ImageContentSettings> imageNameToSettings = settings.imageNameToSettings;

        assertEquals(planeNameToSettings.size(), 2);
        assertTrue(planeNameToSettings.containsKey("block"));
        assertTrue(planeNameToSettings.containsKey("target"));

        assertEquals(imageNameToSettings.size(), 1);
        assertTrue(imageNameToSettings.containsKey("image"));
    }

    @Test
    void readInvalidSettings( @TempDir Path tempDir ) {
        // Disable logging to keep the test logs clean (we're expecting an error here)
        Logger logger = (Logger) LoggerFactory.getLogger(SettingsReader.class);
        Level loggerLevel = logger.getLevel();
        logger.setLevel(Level.OFF);

        File invalidJsonPath = tempDir.resolve( "invalid.json" ).toFile();
        Settings settings = settingsReader.readSettings( invalidJsonPath.getAbsolutePath() );
        assertNull( settings );

        logger.setLevel(loggerLevel);
    }

    @Test
    void loadSettings() throws MicrotomeManager.IncorrectMicrotomeConfiguration {
        // read settings
        ClassLoader classLoader = this.getClass().getClassLoader();
        File json = new File(classLoader.getResource("exampleBlock.json").getFile());
        Settings settings = settingsReader.readSettings( json.getAbsolutePath() );

        // initialise planemanager (with no planes) + image content
        BdvAnd3DViewer.reset();
        Image3DUniverse universe = BdvAnd3DViewer.getUniverse();
        Content imageContent = BdvAnd3DViewer.getImageContent();

        PlaneManager planeManager = new PlaneManager(BdvAnd3DViewer.getBdvStackSource(), universe, imageContent, "microns");
        assertTrue(planeManager.getPlaneNames().isEmpty());

        Map<String, Content> imageNameToContent = new HashMap<>();
        imageNameToContent.put(Crosshair.image, imageContent);

        // Load settings
        settingsReader.loadSettings(settings, planeManager, imageNameToContent);

        // Check all planes loaded from settings
        for ( PlaneSettings planeSettings: settings.planeNameToSettings.values() ) {
            assertTrue(planeManager.checkNamedPlaneExists(planeSettings.name));
            assertTrue(universe.contains(planeSettings.name));

            if ( planeSettings instanceof BlockPlaneSettings) {
                assertEquals(planeManager.getBlockPlane(
                        planeSettings.name).getSettings(),
                        planeSettings
                );
            } else {
                assertEquals(planeManager.getPlane(planeSettings.name).getSettings(), planeSettings);
            }
        }

        // Check all image settings are also loaded
        for ( ImageContentSettings imageSettings: settings.imageNameToSettings.values() ) {
            Content content = imageNameToContent.get(imageSettings.name);

            assertNull(content.getColor());
            assertNull(imageSettings.imageColour);

            assertEquals(content.getTransparency(), imageSettings.imageTransparency);

            int[] lut = new int[256];
            content.getRedLUT(lut);
            assertArrayEquals(lut, imageSettings.redLut);

            content.getGreenLUT(lut);
            assertArrayEquals(lut, imageSettings.greenLut);

            content.getBlueLUT(lut);
            assertArrayEquals(lut, imageSettings.blueLut);

            content.getAlphaLUT(lut);
            assertArrayEquals(lut, imageSettings.alphaLut);
        }

        // cleanup bdv and 3D viewer
        BdvAnd3DViewer.reset();
    }
}