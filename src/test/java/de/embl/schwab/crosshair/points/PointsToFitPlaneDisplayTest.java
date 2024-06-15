package de.embl.schwab.crosshair.points;

import bdv.util.*;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.schwab.crosshair.solution.SolutionReader;
import de.embl.schwab.crosshair.ui.command.OpenCrosshairFromBdvXmlCommand;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.RealPoint;
import net.imglib2.type.numeric.ARGBType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vib.BenesNamedPoint;

import java.io.File;
import java.util.ArrayList;

import static de.embl.cba.tables.ij3d.UniverseUtils.addSourceToUniverse;
import static org.junit.jupiter.api.Assertions.*;

class PointsToFitPlaneDisplayTest {

    private PointsToFitPlaneDisplay pointsToFitPlaneDisplay;
    private Content imageContent;
    private BdvHandle bdvHandle;

    @BeforeEach
    public void setUp() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        File blockFile = new File(classLoader.getResource("exampleBlock.xml").getFile());
        String blockXmlPath = blockFile.getAbsolutePath();

        final LazySpimSource imageSource = new LazySpimSource("raw", blockXmlPath);
//        https://github.com/bigdataviewer/bigdataviewer-vistools/blob/master/src/main/java/bdv/util/BdvHandleFrame.java
        BdvSource bdvSource = BdvFunctions.show(imageSource, 1);
        bdvHandle = bdvSource.getBdvHandle();
        Image3DUniverse universe = new Image3DUniverse();
        universe.show();

        // Set to arbitrary colour
        ARGBType colour =  new ARGBType( ARGBType.rgba( 0, 0, 0, 0 ) );
        imageContent = addSourceToUniverse(universe, imageSource, 300 * 300 * 300,
                Content.VOLUME, colour, 0.7f, 0, 255 );

        String name = "test";
        pointsToFitPlaneDisplay = new PointsToFitPlaneDisplay(name, bdvSource, imageContent);
    }

    @Test
    void addPointToFitPlane() {
        double x = 429.04;
        double y = 684.83;
        double z = 398.32;
        RealPoint point = new RealPoint(x, y, z);
        pointsToFitPlaneDisplay.addPointToFitPlane( point );

        BenesNamedPoint point3d = imageContent.getPointList().get(0);
        assertEquals(x, point3d.x);
        assertEquals(y, point3d.y);
        assertEquals(z, point3d.z);

//        bdvHandle.bdv
//        bdvHandle.bdvSources.get(1)

        assertEquals(pointsToFitPlaneDisplay.getPointsToFitPlane().get(0), point);

//        pointsToFitPlaneDisplay.getPoint2dOverlay().

    }
}