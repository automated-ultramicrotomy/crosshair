package de.embl.schwab.crosshair.plane;

import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import customnode.CustomMeshNode;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.schwab.crosshair.settings.PlaneSettings;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.type.numeric.ARGBType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3d;

import java.io.File;
import java.util.List;

import static de.embl.cba.tables.ij3d.UniverseUtils.addSourceToUniverse;
import static org.junit.jupiter.api.Assertions.*;

class PlaneCreatorTest {

    private PlaneCreator planeCreator;
    private Image3DUniverse universe;
    private Point3d min;
    private Point3d max;

    @BeforeEach
    void setUp() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        File imageFile = new File(classLoader.getResource("exampleBlock.xml").getFile());
        final LazySpimSource imageSource = new LazySpimSource("raw", imageFile.getAbsolutePath());

        BdvStackSource bdvStackSource = BdvFunctions.show(imageSource, 1);
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

    @Test
    void createPlane() {

        PlaneSettings planeSettings = new PlaneSettings();
        planeSettings.name = "testPlane";
        planeSettings.point = new Vector3d(
                min.x + (max.x - min.x) / 2,
                min.y + (max.y - min.y) / 2,
                min.z + (max.z - min.z) / 2
                );
        planeSettings.normal = new Vector3d(0, 0, 1);
        planeSettings.transparency = 0.8f;
        planeSettings.color = new Color3f(1, 0, 0);
        planeSettings.isVisible = false;

        Plane plane = planeCreator.createPlane(planeSettings);

        assertEquals(planeSettings.name, plane.getName());
        assertEquals(planeSettings.point, plane.getPoint());
        assertEquals(planeSettings.normal, plane.getNormal());
        assertEquals(planeSettings.transparency, plane.getTransparency());
        assertEquals(planeSettings.color, plane.getColor());
        assertEquals(planeSettings.isVisible, plane.isVisible());
        assertNotNull(plane.getPointsToFitPlaneDisplay());

        // As the plane point was set at the centre of the image volume, the centroid should be the same
        assertEquals(planeSettings.point, plane.getCentroid());

        // Check plane mesh is added to 3D viewer with correct settings
        assertTrue(universe.contains(plane.getName()));
        Content planeMesh = universe.getContent(plane.getName());
        assertEquals(planeMesh.isVisible(), planeSettings.isVisible);
        assertEquals(planeMesh.getTransparency(), planeSettings.transparency);
        assertEquals(planeMesh.getColor(), planeSettings.color);

        // Check all points in the mesh lie on the given plane, and are within the content bounds
        CustomMeshNode meshNode = (CustomMeshNode) planeMesh.getContent();
        List<Point3f> meshPoints = meshNode.getMesh().getMesh();
        for (Point3f meshPoint: meshPoints) {
            assertTrue(GeometryUtils.checkPointLiesInPlane(
                    new Vector3d(meshPoint), planeSettings.normal, planeSettings.point
            ));
            assertTrue(meshPoint.x >= min.x && meshPoint.x <= max.x);
            assertTrue(meshPoint.y >= min.y && meshPoint.y <= max.y);
            assertTrue(meshPoint.z >= min.z && meshPoint.z <= max.z);
        }

        // TODO - try a few different planes maybe one aligned, rest at weird orientations - parametrise
        // TODO - add teardown to close viewers etc? Saw some java errors?

    }

//    @Test
//    void createBlockPlane() {
//    }
//
//    @Test
//    void updatePlaneOrientation() {
//    }
}