package de.embl.schwab.crosshair.io;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import customnode.CustomMesh;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class STLResourceLoaderTest {

    @ParameterizedTest
    @ValueSource(strings = {"/arc.stl", "/holder_back.stl", "/holder_front.stl", "/knife.stl"})
    void loadSTL(String modelName) {
        Map<String, CustomMesh> currentStl = STLResourceLoader.loadSTL(modelName);
        assertNotNull(currentStl);
    }

    @Test
    void loadInvalidSTL() {
        // Disable logging to keep the test logs clean (we're expecting an error here)
        Logger logger = (Logger) LoggerFactory.getLogger(STLResourceLoader.class);
        Level loggerLevel = logger.getLevel();
        logger.setLevel(Level.OFF);

        Map<String, CustomMesh> currentStl = STLResourceLoader.loadSTL("invalid.stl");
        assertNull(currentStl);

        logger.setLevel(loggerLevel);
    }
}