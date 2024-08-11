package de.embl.schwab.crosshair.settings;

import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.TestHelpers;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.plane.PlaneManager;
import ij3d.Content;
import ij3d.Image3DUniverse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static de.embl.schwab.crosshair.TestHelpers.createBdvAnd3DViewer;
import static org.junit.jupiter.api.Assertions.*;

class SettingsWriterTest {

    private SettingsWriter settingsWriter;
    private Settings settings;

    @BeforeEach
    public void setUp() {
        // read sample settings
        ClassLoader classLoader = this.getClass().getClassLoader();
        File settingsJson = new File(classLoader.getResource("exampleBlock.json").getFile());
        settings = new SettingsReader().readSettings( settingsJson.getAbsolutePath() );

        settingsWriter = new SettingsWriter();
    }

    @Test
    void writeSettings( @TempDir Path tempDir ) {
        // Write settings to json file
        File newJson = tempDir.resolve( "newBlock.json" ).toFile();
        settingsWriter.writeSettings( settings, newJson.getAbsolutePath() );
        assertTrue( newJson.exists() );

        // Read from json file + check all settings are the same
        Settings newSettings = new SettingsReader().readSettings( newJson.getAbsolutePath() );
        assertEquals(settings, newSettings);
    }

    @Test
    void createSettings() throws MicrotomeManager.IncorrectMicrotomeConfiguration {

        // initialise planemanager + image content from example settings file
        TestHelpers.BdvAnd3DViewer bdvAnd3DViewer = createBdvAnd3DViewer();
        Image3DUniverse universe = bdvAnd3DViewer.universe;
        Content imageContent = bdvAnd3DViewer.imageContent;

        PlaneManager planeManager = new PlaneManager(bdvAnd3DViewer.bdvStackSource, universe, imageContent);
        Map<String, Content> imageNameToContent = new HashMap<>();
        imageNameToContent.put(Crosshair.image, imageContent);

        new SettingsReader().loadSettings(settings, planeManager, imageNameToContent);

        // Create settings from current planemanager / image content state + check same as original settings
        Settings newSettings = settingsWriter.createSettings(planeManager, imageNameToContent);
        assertEquals(settings, newSettings);

        // cleanup bdv and 3D viewer
        universe.close();
        universe.cleanup();
        bdvAnd3DViewer.bdvStackSource.getBdvHandle().close();
    }
}