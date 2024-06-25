package de.embl.schwab.crosshair.io.serialise;

import com.google.gson.*;
import de.embl.schwab.crosshair.points.VertexPoint;

import java.lang.reflect.Type;

/**
 * Class to handle json serialization and deserialization for VertexPoint
 */
public class VertexPointAdapter implements JsonDeserializer<VertexPoint>, JsonSerializer<VertexPoint>
{

    /**
     * Deserializes json element to a VertexPoint
     * @param json json element
     * @param typeOfT type of the object to deserialize to
     * @param context json deserialization context
     * @return a VertexPoint object
     * @throws JsonParseException
     */
    @Override
    public VertexPoint deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
    {
        return VertexPoint.fromString( json.getAsString() );
    }

    /**
     * Serialises VertexPoint to a json element
     * @param vertexPoint VertexPoint object
     * @param typeOfSrc the actual type (fully genericized version) of the source object
     * @param context json serialization context
     * @return a json element
     */
    @Override
    public JsonElement serialize( VertexPoint vertexPoint, Type typeOfSrc, JsonSerializationContext context ) {
        return new JsonPrimitive( vertexPoint.toString() );
    }
}
