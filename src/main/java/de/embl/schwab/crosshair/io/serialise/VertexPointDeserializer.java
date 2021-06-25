package de.embl.schwab.crosshair.io.serialise;

import com.google.gson.*;
import de.embl.schwab.crosshair.points.VertexPoint;

import java.lang.reflect.Type;

public class VertexPointDeserializer implements JsonDeserializer<VertexPoint>
{

    @Override
    public VertexPoint deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
    {
        return VertexPoint.fromString( json.getAsString() );
    }
}
