package de.embl.schwab.crosshair.io;

import com.google.gson.*;
import de.embl.schwab.crosshair.plane.BlockPlane;
import de.embl.schwab.crosshair.plane.BlockPlaneSettings;
import de.embl.schwab.crosshair.plane.Plane;
import de.embl.schwab.crosshair.plane.PlaneSettings;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaneSettingsListDeserializer implements JsonDeserializer<List<PlaneSettings>>
{

    @Override
    public List<PlaneSettings> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
    {
        List<PlaneSettings> planeSettingsList = new ArrayList<>();

        for ( JsonElement jElement: json.getAsJsonArray() ) {
            JsonObject jObject = jElement.getAsJsonObject();
            PlaneSettings planeSettings;
            if ( jObject.has("vertices") ) {
                planeSettings =  context.deserialize( jObject, BlockPlaneSettings.class);
            } else {
                planeSettings = context.deserialize( jObject, PlaneSettings.class );
            }
            planeSettingsList.add( planeSettings );
        }

        return planeSettingsList;
    }
}
