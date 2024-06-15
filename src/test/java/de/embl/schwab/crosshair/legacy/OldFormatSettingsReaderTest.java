package de.embl.schwab.crosshair.legacy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        File invalidJsonPath = tempDir.resolve( "invalid.json" ).toFile();
        OldFormatSettings settings = oldFormatSettingsReader.readSettings( invalidJsonPath.getAbsolutePath() );

        assertNull( settings );
    }
}