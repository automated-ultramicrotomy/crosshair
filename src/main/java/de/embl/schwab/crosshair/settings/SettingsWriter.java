package de.embl.schwab.crosshair.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.embl.schwab.crosshair.plane.Plane;
import de.embl.schwab.crosshair.plane.PlaneManager;
import ij3d.Content;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingsWriter {

    public SettingsWriter() { }

    public Settings createSettings(PlaneManager planeManager, Map<String, Content> imageNameToContent ) {
        Settings settings = new Settings();

        List<PlaneSettings> planeSettings = new ArrayList<>();
        for ( Plane plane: planeManager.getPlanes() ) {
            planeSettings.add( plane.getSettings() );
        }
        settings.planeSettings = planeSettings;

        List<ImageContentSettings> imageSettings = new ArrayList<>();
        for ( String imageName: imageNameToContent.keySet() ) {

            Content imageContent = imageNameToContent.get( imageName );

            // transfer function settings
            int[] redLut = new int[256];
            int[] greenLut = new int[256];
            int[] blueLut = new int[256];
            int[] alphaLut = new int[256];

            imageContent.getRedLUT(redLut);
            imageContent.getGreenLUT(greenLut);
            imageContent.getBlueLUT(blueLut);
            imageContent.getAlphaLUT(alphaLut);

            imageSettings.add(
                    new ImageContentSettings( imageName, imageContent.getTransparency(),
                            imageContent.getColor(), redLut, greenLut, blueLut, alphaLut ) ) ;
        }

        settings.imageSettings = imageSettings;

        return settings;
    }

    public void writeSettings( Settings settings, String filePath ) {
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson( settings, fileWriter);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
