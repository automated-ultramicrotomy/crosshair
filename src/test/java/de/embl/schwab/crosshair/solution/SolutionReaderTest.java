package de.embl.schwab.crosshair.solution;

import de.embl.schwab.crosshair.points.VertexPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SolutionReaderTest {

    private SolutionReader solutionReader;

    @BeforeEach
    public void setUp() { solutionReader = new SolutionReader(); }

    @Test
    void readSolution() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        File json = new File(classLoader.getResource("exampleBlockSolution.json").getFile());
        Solution solution = solutionReader.readSolution( json.getAbsolutePath() );

        assertEquals(solution.getInitialKnifeAngle(), 10.0);
        assertEquals(solution.getInitialTiltAngle(), 5.0);
        assertEquals(solution.getKnife(), 14.9449);
        assertEquals(solution.getTilt(), -9.8089);
        assertEquals(solution.getRotation(), 15.0);
        assertEquals(solution.getFirstTouch(), VertexPoint.BottomRight);
        assertEquals(solution.getDistanceToCut(), 210.8122);
        assertEquals(solution.getAnglesUnit(), "degrees");
        assertEquals(solution.getDistanceUnit(), "microns");
    }

    @Test
    void readInvalidSolution( @TempDir Path tempDir ) {
        // Disable logging to keep the test logs clean (we're expecting an error here)
        Logger logger = (Logger) LoggerFactory.getLogger(SolutionReader.class);
        Level loggerLevel = logger.getLevel();
        logger.setLevel(Level.OFF);

        File invalidJsonPath = tempDir.resolve( "invalid.json" ).toFile();
        Solution solution = solutionReader.readSolution( invalidJsonPath.getAbsolutePath() );
        assertNull( solution );

        logger.setLevel(loggerLevel);
    }
}