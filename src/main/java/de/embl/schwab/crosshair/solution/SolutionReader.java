package de.embl.schwab.crosshair.solution;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.embl.schwab.crosshair.io.serialise.VertexPointDeserializer;
import de.embl.schwab.crosshair.points.VertexPoint;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class SolutionReader {
    public SolutionReader() {}

    public Solution readSolution( String filePath ) {
        Gson gson = new GsonBuilder().
                registerTypeAdapter( new TypeToken<VertexPoint>(){}.getType(), new VertexPointDeserializer() ).create();

        try {
            FileReader fileReader = new FileReader(filePath);
            return gson.fromJson(fileReader, Solution.class);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return null;
        }
    }
}
