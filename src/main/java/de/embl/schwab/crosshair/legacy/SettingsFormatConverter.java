package de.embl.schwab.crosshair.legacy;

import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.settings.ImageContentSettings;
import de.embl.schwab.crosshair.settings.Settings;
import de.embl.schwab.crosshair.settings.SettingsWriter;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.PlaneSettings;
import de.embl.schwab.crosshair.points.VertexPoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to convert old format Crosshair settings files to the new format
 */
public class SettingsFormatConverter {

    private final File oldFormatJson;
    private final File newFormatJson;

    /**
     * Create a settings format converter
     * @param oldFormatJson Settings json file in old format to convert
     * @param newFormatJson Settings json file to write new format settings into
     */
    public SettingsFormatConverter( File oldFormatJson, File newFormatJson ) {
        this.oldFormatJson = oldFormatJson;
        this.newFormatJson = newFormatJson;
    }

    private Map<String, PlaneSettings> makePlaneSettings( OldFormatSettings oldSettings ) {
        Map<String, PlaneSettings> planeSettings = new HashMap<>();

        // Default to using 'microns' as the unit - this will set distanceBetweenPlanesThreshold to the default
        // with no conversion
        PlaneSettings targetSettings = new PlaneSettings("microns");
        BlockPlaneSettings blockSettings = new BlockPlaneSettings("microns");

        targetSettings.name = Crosshair.target;
        blockSettings.name = Crosshair.block;
        blockSettings.pointsToFitPlane = oldSettings.points;
        blockSettings.vertices = oldSettings.blockVertices;

        blockSettings.assignedVertices = new HashMap<>();
        if ( oldSettings.namedVertices.size() > 0 ) {
            for ( String vertexName: oldSettings.namedVertices.keySet() ) {
                blockSettings.assignedVertices.put( VertexPoint.fromString( vertexName ),
                        oldSettings.namedVertices.get( vertexName ) );
            }
        }

        targetSettings.color = oldSettings.targetPlaneColour;
        blockSettings.color = oldSettings.blockPlaneColour;
        targetSettings.transparency = oldSettings.targetTransparency;
        blockSettings.transparency = oldSettings.blockTransparency;

        for ( PlaneSettings settings: new PlaneSettings[]{targetSettings, blockSettings} ) {
            if (oldSettings.planePoints.containsKey( settings.name )) {
                settings.point = oldSettings.planePoints.get( settings.name );
            }
            if (oldSettings.planeNormals.containsKey( settings.name )) {
                settings.normal = oldSettings.planeNormals.get( settings.name );
            }
            settings.isVisible = true;
        }

        planeSettings.put( Crosshair.target, targetSettings );
        planeSettings.put( Crosshair.block, blockSettings );

        return planeSettings;
    }

    private Map<String, ImageContentSettings> makeImageSettings( OldFormatSettings oldFormatSettings ) {
        Map<String, ImageContentSettings> imageNameToSettings = new HashMap<>();

        ImageContentSettings settings = new ImageContentSettings( Crosshair.image, oldFormatSettings.imageTransparency,
                oldFormatSettings.imageColour, oldFormatSettings.redLut, oldFormatSettings.greenLut,
                oldFormatSettings.blueLut, oldFormatSettings.alphaLut );

        imageNameToSettings.put( Crosshair.image, settings );

        return imageNameToSettings;
    }

    /**
     * Reads settings from the old format settings json file, converts them to the new format,
     * then writes to the new format settings json file
     */
    public void convertOldSettingsToNew() {
        OldFormatSettings oldSettings = new OldFormatSettingsReader().readSettings( oldFormatJson.getAbsolutePath() );
        Settings settings = new Settings();
        settings.planeNameToSettings = makePlaneSettings( oldSettings );
        settings.imageNameToSettings = makeImageSettings( oldSettings );

        new SettingsWriter().writeSettings( settings, newFormatJson.getAbsolutePath() );
    }
}
