package de.embl.schwab.crosshair.io.serialise;

import com.google.gson.*;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.PlaneSettings;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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
