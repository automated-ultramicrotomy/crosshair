package de.embl.schwab.crosshair.plane;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import customnode.CustomMeshNode;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.PlaneSettings;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.type.numeric.ARGBType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3d;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import static de.embl.cba.tables.ij3d.UniverseUtils.addSourceToUniverse;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class PlaneCreatorTest {

    private PlaneCreator planeCreator;
    private Image3DUniverse universe;
    private Bdv bdvHandle;
    private Point3d min;
    private Point3d max;

    @BeforeEach
    void setUp() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        File imageFile = new File(classLoader.getResource("exampleBlock.xml").getFile());
        final LazySpimSource imageSource = new LazySpimSource("raw", imageFile.getAbsolutePath());

        BdvStackSource bdvStackSource = BdvFunctions.show(imageSource, 1);
        bdvHandle = bdvStackSource.getBdvHandle();
        universe = new Image3DUniverse();
        universe.show();

        // Set to arbitrary colour
        ARGBType colour =  new ARGBType( ARGBType.rgba( 0, 0, 0, 0 ) );
        Content imageContent = addSourceToUniverse(universe, imageSource, 300 * 300 * 300,
                Content.VOLUME, colour, 0, 0, 255 );
        imageContent.setColor(null);

        min = new Point3d();
        imageContent.getMin(min);

        max = new Point3d();
        imageContent.getMax(max);

        planeCreator = new PlaneCreator(universe, imageContent, bdvStackSource);
    }

    @AfterEach
    void tearDown() {
        universe.close();
        bdvHandle.close();
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

        // Check all points in the mesh lie on the given plane (within 1E-5 to account for precision errors - the mesh
        // stores points as floats rather than doubles), and are within the content bounds
        CustomMeshNode meshNode = (CustomMeshNode) planeMesh.getContent();
        List<Point3f> meshPoints = meshNode.getMesh().getMesh();
        for (Point3f meshPoint: meshPoints) {
            assertTrue(GeometryUtils.distanceFromPointToPlane(
                    new Vector3d(meshPoint), planeSettings.normal, planeSettings.point) < 1E-5);
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
                        new Vector3d(607.3829956054688, 607.3829956054688, 399.9140783532721))
        );
    }

    @ParameterizedTest
    @MethodSource("planeArgumentProvider")
    void createPlane(Vector3d point, Vector3d normal, Vector3d expectedCentroid) {

        // Create matching plane settings
        PlaneSettings planeSettings = new PlaneSettings();
        planeSettings.name = "testPlane";
        planeSettings.point = point;
        planeSettings.normal = normal;
        planeSettings.transparency = 0.8f;
        planeSettings.color = new Color3f(1, 0, 0);
        planeSettings.isVisible = false;

        Plane plane = planeCreator.createPlane(planeSettings);

        assertionsForPlane(planeSettings, plane, expectedCentroid);
        assertionsFor3DMesh(planeSettings, plane);
    }

    @ParameterizedTest
    @MethodSource("planeArgumentProvider")
    void createBlockPlane(Vector3d point, Vector3d normal, Vector3d expectedCentroid) {

        // Create matching block plane settings
        BlockPlaneSettings blockPlaneSettings = new BlockPlaneSettings();
        blockPlaneSettings.name = "testBlockPlane";
        blockPlaneSettings.point = point;
        blockPlaneSettings.normal = normal;
        blockPlaneSettings.transparency = 0.8f;
        blockPlaneSettings.color = new Color3f(1, 0, 0);
        blockPlaneSettings.isVisible = false;

        BlockPlane blockPlane = planeCreator.createBlockPlane(blockPlaneSettings);

        assertionsForPlane(blockPlaneSettings, blockPlane, expectedCentroid);
        assertNotNull(blockPlane.getVertexDisplay());
        assertionsFor3DMesh(blockPlaneSettings, blockPlane);
    }

//    @Test
//    void updatePlaneOrientation() {
//    }
}