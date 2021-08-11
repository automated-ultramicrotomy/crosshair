package de.embl.schwab.crosshair.solution;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.embl.schwab.crosshair.io.serialise.VertexPointAdapter;
import de.embl.schwab.crosshair.points.VertexPoint;

import java.io.FileReader;
import java.io.IOException;

public class SolutionReader {
    public SolutionReader() {}

    public Solution readSolution( String filePath ) {
        Gson gson = new GsonBuilder().
                registerTypeAdapter( new TypeToken<VertexPoint>(){}.getType(), new VertexPointAdapter() ).create();

        try ( FileReader fileReader = new FileReader(filePath) ) {
            return gson.fromJson(fileReader, Solution.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return  null;
    }
}
