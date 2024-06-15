package de.embl.schwab.crosshair.solution;

import de.embl.schwab.crosshair.points.VertexPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SolutionWriterTest {

    @Test
    void writeSolution( @TempDir Path tempDir ) {

        double initialKnifeAngle = 10.0 ;
        double initialTiltAngle = 5.0;
        double knife = 3.0;
        double tilt = 8.0;
        double rotation = 12.0;
        VertexPoint firstTouch = VertexPoint.BottomLeft;
        double distanceToCut = 100.0;
        String distanceUnit = "microns";

        File newJson = tempDir.resolve( "newBlock.json" ).toFile();
        Solution solution = new Solution(
                initialKnifeAngle, initialTiltAngle, knife,
                tilt, rotation, firstTouch, distanceToCut, distanceUnit);

        SolutionWriter solutionWriter = new SolutionWriter(solution, newJson.getAbsolutePath());
        solutionWriter.writeSolution();

        assertTrue( newJson.exists() );

        Solution newSolution = new SolutionReader().readSolution( newJson.getAbsolutePath() );
        assertEquals(newSolution.getInitialKnifeAngle(), initialKnifeAngle);
        assertEquals(newSolution.getInitialTiltAngle(), initialTiltAngle);
        assertEquals(newSolution.getKnife(), knife);
        assertEquals(newSolution.getTilt(), tilt);
        assertEquals(newSolution.getRotation(), rotation);
        assertEquals(newSolution.getFirstTouch(), firstTouch);
        assertEquals(newSolution.getDistanceToCut(), distanceToCut);
        assertEquals(newSolution.getDistanceUnit(), distanceUnit);
    }
}