package de.embl.schwab.crosshair.plane;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.PlaneSettings;
import dotty.tools.dotc.transform.PatternMatcher;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.type.numeric.ARGBType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;
import scala.concurrent.impl.FutureConvertersImpl;

import java.io.File;
import java.util.ArrayList;

import static de.embl.cba.tables.ij3d.UniverseUtils.addSourceToUniverse;
import static org.junit.jupiter.api.Assertions.*;

class PlaneManagerTest {

    private PlaneManager planeManager;
    private Image3DUniverse universe;
    private Bdv bdvHandle;
    private Point3d min;
    private Point3d max;
    private Vector3d normal;
    private Vector3d point;

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

        // Example point and normal for testing
        point = new Vector3d(-188.47561306126704, 15.353222856645068, 621.5211240735744);
        normal = new Vector3d(0.20791169081775954, 0.08525118065879457, 0.9744254538021788);

        planeManager = new PlaneManager(bdvStackSource, universe, imageContent);
    }

    @AfterEach
    void tearDown() {
        universe.close();
        bdvHandle.close();
    }

    @Test
    void getPlane() {
    }

    @Test
    void getBlockPlane() {
    }

    @Test
    void getPlaneNames() {
    }

    @Test
    void getPlanes() {
    }

    @Test
    void isTrackingPlane() {
    }

    @Test
    void setTrackingPlane() {
    }

    @Test
    void setTrackedPlaneName() {
    }

    @Test
    void getTrackedPlaneName() {
    }

    @Test
    void isInPointMode() {
    }

    @Test
    void setPointMode() {
    }

    @Test
    void isInVertexMode() {
    }

    @Test
    void setVertexMode() {
    }

    @Test
    void checkNamedPlaneExists() {
    }

    @Test
    void checkNamedPlaneExistsAndOrientationIsSet() {
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
    void getPointsToFitPlaneDisplay() {
    }

    @Test
    void getVertexDisplay() {
    }

    @Test
    void updatePlane() {
    }

    @Test
    void setPlaneColourToAligned() {
    }

    @Test
    void setPlaneColourToUnaligned() {
    }

    @Test
    void updatePlaneOnTransformChange() {
    }

    @Test
    void updatePlaneCurrentView() {
    }

    @Test
    void redrawCurrentPlanes() {
    }

    @Test
    void fitToPoints() {
    }

    @Test
    void getAll2dPointOverlays() {
    }

    @Test
    void getPlaneDefinitionOfCurrentView() {
    }

    @Test
    void getGlobalViewCentre() {
    }

    @Test
    void moveViewToNamedPlane() {
    }

    @Test
    void removeNamedPlane() {
    }
}