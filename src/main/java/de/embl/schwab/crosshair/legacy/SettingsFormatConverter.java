package de.embl.schwab.crosshair.legacy;

import com.google.gson.Gson;
import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.settings.ImageContentSettings;
import de.embl.schwab.crosshair.settings.Settings;
import de.embl.schwab.crosshair.settings.SettingsWriter;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.PlaneSettings;
import de.embl.schwab.crosshair.points.VertexPoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class SettingsFormatConverter {

    private final File oldFormatJson;
    private final File newFormatJson;

    public SettingsFormatConverter( File oldFormatJson, File newFormatJson ) {
        this.oldFormatJson = oldFormatJson;
        this.newFormatJson = newFormatJson;
    }

    private OldFormatSettings readOldSettings() throws FileNotFoundException {
        Gson gson = new Gson();
        FileReader fileReader = new FileReader( oldFormatJson.getAbsolutePath() );
        return gson.fromJson(fileReader, OldFormatSettings.class);
    }

    private ArrayList<PlaneSettings> makePlaneSettings( OldFormatSettings oldSettings ) {
        ArrayList<PlaneSettings> planeSettings = new ArrayList<>();

        PlaneSettings targetSettings = new PlaneSettings();
        BlockPlaneSettings blockSettings = new BlockPlaneSettings();

        targetSettings.name = Crosshair.target;
        blockSettings.name = Crosshair.block;
        targetSettings.pointsToFitPlane = oldSettings.points;
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

        planeSettings.add( targetSettings );
        planeSettings.add( blockSettings );

        return planeSettings;
    }

    private ArrayList<ImageContentSettings> makeImageSettings( OldFormatSettings oldFormatSettings ) {
        ArrayList<ImageContentSettings> imageSettings = new ArrayList<>();

        ImageContentSettings settings = new ImageContentSettings( Crosshair.image, oldFormatSettings.imageTransparency,
                oldFormatSettings.imageColour, oldFormatSettings.redLut, oldFormatSettings.greenLut,
                oldFormatSettings.blueLut, oldFormatSettings.alphaLut );

        imageSettings.add( settings );
        return imageSettings;
    }

    public void convertOldSettingsToNew() {
        try {
            OldFormatSettings oldSettings = readOldSettings();
            Settings settings = new Settings();
            settings.planeSettings = makePlaneSettings( oldSettings );
            settings.imageSettings = makeImageSettings( oldSettings );

            new SettingsWriter().writeSettings( settings, newFormatJson.getAbsolutePath() );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
