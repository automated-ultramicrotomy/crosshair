package de.embl.schwab.crosshair.microtome;

import bdv.util.BdvStackSource;
import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.TestHelpers;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.points.VertexDisplay;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.PlaneSettings;
import de.embl.schwab.crosshair.settings.Settings;
import de.embl.schwab.crosshair.settings.SettingsReader;
import de.embl.schwab.crosshair.points.VertexPoint;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Vector3d;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static de.embl.schwab.crosshair.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
    void enterExitMicrotomeMode() throws MicrotomeManager.IncorrectMicrotomeConfiguration {
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

    /**
     * Check can't enter microtome mode without target or block plane initialised
     */
    @ParameterizedTest
    @ValueSource(strings = { Crosshair.target, Crosshair.block })
    void enterMicrotomeModeWithInvalidPlanes(String missingPlane) {
        double initialKnifeAngle = 10;
        double initialTiltAngle = 10;

        // remove the target plane
        planeManager.removeNamedPlane(missingPlane);
        assertFalse(planeManager.getPlaneNames().contains(missingPlane));

        // Check microtome mode can't be entered + throws error
        MicrotomeManager.IncorrectMicrotomeConfiguration thrown = assertThrows(
                MicrotomeManager.IncorrectMicrotomeConfiguration.class,
                () -> microtomeManager.enterMicrotomeMode(initialKnifeAngle, initialTiltAngle)
        );
        assertFalse(microtomeManager.isMicrotomeModeActive());
    }

    /**
     * Check can't enter microtome mode without all block face vertices initialised
     */
    @ParameterizedTest
    @EnumSource(VertexPoint.class)
    void enterMicrotomeModeWithInvalidVertices(VertexPoint missingVertex) {
        double initialKnifeAngle = 10;
        double initialTiltAngle = 10;

        // remove the specified vertex
        VertexDisplay vertexDisplay = planeManager.getVertexDisplay(Crosshair.block);
        vertexDisplay.getAssignedVertices().remove(missingVertex);
        assertFalse(vertexDisplay.getAssignedVertices().containsKey(missingVertex));

        // Check microtome mode can't be entered + throws error
        MicrotomeManager.IncorrectMicrotomeConfiguration thrown = assertThrows(
                MicrotomeManager.IncorrectMicrotomeConfiguration.class,
                () -> microtomeManager.enterMicrotomeMode(initialKnifeAngle, initialTiltAngle)
        );
        assertFalse(microtomeManager.isMicrotomeModeActive());
    }

    /**
     * Different knife angles to test...
     * @return stream of knife angle, expected angle between knife and target, expected knife model translation
     * and expected knife model rotation. All expected values were read from the debugger after setting the given
     * knife angle.
     */
    static Stream<Arguments> knifeAngleProvider() {
        return Stream.of(
                arguments(
                        -10,
                        28.902061339214328,
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, -3.7375830004293675E-6,
                                0.0, 1.0, 0.0, -1824.0312175965944,
                                0.0, 0.0, 1.0, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                355.02252197265625, 62.60005187988281, 0.0, 125.20010375976562,
                                -62.60005187988281, 355.02252197265625, 0.0, 708.0450439453125,
                                0.0, 0.0, 360.49932861328125, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        })
                ),
                arguments(
                        5,
                        15.796863886874052,
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, 3.309421288122394E-7,
                                0.0, 1.0, 0.0, -1824.0312066123636,
                                0.0, 0.0, 1.0, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                359.12750244140625, -31.419586181640625, 0.0, -62.83917236328125,
                                31.419586181640625, 359.12750244140625, 0.0, 716.2550048828125,
                                0.0, 0.0, 360.49932861328125, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        })
                )
        );
    }

    @ParameterizedTest
    @MethodSource("knifeAngleProvider")
    void setKnife(double knifeAngle, double expectedAngleKnifeTarget, Transform3D expectedTranslation,
                  Transform3D expectedRotation) throws MicrotomeManager.IncorrectMicrotomeConfiguration {
        double initialKnifeAngle = 10;
        double initialTiltAngle = 10;
        Microtome microtome = microtomeManager.getMicrotome();
        microtomeManager.enterMicrotomeMode(initialKnifeAngle, initialTiltAngle);

        microtomeManager.setKnife(knifeAngle);
        assertEquals(microtome.getKnife(), knifeAngle);

        // Check transform of 3D model was updated correctly
        Transform3D transform = new Transform3D();
        Content knifeModel = universe.getContent("/knife.stl");

        knifeModel.getLocalTranslate(transform);
        assertEquals(transform, expectedTranslation);
        knifeModel.getLocalRotate(transform);
        assertEquals(transform, expectedRotation);

        // check angle between knife and target was updated correctly
        assertEquals(microtome.getAngleKnifeTarget(), expectedAngleKnifeTarget);
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