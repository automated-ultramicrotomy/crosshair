package de.embl.schwab.crosshair.plane;

import bdv.util.BdvStackSource;
import customnode.CustomMeshNode;
import de.embl.schwab.crosshair.BdvAnd3DViewer;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.PlaneSettings;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.realtransform.AffineTransform3D;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3d;

import java.util.List;
import java.util.stream.Stream;

import static de.embl.schwab.crosshair.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlaneCreatorTest {

    private PlaneCreator planeCreator;
    private Image3DUniverse universe;
    private BdvStackSource bdvStackSource;
    private AffineTransform3D initialViewerTransform;
    private Content imageContent;

    private Point3d min;
    private Point3d max;

    @BeforeAll
    void overallSetup() {
        // Keep same 3D viewer and bigdataviewer open for all tests in class - this speeds up the tests + makes them
        // more stable
        BdvAnd3DViewer bdvAnd3DViewer = createBdvAnd3DViewer();
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
        planeCreator = new PlaneCreator(universe, imageContent, bdvStackSource);
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
    }

    /**
     * Check values in plane match plane settings and expected centroid
     */
    private void assertionsForPlane(PlaneSettings planeSettings, Plane plane, Vector3d expectedCentroid) {
        assertEquals(plane.getName(), planeSettings.name);
        assertEquals(plane.getPoint(), planeSettings.point);
        assertEquals(plane.getNormal(), planeSettings.normal);
        assertEquals(plane.getTransparency(), planeSettings.transparency);
        assertEquals(plane.getColor(), planeSettings.color);
        assertEquals(plane.isVisible(), planeSettings.isVisible);
        assertNotNull(plane.getPointsToFitPlaneDisplay());
        assertEquals(plane.getCentroid(), expectedCentroid);
    }

    /**
     * Check plane mesh is added to the 3D viewer with correct settings
     */
    private void assertionsFor3DMesh(PlaneSettings planeSettings, Plane plane) {
        assertTrue(universe.contains(plane.getName()));

        Content planeMesh = universe.getContent(plane.getName());
        assertEquals(planeMesh.isVisible(), planeSettings.isVisible);
        assertEquals(planeMesh.getTransparency(), planeSettings.transparency);
        assertEquals(planeMesh.getColor(), planeSettings.color);

        // Check all points in the mesh lie on the given plane (within 1E-4 to account for precision errors - the mesh
        // stores points as floats rather than doubles, so we have to give more leeway). Also check all points
        // are within the content bounds.
        CustomMeshNode meshNode = (CustomMeshNode) planeMesh.getContent();
        List<Point3f> meshPoints = meshNode.getMesh().getMesh();
        for (Point3f meshPoint: meshPoints) {
            assertTrue(GeometryUtils.distanceFromPointToPlane(
                    new Vector3d(meshPoint), planeSettings.normal, planeSettings.point) < 1E-4);
            assertTrue(meshPoint.x >= min.x && meshPoint.x <= max.x);
            assertTrue(meshPoint.y >= min.y && meshPoint.y <= max.y);
            assertTrue(meshPoint.z >= min.z && meshPoint.z <= max.z);
        }
    }

    /**
     * Different plane orientations to test...
     * @return stream of point, normal, expectedCentroid
     */
    static Stream<Arguments> planeArgumentProvider() {
        return Stream.of(
                // Point at centre of image content, plane aligned to z axis
                arguments(
                        new Vector3d(607.3829956054688, 607.3829956054688, 309.4661560058594),
                        new Vector3d(0, 0, 1),
                        new Vector3d(607.3829956054688, 607.3829956054688, 309.4661560058594)),
                // Values from a randomly aligned plane created by tracking in the viewer
                arguments(
                        new Vector3d(-188.47561306126704, 15.353222856645068, 621.5211240735744),
                        new Vector3d(0.20791169081775954, 0.08525118065879457, 0.9744254538021788),
                        new Vector3d(607.3829956054688, 607.3829956054688, 399.9140783532721)),
                // Values from a randomly aligned plane created by tracking in the viewer
                arguments(
                        new Vector3d(0.6618597935512298, 37.48411448950936, 984.2992808834338),
                        new Vector3d(0.6156614753256584, 0.05496885144405574, 0.7860911990162179),
                        new Vector3d(882.8868171968555, 588.991395095908, 254.78133011679319))
        );
    }

    @ParameterizedTest
    @MethodSource("planeArgumentProvider")
    void createPlane(Vector3d point, Vector3d normal, Vector3d expectedCentroid) {

//        // Create matching plane settings
//        PlaneSettings planeSettings = new PlaneSettings();
//        planeSettings.name = "testPlane";
//        planeSettings.point = point;
//        planeSettings.normal = normal;
//        planeSettings.transparency = 0.8f;
//        planeSettings.color = new Color3f(1, 0, 0);
//        planeSettings.isVisible = false;
//
//        Plane plane = planeCreator.createPlane(planeSettings);
//
//        assertionsForPlane(planeSettings, plane, expectedCentroid);
//        assertionsFor3DMesh(planeSettings, plane);
    }

    @ParameterizedTest
    @MethodSource("planeArgumentProvider")
    void createBlockPlane(Vector3d point, Vector3d normal, Vector3d expectedCentroid) {

//        // Create matching block plane settings
//        BlockPlaneSettings blockPlaneSettings = new BlockPlaneSettings();
//        blockPlaneSettings.name = "testBlockPlane";
//        blockPlaneSettings.point = point;
//        blockPlaneSettings.normal = normal;
//        blockPlaneSettings.transparency = 0.8f;
//        blockPlaneSettings.color = new Color3f(1, 0, 0);
//        blockPlaneSettings.isVisible = false;
//
//        BlockPlane blockPlane = planeCreator.createBlockPlane(blockPlaneSettings);
//
//        assertionsForPlane(blockPlaneSettings, blockPlane, expectedCentroid);
//        assertNotNull(blockPlane.getVertexDisplay());
//        assertionsFor3DMesh(blockPlaneSettings, blockPlane);
    }

    @Test
    void updatePlaneOrientation() {

//        // Create a plane for the initial orientation
//        PlaneSettings planeSettings = new PlaneSettings();
//        planeSettings.name = "testPlane";
//        planeSettings.point = new Vector3d(1248.2709228163537, 78.08437737298017, 116.87559032668315);
//        planeSettings.normal = new Vector3d(-0.39675171761579714, -0.014019244058793784, -0.9178189011809109);
//        Plane plane = planeCreator.createPlane(planeSettings);
//
//        // Update plane orientation + check done correctly
//        Vector3d newNormal = new Vector3d(0.6156614753256584, 0.05496885144405574, 0.7860911990162179);
//        Vector3d newPoint = new Vector3d(0.6618597935512298, 37.48411448950936, 984.2992808834338);
//        Vector3d expectedCentroid = new Vector3d(882.8868171968555, 588.991395095908, 254.78133011679319);
//        planeCreator.updatePlaneOrientation( plane, newNormal, newPoint );
//
//        assertEquals(plane.getNormal(), newNormal);
//        assertEquals(plane.getPoint(), newPoint);
//        assertEquals(plane.getCentroid(), expectedCentroid);
//
//        assertionsFor3DMesh(plane.getSettings(), plane);
    }
}