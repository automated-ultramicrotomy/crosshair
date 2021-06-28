package de.embl.schwab.crosshair.solution;

import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.microtome.Microtome;
import de.embl.schwab.crosshair.microtome.TargetOffsetAndTilt;
import de.embl.schwab.crosshair.plane.BlockPlane;
import de.embl.schwab.crosshair.plane.Plane;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.points.VertexDisplay;
import de.embl.schwab.crosshair.points.VertexPoint;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.Settings;
import de.embl.schwab.crosshair.settings.SettingsReader;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SolutionsCalculatorTest {

    private Settings crosshairSettings;
    private TargetOffsetAndTilt targetOffsetAndTilt;
    private Plane targetPlane;
    private BlockPlane blockPlane;
    private PlaneManager planeManager;

    @BeforeEach
    public void setUp() throws IOException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        File exampleJson = new File(classLoader.getResource("exampleBlock.json").getFile());

        SettingsReader settingsReader = new SettingsReader();
        crosshairSettings = settingsReader.readSettings( exampleJson.getAbsolutePath() );

        BlockPlaneSettings blockPlaneSettings = (BlockPlaneSettings) crosshairSettings.planeNameToSettings.get( Crosshair.block );
        targetOffsetAndTilt = new TargetOffsetAndTilt( blockPlaneSettings.assignedVertices, blockPlaneSettings.normal,
                crosshairSettings.planeNameToSettings.get( Crosshair.target ).normal );

        targetPlane = new Plane( crosshairSettings.planeNameToSettings.get( Crosshair.target ), null, null, null );
        blockPlane = new BlockPlane( (BlockPlaneSettings) crosshairSettings.planeNameToSettings.get( Crosshair.block ),
                null, null, null, null );

        planeManager = createMockPlaneManager( createMockVertexDisplay() );
    }

    private VertexDisplay createMockVertexDisplay() {
        VertexDisplay vertexDisplay = mock( VertexDisplay.class );
        BlockPlaneSettings blockPlaneSettings = (BlockPlaneSettings) crosshairSettings.planeNameToSettings.get( Crosshair.block );
        when( vertexDisplay.getAssignedVertices() ).thenReturn( blockPlaneSettings.assignedVertices );

        return vertexDisplay;
    }

    private PlaneManager createMockPlaneManager( VertexDisplay vertexDisplay ) {

        PlaneManager planeManager = mock( PlaneManager.class );
        when( planeManager.getPlane( Crosshair.target ) ).thenReturn( targetPlane );
        when( planeManager.getPlane( Crosshair.block ) ).thenReturn( blockPlane );
        when( planeManager.getVertexDisplay(Crosshair.block) ).thenReturn( vertexDisplay );

        return planeManager;
    }

    private Microtome createMockMicrotome( double initialTiltAngle, double initialKnifeAngle ) {
        Microtome microtome = mock( Microtome.class );
        when( microtome.getInitialKnifeAngle() ).thenReturn( initialKnifeAngle );
        when( microtome.getInitialTiltAngle() ).thenReturn( initialTiltAngle );

        when( microtome.getInitialTargetTilt() ).thenReturn( targetOffsetAndTilt.targetTilt );
        when( microtome.getInitialTargetOffset() ).thenReturn( targetOffsetAndTilt.targetOffset );
        when( microtome.getPlaneManager() ).thenReturn( planeManager );

        return microtome;
    }

    @org.junit.jupiter.api.Test
    void setSolutionFromRotation() {
        double solutionRotation = 15.0;
        double initialTiltAngle = 5.0;
        double initialKnifeAngle = 10.0;

        SolutionsCalculator solutionsCalculator = new SolutionsCalculator( createMockMicrotome( initialTiltAngle, initialKnifeAngle ) );
        solutionsCalculator.setSolutionFromRotation( solutionRotation );

        assertEquals( solutionsCalculator.getSolutionRotation(), solutionRotation );
        assertEquals( solutionsCalculator.getSolutionTilt(), -9.8089, 0.00005 );
        assertEquals( solutionsCalculator.getSolutionKnife(), 14.9449, 0.00005 );
        assertEquals( solutionsCalculator.getDistanceToCut(), 210.8122, 0.00005 );
        assertEquals( solutionsCalculator.getSolutionFirstTouchVertexPoint(), VertexPoint.BottomRight );
    }

    @org.junit.jupiter.api.Test
    void setSolutionFromRotation2() {
        double solutionRotation = -100.0;
        double initialTiltAngle = 5.0;
        double initialKnifeAngle = -5.0;

        SolutionsCalculator solutionsCalculator = new SolutionsCalculator( createMockMicrotome( initialTiltAngle, initialKnifeAngle ) );
        solutionsCalculator.setSolutionFromRotation( solutionRotation );

        assertEquals( solutionsCalculator.getSolutionRotation(), solutionRotation );
        assertEquals( solutionsCalculator.getSolutionTilt(), 3.0567, 0.00005 );
        assertEquals( solutionsCalculator.getSolutionKnife(), 4.6144, 0.00005 );
        assertEquals( solutionsCalculator.getDistanceToCut(), 204.3437, 0.00005 );
        assertEquals( solutionsCalculator.getSolutionFirstTouchVertexPoint(), VertexPoint.BottomRight );
    }
}