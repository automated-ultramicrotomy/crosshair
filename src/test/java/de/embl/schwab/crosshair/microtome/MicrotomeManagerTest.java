package de.embl.schwab.crosshair.microtome;

import bdv.util.BdvStackSource;
import de.embl.schwab.crosshair.TestHelpers;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.PlaneSettings;
import de.embl.schwab.crosshair.settings.Settings;
import de.embl.schwab.crosshair.settings.SettingsReader;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.realtransform.AffineTransform3D;
import org.junit.jupiter.api.*;
import org.scijava.java3d.Transform3D;

import java.io.File;

import static de.embl.schwab.crosshair.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MicrotomeManagerTest {

    private Image3DUniverse universe;
    private BdvStackSource bdvStackSource;
    private Content imageContent;
    private AffineTransform3D initialViewerTransform;
    private MicrotomeManager microtomeManager;
    private PlaneManager planeManager;

    @BeforeAll
    void overallSetup() {
        // Keep same 3D viewer and bigdataviewer open for all tests in class - this speeds up the tests + makes them
        // more stable
        TestHelpers.BdvAnd3DViewer bdvAnd3DViewer = createBdvAnd3DViewer();
        universe = bdvAnd3DViewer.universe;
        bdvStackSource = bdvAnd3DViewer.bdvStackSource;
        imageContent = bdvAnd3DViewer.imageContent;
        initialViewerTransform = bdvAnd3DViewer.initialViewerTransform;
    }

    @BeforeEach
    void setUp() {
        // add block and target plane for testing - load from example settings file
        planeManager = new PlaneManager(bdvStackSource, universe, imageContent);

        ClassLoader classLoader = this.getClass().getClassLoader();
        File settingsFile = new File(classLoader.getResource("exampleBlock.json").getFile());
        SettingsReader reader = new SettingsReader();
        Settings settings = reader.readSettings(settingsFile.getAbsolutePath());

        for ( PlaneSettings planeSettings: settings.planeNameToSettings.values() ) {
            if ( planeSettings instanceof BlockPlaneSettings) {
                planeManager.addBlockPlane( (BlockPlaneSettings) planeSettings );
            } else {
                planeManager.addPlane( planeSettings );
            }
        }

        microtomeManager = new MicrotomeManager(planeManager, universe, imageContent, bdvStackSource, "microns");
    }

    @AfterEach
    void tearDown() {
        resetBdv(bdvStackSource.getBdvHandle(), initialViewerTransform);
        reset3DViewer(universe, imageContent);
    }

    @AfterAll
    void overallTearDown() {
        universe.close();
        universe.cleanup();
        bdvStackSource.getBdvHandle().close();
    }

    @Test
    void enterExitMicrotomeMode() {
        double initialKnifeAngle = 10;
        double initialTiltAngle = 10;
        Microtome microtome = microtomeManager.getMicrotome();

        // Check image content (block) starts in neutral position
        Transform3D identityTransform = new Transform3D();
        Transform3D transform = new Transform3D();
        imageContent.getLocalRotate().getTransform(transform);
        assertEquals(transform, identityTransform);

        imageContent.getLocalTranslate().getTransform(transform);
        assertEquals(transform, identityTransform);

        // Check microtome mode becomes active
        assertFalse(microtomeManager.isMicrotomeModeActive());
        microtomeManager.enterMicrotomeMode(initialKnifeAngle, initialTiltAngle);
        assertTrue(microtomeManager.isMicrotomeModeActive());

        // Check image content (block) transform has been changed from identity
        imageContent.getLocalRotate().getTransform(transform);
        assertNotEquals(transform, identityTransform);
        imageContent.getLocalTranslate().getTransform(transform);
        assertNotEquals(transform, identityTransform);

        // Check initial angles properly set
        assertEquals(microtome.getInitialKnifeAngle(), initialKnifeAngle);
        assertEquals(microtome.getInitialTiltAngle(), initialTiltAngle);

        // Check all microtome meshes are added + visible
        for (String name: microtome.getMicrotomeObjectNames()) {
            assertTrue(universe.contains(name));
            assertTrue(universe.getContent(name).isVisible());
        }

        // Check microtome mode becomes inactive
        microtomeManager.exitMicrotomeMode();
        assertFalse(microtomeManager.isMicrotomeModeActive());

        // Check microtome meshes becomes invisible
        for (String name: microtome.getMicrotomeObjectNames()) {
            assertFalse(universe.getContent(name).isVisible());
        }

        // Check image content (block) transform is reset
        imageContent.getLocalRotate().getTransform(transform);
        assertEquals(transform,  identityTransform);
        imageContent.getLocalTranslate().getTransform(transform);
        assertEquals(transform, identityTransform);
    }

    @Test
    void setKnife() {
    }

    @Test
    void setTilt() {
    }

    @Test
    void setRotation() {
    }

    @Test
    void setSolution() {
    }

    @Test
    void enterCuttingMode() {
    }

    @Test
    void setCuttingDepth() {
    }

    @Test
    void exitCuttingMode() {
    }
}