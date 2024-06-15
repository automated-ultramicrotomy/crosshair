package de.embl.schwab.crosshair.io;

import customnode.CustomMesh;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
        Map<String, CustomMesh> currentStl = STLResourceLoader.loadSTL("invalid.stl");
        assertNull(currentStl);
    }
}