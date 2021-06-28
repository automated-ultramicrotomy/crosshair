package de.embl.schwab.crosshair.legacy;

import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.points.VertexPoint;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.Settings;
import de.embl.schwab.crosshair.settings.SettingsReader;
import net.imglib2.RealPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;


class SettingsFormatConverterTest {

    @Test
    void convertOldSettingsToNew( @TempDir Path tempDir ) {
        // check conversion of old format settings to new format settings is consistent

        ClassLoader classLoader = this.getClass().getClassLoader();
        File oldJson = new File(classLoader.getResource("legacy/exampleBlock.json").getFile());
        File newJson = tempDir.resolve( "newBlock.json" ).toFile();

        new SettingsFormatConverter( oldJson, newJson ).convertOldSettingsToNew();

        assertTrue( newJson.exists() );

        OldFormatSettings oldSettings = new OldFormatSettingsReader().readSettings( oldJson.getAbsolutePath() );
        Settings newSettings = new SettingsReader().readSettings( newJson.getAbsolutePath() );

        assertEquals( oldSettings.blockTransparency, newSettings.planeNameToSettings.get( Crosshair.block ).transparency );
        assertEquals( oldSettings.imageTransparency, newSettings.imageNameToSettings.get( Crosshair.image ).imageTransparency );
        assertEquals( oldSettings.targetTransparency, newSettings.planeNameToSettings.get( Crosshair.target ).transparency );

        assertEquals(oldSettings.blockPlaneColour, newSettings.planeNameToSettings.get( Crosshair.block ).color );
        assertEquals(oldSettings.targetPlaneColour, newSettings.planeNameToSettings.get( Crosshair.target ).color );
        assertEquals( oldSettings.imageColour, newSettings.imageNameToSettings.get( Crosshair.image ).imageColour );

        assertArrayEquals( oldSettings.alphaLut, newSettings.imageNameToSettings.get( Crosshair.image ).alphaLut );
        assertArrayEquals(oldSettings.redLut, newSettings.imageNameToSettings.get( Crosshair.image ).redLut);
        assertArrayEquals(oldSettings.blueLut, newSettings.imageNameToSettings.get( Crosshair.image ).blueLut );
        assertArrayEquals(oldSettings.greenLut, newSettings.imageNameToSettings.get( Crosshair.image ).greenLut );

        for( String planeName: new String[]{Crosshair.target, Crosshair.block} ) {
            assertEquals( oldSettings.planeNormals.get(planeName), newSettings.planeNameToSettings.get( planeName ).normal );
            assertEquals( oldSettings.planePoints.get(planeName), newSettings.planeNameToSettings.get( planeName ).point );
        }

        assertEquals( oldSettings.points, newSettings.planeNameToSettings.get( Crosshair.block ).pointsToFitPlane );

        BlockPlaneSettings blockPlaneSettings = (BlockPlaneSettings) newSettings.planeNameToSettings.get( Crosshair.block );
        assertEquals( oldSettings.blockVertices, blockPlaneSettings.vertices );

        for( String vertexName: oldSettings.namedVertices.keySet() ) {
            RealPoint vertex = blockPlaneSettings.assignedVertices.get( VertexPoint.fromString( vertexName ) );
            assertEquals( oldSettings.namedVertices.get( vertexName ), vertex );
        }
    }
}