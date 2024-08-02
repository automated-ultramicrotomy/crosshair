package de.embl.schwab.crosshair.legacy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OldFormatSettingsReaderTest {

    private OldFormatSettingsReader oldFormatSettingsReader;

    @BeforeEach
    public void setUp() {
        oldFormatSettingsReader = new OldFormatSettingsReader();
    }


    @Test
    void readSettings() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        File oldJson = new File(classLoader.getResource("legacy/exampleBlock.json").getFile());
        OldFormatSettings settings = oldFormatSettingsReader.readSettings( oldJson.getAbsolutePath() );

        assertEquals(settings.planeNormals.size(), 2);
        assertTrue(settings.planeNormals.containsKey("block"));
        assertTrue(settings.planeNormals.containsKey("target"));
    }

    @Test
    void readInvalidSettings( @TempDir Path tempDir ) {
        // Disable logging to keep the test logs clean (we're expecting an error here)
        Logger logger = (Logger) LoggerFactory.getLogger(OldFormatSettingsReader.class);
        Level loggerLevel = logger.getLevel();
        logger.setLevel(Level.OFF);

        File invalidJsonPath = tempDir.resolve( "invalid.json" ).toFile();
        OldFormatSettings settings = oldFormatSettingsReader.readSettings( invalidJsonPath.getAbsolutePath() );
        assertNull( settings );

        logger.setLevel(loggerLevel);
    }
}