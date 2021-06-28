package de.embl.schwab.crosshair.settings;

import de.embl.schwab.crosshair.legacy.OldFormatSettingsReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SettingsWriterTest {

    private SettingsWriter settingsWriter;

    @BeforeEach
    public void setUp() {
        settingsWriter = new SettingsWriter();
    }

    @Test
    void writeSettings( @TempDir Path tempDir ) {
        // read sample settings
        ClassLoader classLoader = this.getClass().getClassLoader();
        File settingsJson = new File(classLoader.getResource("exampleBlock.json").getFile());
        File newJson = tempDir.resolve( "newBlock.json" ).toFile();

        Settings settings = new SettingsReader().readSettings( settingsJson.getAbsolutePath() );
        settingsWriter.writeSettings( settings, newJson.getAbsolutePath() );

        assertTrue( newJson.exists() );

        Settings newSettings = new SettingsReader().readSettings( newJson.getAbsolutePath() );

        for( String planeName: settings.planeNameToSettings.keySet() ) {
            assertTrue( newSettings.planeNameToSettings.containsKey( planeName ) );
        }

        for( String imageName: settings.imageNameToSettings.keySet() ) {
            assertTrue( newSettings.imageNameToSettings.containsKey( imageName ));
        }

        // TODO - properly test if all the fields are equal, not just that they contain the same
        // images and planes
    }
}