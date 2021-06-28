package de.embl.schwab.crosshair.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

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
        settingsReader.readSettings( json.getAbsolutePath() );
    }
}