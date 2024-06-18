package de.embl.schwab.crosshair.plane;

import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.schwab.crosshair.settings.PlaneSettings;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.type.numeric.ARGBType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;

import java.io.File;

import static de.embl.cba.tables.ij3d.UniverseUtils.addSourceToUniverse;
import static org.junit.jupiter.api.Assertions.*;

class PlaneCreatorTest {

    private PlaneCreator planeCreator;
    private Point3d min;
    private Point3d max;

    @BeforeEach
    void setUp() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        File imageFile = new File(classLoader.getResource("exampleBlock.xml").getFile());
        final LazySpimSource imageSource = new LazySpimSource("raw", imageFile.getAbsolutePath());

        BdvStackSource bdvStackSource = BdvFunctions.show(imageSource, 1);
        Image3DUniverse universe = new Image3DUniverse();
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
        Plane plane = planeCreator.createPlane(planeSettings);

        assertEquals(planeSettings.name, plane.getName());


    }

//    @Test
//    void createBlockPlane() {
//    }
//
//    @Test
//    void updatePlaneOrientation() {
//    }
}