package de.embl.schwab.crosshair.io.serialise;

import com.google.gson.*;
import de.embl.schwab.crosshair.points.VertexPoint;

import java.lang.reflect.Type;

public class VertexPointAdapter implements JsonDeserializer<VertexPoint>, JsonSerializer<VertexPoint>
{

    @Override
    public VertexPoint deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
    {
        return VertexPoint.fromString( json.getAsString() );
    }

    @Override
    public JsonElement serialize( VertexPoint vertexPoint, Type typeOfSrc, JsonSerializationContext context ) {
        return new JsonPrimitive( vertexPoint.toString() );
    }
}
