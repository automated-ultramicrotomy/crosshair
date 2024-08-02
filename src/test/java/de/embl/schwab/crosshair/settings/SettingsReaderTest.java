package de.embl.schwab.crosshair.settings;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
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
}