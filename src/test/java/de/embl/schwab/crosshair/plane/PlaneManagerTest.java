package de.embl.schwab.crosshair.plane;

import bdv.util.BdvStackSource;
import de.embl.schwab.crosshair.TestHelpers;
import de.embl.schwab.crosshair.points.PointsToFitPlaneDisplay;
import de.embl.schwab.crosshair.points.overlays.PointOverlay2d;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.PlaneSettings;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static de.embl.schwab.crosshair.TestHelpers.*;
import static de.embl.schwab.crosshair.utils.GeometryUtils.checkVectorsParallel;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlaneManagerTest {

    private PlaneManager planeManager;
    private Image3DUniverse universe;
    private BdvStackSource bdvStackSource;
    private AffineTransform3D initialViewerTransform;
    private Content imageContent;
    private Point3d min;
    private Point3d max;
    private Vector3d normal;
    private Vector3d point;

    @BeforeAll
    void overallSetUp() {
        // Keep same 3D viewer and bigdataviewer open for all tests in class - this speeds up the tests + makes them
        // more stable
        TestHelpers.BdvAnd3DViewer bdvAnd3DViewer = createBdvAnd3DViewer();
        universe = bdvAnd3DViewer.universe;
        bdvStackSource = bdvAnd3DViewer.bdvStackSource;
        imageContent = bdvAnd3DViewer.imageContent;
        initialViewerTransform = bdvAnd3DViewer.initialViewerTransform;

        min = new Point3d();
        imageContent.getMin(min);
        max = new Point3d();
        imageContent.getMax(max);
    }

    @BeforeEach
    void setUp() {
        // Example point and normal for testing
        point = new Vector3d(-188.47561306126704, 15.353222856645068, 621.5211240735744);
        normal = new Vector3d(0.20791169081775954, 0.08525118065879457, 0.9744254538021788);

        planeManager = new PlaneManager(bdvStackSource, universe, imageContent);
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

        universe = null;
        bdvStackSource = null;
        planeManager = null;
        imageContent = null;
    }

    @Test
    void checkNamedPlaneExists() {
        String name = "testPlane";

        assertFalse(planeManager.checkNamedPlaneExists(name));
        planeManager.addPlane(name, normal, point);
        assertTrue(planeManager.checkNamedPlaneExists(name));
    }

    @Test
    void checkNamedPlaneExistsAndOrientationIsSet() {
        // Plane without orientation set
        String planeWithoutOrientation = "testPlane";
        planeManager.addPlane(planeWithoutOrientation);
        assertFalse(planeManager.checkNamedPlaneExistsAndOrientationIsSet(planeWithoutOrientation));

        // PLane with orientation set
        String planeWithOrientation = "testPlane2";
        planeManager.addPlane(planeWithOrientation, normal, point);
        assertTrue(planeManager.checkNamedPlaneExistsAndOrientationIsSet(planeWithOrientation));
    }

    @Test
    void addPlaneFromSettings() {
        PlaneSettings planeSettings = new PlaneSettings();
        planeSettings.name = "testPlane";
        planeSettings.point = point;
        planeSettings.normal = normal;

        planeManager.addPlane(planeSettings);
        assertTrue(planeManager.getPlaneNames().contains(planeSettings.name));
        Plane plane = planeManager.getPlane(planeSettings.name);
        assertEquals(plane.getName(), planeSettings.name);
        assertEquals(plane.getPoint(), planeSettings.point);
        assertEquals(plane.getNormal(), planeSettings.normal);
    }

    @Test
    void addPlaneFromNormalAndPoint() {
        String name = "testPlane";
        planeManager.addPlane(name, normal, point);

        assertTrue(planeManager.getPlaneNames().contains(name));
        Plane plane = planeManager.getPlane(name);
        assertEquals(plane.getName(), name);
        assertEquals(plane.getPoint(), point);
        assertEquals(plane.getNormal(), normal);
    }

    @Test
    void addPlaneFromName() {
        String name = "testPlane";
        planeManager.addPlane(name);

        assertTrue(planeManager.getPlaneNames().contains(name));
        Plane plane = planeManager.getPlane(name);
        assertEquals(plane.getName(), name);

        // point, normal and centroid should be null as no orientation info was given
        assertNull(plane.getPoint());
        assertNull(plane.getNormal());
        assertNull(plane.getCentroid());
    }

    @Test
    void addPlaneAtCurrentView() {
        String name = "testPlane";
        planeManager.addPlaneAtCurrentView(name);

        ArrayList<Vector3d> planeDefinition = planeManager.getPlaneDefinitionOfCurrentView();
        assertTrue(planeManager.getPlaneNames().contains(name));
        Plane plane = planeManager.getPlane(name);
        assertEquals(plane.getName(), name);
        assertEquals(plane.getPoint(), planeDefinition.get(1));
        assertEquals(plane.getNormal(), planeDefinition.get(0));
    }

    @Test
    void addBlockPlaneFromSettings() {
        BlockPlaneSettings blockPlaneSettings = new BlockPlaneSettings();
        blockPlaneSettings.name = "testBlockPlane";
        blockPlaneSettings.point = point;
        blockPlaneSettings.normal = normal;

        planeManager.addBlockPlane(blockPlaneSettings);
        assertTrue(planeManager.getPlaneNames().contains(blockPlaneSettings.name));
        BlockPlane blockPlane = planeManager.getBlockPlane(blockPlaneSettings.name);
        assertEquals(blockPlane.getName(), blockPlaneSettings.name);
        assertEquals(blockPlane.getPoint(), blockPlaneSettings.point);
        assertEquals(blockPlane.getNormal(), blockPlaneSettings.normal);
    }

    @Test
    void addBlockPlaneFromNormalAndPoint() {
        String name = "testBlockPlane";
        planeManager.addBlockPlane(name, normal, point);

        assertTrue(planeManager.getPlaneNames().contains(name));
        BlockPlane blockPlane = planeManager.getBlockPlane(name);
        assertEquals(blockPlane.getName(), name);
        assertEquals(blockPlane.getPoint(), point);
        assertEquals(blockPlane.getNormal(), normal);
    }

    @Test
    void addBlockPlaneFromName() {
        String name = "testBlockPlane";
        planeManager.addBlockPlane(name);

        assertTrue(planeManager.getPlaneNames().contains(name));
        BlockPlane blockPlane = planeManager.getBlockPlane(name);
        assertEquals(blockPlane.getName(), name);

        // point, normal and centroid should be null as no orientation info was given
        assertNull(blockPlane.getPoint());
        assertNull(blockPlane.getNormal());
        assertNull(blockPlane.getCentroid());
    }

    @Test
    void addBlockPlaneAtCurrentView() {
        String name = "testBlockPlane";
        planeManager.addBlockPlaneAtCurrentView(name);

        ArrayList<Vector3d> planeDefinition = planeManager.getPlaneDefinitionOfCurrentView();
        assertTrue(planeManager.getPlaneNames().contains(name));
        BlockPlane blockPlane = planeManager.getBlockPlane(name);
        assertEquals(blockPlane.getName(), name);
        assertEquals(blockPlane.getPoint(), planeDefinition.get(1));
        assertEquals(blockPlane.getNormal(), planeDefinition.get(0));
    }

    @Test
    void updatePlane() {
        // Add plane with certain orientation
        String name = "testPlane";
        planeManager.addPlane(name, normal, point);

        // Update orientation of plane
        Vector3d newNormal = new Vector3d(0, 0, 1);
        Vector3d newPoint = new Vector3d(0, 0, 0);
        planeManager.updatePlane(newNormal, newPoint, name);
        Plane plane = planeManager.getPlane(name);

        assertEquals(plane.getNormal(), newNormal);
        assertEquals(plane.getPoint(), newPoint);
    }

    @Test
    void setPlaneColourToAligned() {
        String name = "testPlane";
        planeManager.addPlane(name, normal, point);
        planeManager.setPlaneColourToAligned(name);

        Color3f alignedColor = new Color3f(1, 0, 0);
        assertEquals(universe.getContent(name).getColor(), alignedColor);
    }

    @Test
    void setPlaneColourToUnaligned() {
        String name = "testPlane";
        planeManager.addPlane(name, normal, point);
        Plane plane = planeManager.getPlane(name);

        planeManager.setPlaneColourToAligned(name);
        assertNotEquals(universe.getContent(name).getColor(), plane.getColor());

        planeManager.setPlaneColourToUnaligned(name);
        assertEquals(universe.getContent(name).getColor(), plane.getColor());
    }

    @Test
    void updatePlaneOnTransformChange() {
        // Add plane with certain orientation
        String name = "testPlane";
        planeManager.addPlane(name, normal, point);

        // Random affine transform containing translation and rotation
        AffineTransform3D transform = new AffineTransform3D();
        transform.translate(20, 30, 40);
        transform.rotate(1, 20);

        planeManager.updatePlaneOnTransformChange(transform, name);
        Plane plane = planeManager.getPlane(name);

        // Check normal and point were updated correctly. Expected normal / point comes from
        // getPlaneDefinitionFromViewTransform with the transform above
        assertEquals(plane.getNormal(), new Vector3d(-0.9129452507276278, 0.0, 0.40808206181339196));
        assertEquals(plane.getPoint(), new Vector3d(-20.0, -30.0, -40.0));
    }

    @Test
    void updatePlaneCurrentView() {
        // Add plane with certain orientation
        String name = "testPlane";
        planeManager.addPlane(name, normal, point);

        // Update plane to match orientation of current bdv view
        planeManager.updatePlaneCurrentView(name);
        Plane plane = planeManager.getPlane(name);

        // Check normal and point match expected
        assertEquals(plane.getNormal(), new Vector3d(0, 0, 1));
        assertEquals(plane.getPoint(), new Vector3d(
                -237.91633435828103, -1.9942294517880588, 398.32276000000013));
    }

    /**
     * Different points to fit plane to test...
     * @return stream of points list (used to fit a plane), followed by expected normal and point for plane
     */
    static Stream<Arguments> fitToPointsProvider() {
        return Stream.of(
                // Points all on plane perpendicular to z axis
                arguments(
                        Arrays.asList(
                                new RealPoint(0, 0, 0),
                                new RealPoint(1, 1, 0),
                                new RealPoint(-1, 1, 0)
                        ),
                        new Vector3d(0, 0, 1),
                        new Vector3d(0.0, 0.6666666666666666, 0.0)
                ),
                // Points all on plane perpendicular to x axis
                arguments(
                        Arrays.asList(
                                new RealPoint(1, -1, 0),
                                new RealPoint(1, 1, 0),
                                new RealPoint(1, 0, 1)
                        ),
                        new Vector3d(-1.0, -4.039029528107861E-17, -1.3608770355161334E-16),
                        new Vector3d(1.0, 0.0, 0.3333333333333333)
                ),
                // Randomly oriented plane
                arguments(
                        Arrays.asList(
                                new RealPoint(1, -1, 1),
                                new RealPoint(1, 1, 1),
                                new RealPoint(-1, 0, -1)
                        ),
                        new Vector3d(-0.7071067811865475, 1.1102230246251565E-16, 0.7071067811865476),
                        new Vector3d(0.3333333333333333, 0.0, 0.3333333333333333)
                )
        );
    }

    @ParameterizedTest
    @MethodSource("fitToPointsProvider")
    void fitToPoints(List<RealPoint> points, Vector3d expectedNormal, Vector3d expectedPoint) {
        // Add test plane with no orientation
        String name = "testPlane";
        planeManager.addPlane(name);
        Plane plane = planeManager.getPlane(name);
        PointsToFitPlaneDisplay display = plane.getPointsToFitPlaneDisplay();

        // add points to display and fit to them
        for (RealPoint point: points) {
            display.addPointToFitPlane(point);
        }
        planeManager.fitToPoints(name);

        // Check normal and point of plane are as expected
        assertEquals(plane.getNormal(), expectedNormal);
        assertEquals(plane.getPoint(), expectedPoint);
    }

    @Test
    void getAll2dPointOverlays() {
        // Add plane with certain orientation
        String planeName = "testPlane";
        planeManager.addPlane(planeName, normal, point);

        // Add block plane with certain orientation
        String blockPlaneName = "testBlockPlane";
        planeManager.addBlockPlane(blockPlaneName, normal, point);

        // Should have three overlays, one from the points to fit plane display of each plane + one
        // from the block plane's vertex display
        List<PointOverlay2d> overlays = planeManager.getAll2dPointOverlays();
        assertEquals(overlays.size(), 3);
    }

    @Test
    void moveViewToNamedPlane() throws InterruptedException {
        // Add plane with certain orientation
        String name = "testPlane";
        planeManager.addPlane(name, normal, point);

        // Check current view doesn't match the plane's orientation
        List<Vector3d> viewPlaneDefinition = planeManager.getPlaneDefinitionOfCurrentView();
        Vector3d viewVectorPoint = viewPlaneDefinition.get(1);
        RealPoint viewRealPoint = new RealPoint(viewVectorPoint.getX(), viewVectorPoint.getY(), viewVectorPoint.getZ());

        assertFalse(checkVectorsParallel(viewPlaneDefinition.get(0), normal));
        assertFalse(planeManager.getPlane(name).isPointOnPlane(viewRealPoint));

        // Move view to named plane - have to wait for 2 seconds to allow the animated movement to finish
        planeManager.moveViewToNamedPlane(name);
        TimeUnit.SECONDS.sleep(2);

        // Check new view matches the plane's orientation - i.e. normals are parallel and view point lies on plane
        viewPlaneDefinition = planeManager.getPlaneDefinitionOfCurrentView();
        viewVectorPoint = viewPlaneDefinition.get(1);
        viewRealPoint = new RealPoint(viewVectorPoint.getX(), viewVectorPoint.getY(), viewVectorPoint.getZ());

        assertTrue(checkVectorsParallel(viewPlaneDefinition.get(0), normal));
        assertTrue(planeManager.getPlane(name).isPointOnPlane(viewRealPoint));
    }

    @Test
    void removeNamedPlane() {
        // Add a test plane
        String name = "testPlane";
        planeManager.addPlane(name, normal, point);
        assertTrue(planeManager.getPlaneNames().contains(name));
        assertTrue(universe.contains(name));

        // Remove plane
        planeManager.removeNamedPlane(name);
        assertFalse(planeManager.getPlaneNames().contains(name));
        assertFalse(universe.contains(name));
        assertNull(planeManager.getPlane(name));
    }

    @Test
    void removeNamedBlockPlane() {
        // Add a test block plane
        String name = "testBlockPlane";
        planeManager.addBlockPlane(name, normal, point);
        assertTrue(planeManager.getPlaneNames().contains(name));
        assertTrue(universe.contains(name));

        // Remove plane
        planeManager.removeNamedPlane(name);
        assertFalse(planeManager.getPlaneNames().contains(name));
        assertFalse(universe.contains(name));
        assertNull(planeManager.getPlane(name));
    }
}