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
import de.embl.schwab.crosshair.solution.SolutionsCalculator;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.realtransform.AffineTransform3D;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Point3d;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    private double initialKnifeAngle;
    private double initialTiltAngle;

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
        initialKnifeAngle = 10;
        initialTiltAngle = 10;
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
     * Check named contents (in 3D viewer) have correct transforms.
     * @param expectedTranslation expected translation transform
     * @param expectedRotation expected rotation transform
     */
    private void assertionsForContentTransforms(
            Collection<String> contentNames, Transform3D expectedTranslation, Transform3D expectedRotation) {

        Transform3D transform = new Transform3D();

        for (String name: contentNames) {
            Content content = universe.getContent(name);

            content.getLocalRotate().getTransform(transform);
            assertEquals(transform, expectedRotation, "rotation for " + name + " doesn't match");

            content.getLocalTranslate().getTransform(transform);
            assertEquals(transform, expectedTranslation, "translation for " + name + " doesn't match");
        }
    }

    @ParameterizedTest
    @MethodSource("de.embl.schwab.crosshair.microtome.MicrotomeManagerTestProviders#initialAngleProvider")
    void enterExitMicrotomeMode(double initialKnifeAngle, double initialTiltAngle,
                                Transform3D imageExpectedTranslation, Transform3D imageExpectedRotation,
                                Transform3D targetExpectedTranslation, Transform3D targetExpectedRotation,
                                Transform3D blockExpectedTranslation, Transform3D blockExpectedRotation
    ) throws MicrotomeManager.IncorrectMicrotomeConfiguration {

        Microtome microtome = microtomeManager.getMicrotome();

        // Check image content (block) + planes start in neutral position
        Transform3D identityTransform = new Transform3D();
        Set<String> contentNames = new HashSet<>(planeManager.getPlaneNames());
        contentNames.add(imageContent.getName());
        assertionsForContentTransforms(contentNames, identityTransform, identityTransform);

        // Check microtome mode becomes active
        assertFalse(microtomeManager.isMicrotomeModeActive());
        microtomeManager.enterMicrotomeMode(initialKnifeAngle, initialTiltAngle);
        assertTrue(microtomeManager.isMicrotomeModeActive());

        // Check image content (block) + plane transforms have been changed correctly.
        assertionsForContentTransforms(
                Collections.singletonList(imageContent.getName()),
                imageExpectedTranslation,
                imageExpectedRotation
        );
        assertionsForContentTransforms(
                Collections.singletonList(Crosshair.target),
                targetExpectedTranslation,
                targetExpectedRotation
        );
        assertionsForContentTransforms(
                Collections.singletonList(Crosshair.block),
                blockExpectedTranslation,
                blockExpectedRotation
        );

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
        assertionsForContentTransforms(contentNames, identityTransform, identityTransform);
    }

    /**
     * Check can't enter microtome mode without target or block plane initialised
     */
    @ParameterizedTest
    @ValueSource(strings = { Crosshair.target, Crosshair.block })
    void enterMicrotomeModeWithInvalidPlanes(String missingPlane) {
        // remove the named plane
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


    @ParameterizedTest
    @MethodSource("de.embl.schwab.crosshair.microtome.MicrotomeManagerTestProviders#knifeAngleProvider")
    void setKnife(double knifeAngle, double expectedAngleKnifeTarget, Transform3D expectedTranslation,
                  Transform3D expectedRotation) throws MicrotomeManager.IncorrectMicrotomeConfiguration {
        Microtome microtome = microtomeManager.getMicrotome();
        microtomeManager.enterMicrotomeMode(initialKnifeAngle, initialTiltAngle);

        microtomeManager.setKnife(knifeAngle);
        assertEquals(microtome.getKnife(), knifeAngle);

        // Check transform of knife 3D model was updated correctly
        assertionsForContentTransforms(
                Collections.singletonList("/knife.stl"),
                expectedTranslation,
                expectedRotation
        );

        // check angle between knife and target was updated correctly
        assertEquals(microtome.getAngleKnifeTarget(), expectedAngleKnifeTarget);
    }

    @ParameterizedTest
    @MethodSource("de.embl.schwab.crosshair.microtome.MicrotomeManagerTestProviders#tiltAngleProvider")
    void setTilt(double tiltAngle, double expectedAngleKnifeTarget,
                 Transform3D holderBackExpectedTranslation, Transform3D holderBackExpectedRotation,
                 Transform3D holderFrontExpectedTranslation, Transform3D holderFrontExpectedRotation,
                 Transform3D imageExpectedTranslation, Transform3D imageExpectedRotation
    ) throws MicrotomeManager.IncorrectMicrotomeConfiguration {

        Microtome microtome = microtomeManager.getMicrotome();
        microtomeManager.enterMicrotomeMode(initialKnifeAngle, initialTiltAngle);

        microtomeManager.setTilt(tiltAngle);
        assertEquals(microtome.getTilt(), tiltAngle);

        // Check transforms of holder and image 3D models were updated correctly
        assertionsForContentTransforms(
                Collections.singletonList("/holder_back.stl"),
                holderBackExpectedTranslation,
                holderBackExpectedRotation
        );
        assertionsForContentTransforms(
                Collections.singletonList("/holder_front.stl"),
                holderFrontExpectedTranslation,
                holderFrontExpectedRotation
        );
        assertionsForContentTransforms(
                Collections.singletonList(imageContent.getName()),
                imageExpectedTranslation,
                imageExpectedRotation
        );

        // check angle between knife and target was updated correctly
        assertEquals(microtome.getAngleKnifeTarget(), expectedAngleKnifeTarget);
    }

    @ParameterizedTest
    @MethodSource("de.embl.schwab.crosshair.microtome.MicrotomeManagerTestProviders#rotationAngleProvider")
    void setRotation(double rotationAngle, double expectedAngleKnifeTarget,
                     Transform3D holderBackExpectedTranslation, Transform3D holderBackExpectedRotation,
                     Transform3D holderFrontExpectedTranslation, Transform3D holderFrontExpectedRotation,
                     Transform3D imageExpectedTranslation, Transform3D imageExpectedRotation) throws MicrotomeManager.IncorrectMicrotomeConfiguration {

        Microtome microtome = microtomeManager.getMicrotome();
        microtomeManager.enterMicrotomeMode(initialKnifeAngle, initialTiltAngle);

        microtomeManager.setRotation(rotationAngle);
        assertEquals(microtome.getRotation(), rotationAngle);

        // Check transforms of holder and image 3D models were updated correctly
        assertionsForContentTransforms(
                Collections.singletonList("/holder_back.stl"),
                holderBackExpectedTranslation,
                holderBackExpectedRotation
        );
        assertionsForContentTransforms(
                Collections.singletonList("/holder_front.stl"),
                holderFrontExpectedTranslation,
                holderFrontExpectedRotation
        );
        assertionsForContentTransforms(
                Collections.singletonList(imageContent.getName()),
                imageExpectedTranslation,
                imageExpectedRotation
        );

        // check angle between knife and target was updated correctly
        assertEquals(microtome.getAngleKnifeTarget(), expectedAngleKnifeTarget);
    }

    @ParameterizedTest
    @MethodSource("de.embl.schwab.crosshair.microtome.MicrotomeManagerTestProviders#solutionAngleProvider")
    void setSolution(double solutionAngle, double expectedTiltAngle, double expectedKnifeAngle,
                     double expectedDistanceToCut, VertexPoint expectedFirstTouch, boolean expectedValidSolution
    ) throws MicrotomeManager.IncorrectMicrotomeConfiguration {

        microtomeManager.enterMicrotomeMode(initialKnifeAngle, initialTiltAngle);

        microtomeManager.setSolution(solutionAngle);
        SolutionsCalculator solutions = microtomeManager.getSolutions();
        assertEquals(microtomeManager.getCurrentSolution().getRotation(), solutionAngle);
        assertEquals(solutions.getSolutionRotation(), solutionAngle);

        // Check other solution values are as expected
        assertEquals(solutions.getSolutionTilt(), expectedTiltAngle);
        assertEquals(solutions.getSolutionKnife(), expectedKnifeAngle);
        assertEquals(solutions.getDistanceToCut(), expectedDistanceToCut);
        assertEquals(solutions.getSolutionFirstTouchVertexPoint(), expectedFirstTouch);
        assertEquals(solutions.isValidSolution(), expectedValidSolution);
    }

    @ParameterizedTest
    @MethodSource("de.embl.schwab.crosshair.microtome.MicrotomeManagerTestProviders#cuttingProvider")
    void enterExitCuttingMode(double knifeAngle, Point3d expectedCuttingPlaneMin, Point3d expectedCuttingPlaneMax,
                              double expectedCuttingDepthMin, double expectedCuttingDepthMax
    ) throws MicrotomeManager.IncorrectMicrotomeConfiguration {
        microtomeManager.enterMicrotomeMode(initialKnifeAngle, initialTiltAngle);

        // Initialise ultramicrotome with correct angles
        // TODO - this is currently done in the UI - so replicate this here. Eventually this should be part of
        // enterMicrotomeMode() directly
        microtomeManager.setKnife(initialKnifeAngle);
        microtomeManager.setTilt(initialTiltAngle);
        microtomeManager.setRotation(0);

        // Set to given knife angle
        microtomeManager.setKnife(knifeAngle);

        // Check cutting mode becomes active
        assertFalse(microtomeManager.isCuttingModeActive());
        microtomeManager.enterCuttingMode();
        assertTrue(microtomeManager.isCuttingModeActive());

        // Check cutting plane is added + visible
        assertTrue(universe.contains(Cutting.cuttingPlane));
        Content cuttingPlane = universe.getContent(Cutting.cuttingPlane);
        assertTrue(cuttingPlane.isVisible());

        // Check cutting plane mesh min/max position are as expected
        // I check the min/max rather than the transform, as the transform is identity in this initial position.
        Point3d point = new Point3d();
        cuttingPlane.getMin(point);
        assertEquals(point, expectedCuttingPlaneMin);

        cuttingPlane.getMax(point);
        assertEquals(point, expectedCuttingPlaneMax);

        // Check cutting depth min/max are as expected
        assertEquals(microtomeManager.getCutting().getCuttingDepthMin(), expectedCuttingDepthMin);
        assertEquals(microtomeManager.getCutting().getCuttingDepthMax(), expectedCuttingDepthMax);

        // Check cutting mode can be disabled + cutting plane is removed
        microtomeManager.exitCuttingMode();
        assertFalse(microtomeManager.isCuttingModeActive());
        assertFalse(universe.contains(Cutting.cuttingPlane));
    }

    @ParameterizedTest
    @MethodSource("de.embl.schwab.crosshair.microtome.MicrotomeManagerTestProviders#cuttingDepthProvider")
    void setCuttingDepth(double cuttingDepth, Transform3D expectedCuttingPlaneTranslation,
                         Transform3D expectedCuttingPlaneRotation, double[] expectedBdvTransform
    ) throws MicrotomeManager.IncorrectMicrotomeConfiguration, InterruptedException {
        microtomeManager.enterMicrotomeMode(initialKnifeAngle, initialTiltAngle);

        // Initialise ultramicrotome with correct angles
        // TODO - this is currently done in the UI - so replicate this here. Eventually this should be part of
        // enterMicrotomeMode() directly
        microtomeManager.setKnife(initialKnifeAngle);
        microtomeManager.setTilt(initialTiltAngle);
        microtomeManager.setRotation(0);

        // Check viewer is initially not in expected orientation
        assertFalse(Arrays.equals(
                bdvStackSource.getBdvHandle().getViewerPanel().state().getViewerTransform().getRowPackedCopy(),
                expectedBdvTransform
        ));

        // Set cutting depth
        microtomeManager.enterCuttingMode();
        microtomeManager.setCuttingDepth(cuttingDepth);

        // have to wait for 1 second to allow the animated bdv movement to finish (otherwise fails in CI)
        TimeUnit.SECONDS.sleep(1);

        // Check transform of cutting plane is as expected
        assertionsForContentTransforms(
                Collections.singletonList(Cutting.cuttingPlane),
                expectedCuttingPlaneTranslation,
                expectedCuttingPlaneRotation
        );

        // Check viewer is now in expected orientation
        assertTrue(Arrays.equals(
                bdvStackSource.getBdvHandle().getViewerPanel().state().getViewerTransform().getRowPackedCopy(),
                expectedBdvTransform
        ));
    }

}