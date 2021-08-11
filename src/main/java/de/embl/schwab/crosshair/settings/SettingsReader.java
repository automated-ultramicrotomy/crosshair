package de.embl.schwab.crosshair.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.embl.schwab.crosshair.io.serialise.PlaneSettingsMapDeserializer;
import de.embl.schwab.crosshair.io.serialise.VertexPointAdapter;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.points.VertexPoint;
import de.embl.schwab.crosshair.ui.swing.MicrotomePanel;
import de.embl.schwab.crosshair.ui.swing.OtherPanel;
import ij.IJ;
import ij3d.Content;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class SettingsReader {

    public SettingsReader() {}

    public Settings readSettings(String filePath ) {
        Gson gson = new GsonBuilder().
                registerTypeAdapter( new TypeToken<Map<String, PlaneSettings>>(){}.getType(), new PlaneSettingsMapDeserializer()).
                registerTypeAdapter( new TypeToken<VertexPoint>(){}.getType(), new VertexPointAdapter() ).create();

        try ( FileReader fileReader = new FileReader(filePath) ) {
            return gson.fromJson(fileReader, Settings.class);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    private void loadImageSettings( Content imageContent, ImageContentSettings imageSettings ) {
        imageContent.setColor( imageSettings.imageColour );
        imageContent.setTransparency( imageSettings.imageTransparency );

        // TODO - if null, set to the default lut, need to look up what this is and recreate it
        if ( imageSettings.redLut != null & imageSettings.greenLut != null &
                imageSettings.blueLut != null & imageSettings.alphaLut != null) {
            // transfer function
            imageContent.setLUT( imageSettings.redLut, imageSettings.greenLut,
                    imageSettings.blueLut, imageSettings.alphaLut );
        }
    }

    public void loadSettings( Settings settings, MicrotomeManager microtomeManager, MicrotomePanel microtomePanel,
                              PlaneManager planeManager, Map<String, Content> imageNameToContent, OtherPanel otherPanel ) {
        if ( microtomeManager.isMicrotomeModeActive() ) {
            microtomePanel.exitMicrotomeMode();
        }

        loadSettings( settings, planeManager, imageNameToContent, otherPanel );
    }

    public void loadSettings( Settings settings, PlaneManager planeManager,
                              Map<String, Content> imageNameToContent, OtherPanel otherPanel ) {

        if ( !planeManager.isTrackingPlane() ) {

            // setup plane settings
            // make a copy, so not modifying as we loop
            ArrayList<String> planeNames = new ArrayList<>( planeManager.getPlaneNames() );
            for ( String planeName : planeNames ) {
                planeManager.removeNamedPlane(planeName);
            }

            for ( PlaneSettings planeSettings: settings.planeNameToSettings.values() ) {
                if ( planeSettings instanceof BlockPlaneSettings) {
                    planeManager.addBlockPlane( (BlockPlaneSettings) planeSettings );
                } else {
                    planeManager.addPlane( planeSettings );
                }
            }

            // setup image settings
            for ( ImageContentSettings imageSettings: settings.imageNameToSettings.values() ) {
                loadImageSettings( imageNameToContent.get( imageSettings.name ), imageSettings );
            }

            if ( !otherPanel.check3DPointsVisible() ) {
                otherPanel.toggleVisiblity3DPoints();
            }
        } else {
            IJ.log("Cant load settings when tracking a plane");
        }

    }

}
