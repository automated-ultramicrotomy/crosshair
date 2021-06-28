package de.embl.schwab.crosshair.legacy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

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
        oldFormatSettingsReader.readSettings( oldJson.getAbsolutePath() );
    }
}