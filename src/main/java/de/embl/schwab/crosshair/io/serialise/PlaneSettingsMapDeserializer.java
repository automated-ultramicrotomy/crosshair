package de.embl.schwab.crosshair.io.serialise;

import com.google.gson.*;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.PlaneSettings;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class PlaneSettingsMapDeserializer implements JsonDeserializer<Map<String, PlaneSettings>>
{

    @Override
    public Map<String, PlaneSettings> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
    {
        Map<String, PlaneSettings> planeSettingsMap = new HashMap<>();
        JsonObject jobject = json.getAsJsonObject();

        for ( String planeName: jobject.keySet() ) {
            JsonObject planeSettingsObject = jobject.get( planeName ).getAsJsonObject();
            PlaneSettings planeSettings;
            if ( planeSettingsObject.has("vertices") ) {
                planeSettings =  context.deserialize( planeSettingsObject, BlockPlaneSettings.class);
            } else {
                planeSettings = context.deserialize( planeSettingsObject, PlaneSettings.class );
            }
            planeSettingsMap.put( planeName, planeSettings );
        }

        return planeSettingsMap;
    }
}
