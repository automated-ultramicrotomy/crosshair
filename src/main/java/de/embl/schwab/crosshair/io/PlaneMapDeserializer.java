package de.embl.schwab.crosshair.io;

import com.google.gson.*;
import de.embl.schwab.crosshair.plane.BlockPlane;
import de.embl.schwab.crosshair.plane.Plane;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class PlaneMapDeserializer implements JsonDeserializer<Map<String, Plane>>
{

    @Override
    public Map<String, Plane> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
    {
        Map<String, Plane> planeMap = new HashMap<>();
        JsonObject jobject = json.getAsJsonObject();

        for ( String planeName: jobject.keySet() ) {
            JsonObject planeObject = jobject.get( planeName ).getAsJsonObject();
            Plane plane;
            if ( planeObject.has("vertices") ) {
                plane =  context.deserialize( planeObject, BlockPlane.class);
            } else {
                plane = context.deserialize( planeObject, Plane.class );
            }
            planeMap.put( planeName, plane );
        }

        return planeMap;
    }
}
