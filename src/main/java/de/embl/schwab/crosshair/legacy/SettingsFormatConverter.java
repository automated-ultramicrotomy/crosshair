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

public class SettingsFormatConverter {

    private final File oldFormatJson;
    private final File newFormatJson;

    public SettingsFormatConverter( File oldFormatJson, File newFormatJson ) {
        this.oldFormatJson = oldFormatJson;
        this.newFormatJson = newFormatJson;
    }

    private Map<String, PlaneSettings> makePlaneSettings( OldFormatSettings oldSettings ) {
        Map<String, PlaneSettings> planeSettings = new HashMap<>();

        PlaneSettings targetSettings = new PlaneSettings();
        BlockPlaneSettings blockSettings = new BlockPlaneSettings();

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

    public void convertOldSettingsToNew() {
        OldFormatSettings oldSettings = new OldFormatSettingsReader().readSettings( oldFormatJson.getAbsolutePath() );
        Settings settings = new Settings();
        settings.planeNameToSettings = makePlaneSettings( oldSettings );
        settings.imageNameToSettings = makeImageSettings( oldSettings );

        new SettingsWriter().writeSettings( settings, newFormatJson.getAbsolutePath() );
    }
}
