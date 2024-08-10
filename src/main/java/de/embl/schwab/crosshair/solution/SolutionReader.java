package de.embl.schwab.crosshair.solution;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.embl.schwab.crosshair.io.serialise.VertexPointAdapter;
import de.embl.schwab.crosshair.points.VertexPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;

/**
 * Class to read a Crosshair solution from a json file.
 */
public class SolutionReader {

    private static final Logger logger = LoggerFactory.getLogger(SolutionReader.class);

    public SolutionReader() {}

    /**
     * Read a Crosshair solution from a json file
     * @param filePath path to json file
     * @return solution
     */
    public Solution readSolution( String filePath ) {
        Gson gson = new GsonBuilder().
                registerTypeAdapter( new TypeToken<VertexPoint>(){}.getType(), new VertexPointAdapter() ).create();

        try ( FileReader fileReader = new FileReader(filePath) ) {
            return gson.fromJson(fileReader, Solution.class);
        } catch (IOException e) {
            logger.error("Error reading solutions file", e);
        }

        return  null;
    }
}
