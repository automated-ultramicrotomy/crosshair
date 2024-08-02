package de.embl.schwab.crosshair.solution;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.embl.schwab.crosshair.io.serialise.VertexPointAdapter;
import de.embl.schwab.crosshair.points.VertexPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;

public class SolutionWriter {

    private static final Logger logger = LoggerFactory.getLogger(SolutionWriter.class);

    private Solution solution;
    private String filePath;

    public SolutionWriter( Solution solution, String filePath ) {
        this.solution = solution;
        this.filePath = filePath;
    }

    public void writeSolution() {
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            Gson gson = new GsonBuilder().setPrettyPrinting().
                    registerTypeAdapter( new TypeToken<VertexPoint>(){}.getType(), new VertexPointAdapter() ).create();
            gson.toJson( solution, fileWriter);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e1) {
            logger.error("Error writing solution", e1);
        }
    }

}
