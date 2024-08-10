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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Class to read plane / image settings from .json files
 */
public class SettingsReader {

    private static final Logger logger = LoggerFactory.getLogger(SettingsReader.class);

    public SettingsReader() {}

    /**
     * Read settings from .json file
     * @param filePath Path of .json file
     * @return settings
     */
    public Settings readSettings(String filePath ) {
        Gson gson = new GsonBuilder().
                registerTypeAdapter( new TypeToken<Map<String, PlaneSettings>>(){}.getType(), new PlaneSettingsMapDeserializer()).
                registerTypeAdapter( new TypeToken<VertexPoint>(){}.getType(), new VertexPointAdapter() ).create();

        try ( FileReader fileReader = new FileReader(filePath) ) {
            return gson.fromJson(fileReader, Settings.class);
        } catch (IOException e1) {
            logger.error("Error reading settings file", e1);
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

    /**
     * Set state of microtome manager, plane manager, displayed objects in 3D viewer etc from settings
     * @param settings settings to load
     * @param microtomeManager microtome manager
     * @param microtomePanel microtome panel
     * @param planeManager plane manaager
     * @param imageNameToContent map of image name to its displayed content in the 3D viewer
     * @param otherPanel other panel
     */
    public void loadSettings( Settings settings, MicrotomeManager microtomeManager, MicrotomePanel microtomePanel,
                              PlaneManager planeManager, Map<String, Content> imageNameToContent, OtherPanel otherPanel ) {
        if ( microtomeManager.isMicrotomeModeActive() ) {
            microtomePanel.exitMicrotomeMode();
        }

        loadSettings( settings, planeManager, imageNameToContent, otherPanel );
    }

    /**
     * Set state of microtome manager, plane manager, displayed objects in 3D viewer etc. from settings
     * @param settings settings to load
     * @param planeManager plane manager
     * @param imageNameToContent map of image name to its displayed content in the 3D viewer
     * @param otherPanel other panel
     */
    public void loadSettings( Settings settings, PlaneManager planeManager,
                              Map<String, Content> imageNameToContent, OtherPanel otherPanel ) {
        if (planeManager.isTrackingPlane()) {
            IJ.log("Cant load settings when tracking a plane");
            return;
        }

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
    }

}
