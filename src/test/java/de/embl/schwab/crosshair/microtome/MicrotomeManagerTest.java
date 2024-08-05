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
import java.util.HashSet;
import java.util.Set;

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

    /**
     * Check if image content and planes (in 3D viewer) have identity transform
     * @param shouldBeIdentity Whether the transform should be identity or not
     */
    private void assertionsForContentTransforms(boolean shouldBeIdentity) {
        Transform3D identityTransform = new Transform3D();
        Transform3D transform = new Transform3D();

        Set<String> contentNames = new HashSet<>(planeManager.getPlaneNames());
        contentNames.add(imageContent.getName());

        for (String name: contentNames) {
            Content content = universe.getContent(name);

            content.getLocalRotate().getTransform(transform);
            if (shouldBeIdentity) {
                assertEquals(transform, identityTransform);
            } else {
                assertNotEquals(transform, identityTransform);
            }

            content.getLocalTranslate().getTransform(transform);
            if (shouldBeIdentity) {
                assertEquals(transform, identityTransform);
            } else {
                assertNotEquals(transform, identityTransform);
            }

        }
    }

    @Test
    void enterExitMicrotomeMode() {
        double initialKnifeAngle = 10;
        double initialTiltAngle = 10;
        Microtome microtome = microtomeManager.getMicrotome();

        // Check image content (block) + planes start in neutral position
        assertionsForContentTransforms(true);

        // Check microtome mode becomes active
        assertFalse(microtomeManager.isMicrotomeModeActive());
        microtomeManager.enterMicrotomeMode(initialKnifeAngle, initialTiltAngle);
        assertTrue(microtomeManager.isMicrotomeModeActive());

        // Check image content (block) + plane transforms have been changed from identity
        assertionsForContentTransforms(false);

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

        // Check image content (block) + plane transforms are reset
        assertionsForContentTransforms(true);
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