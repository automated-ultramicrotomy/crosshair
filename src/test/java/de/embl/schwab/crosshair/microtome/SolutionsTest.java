package de.embl.schwab.crosshair.microtome;

import com.google.gson.Gson;
import de.embl.schwab.crosshair.io.Settings;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

class SolutionsTest {

    private Solutions solutions;
    private Settings crosshairSettings;

    @BeforeEach
    public void setUp() throws IOException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        File exampleJson = new File(classLoader.getResource("exampleBlock.json").getFile());
        try ( FileReader fileReader = new FileReader( exampleJson ) ) {
            Gson gson = new Gson();
            crosshairSettings = gson.fromJson(fileReader, Settings.class);
            this.solutions = new Solutions();
        }
    }

    // @org.junit.jupiter.api.Test
    // void testSolutionCalculation() {
    //     double solutionRotation = 15.0;
    //     double initialTiltAngle = 5.0;
    //     double initialKnifeAngle = 10.0;
    //     solutions.setSolutionFromRotation( solutionRotation, initialTiltAngle, initialKnifeAngle, crosshairSettings );
    //
    //     assertEquals( solutions.getSolutionRotation(), solutionRotation );
    //     assertEquals( solutions.getSolutionTilt(), -9.8089, 0.00005 );
    //     assertEquals( solutions.getSolutionKnife(), 14.9449, 0.00005 );
    //     assertEquals( solutions.getDistanceToCut(), 210.8122, 0.00005 );
    //     assertEquals( solutions.getSolutionFirstTouchName(), "Bottom Right" );
    // }

    // @org.junit.jupiter.api.Test
    // void testSolutionCalculation2() {
    //     double solutionRotation = -100.0;
    //     double initialTiltAngle = 5.0;
    //     double initialKnifeAngle = -5.0;
    //     solutions.setSolutionFromRotation( solutionRotation, initialTiltAngle, initialKnifeAngle, crosshairSettings );
    //
    //     assertEquals( solutions.getSolutionRotation(), solutionRotation );
    //     assertEquals( solutions.getSolutionTilt(), 3.0567, 0.00005 );
    //     assertEquals( solutions.getSolutionKnife(), 4.6144, 0.00005 );
    //     assertEquals( solutions.getDistanceToCut(), 204.3437, 0.00005 );
    //     assertEquals( solutions.getSolutionFirstTouchName(), "Bottom Right" );
    // }
}