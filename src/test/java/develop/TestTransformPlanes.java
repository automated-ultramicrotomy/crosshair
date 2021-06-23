package develop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.embl.schwab.crosshair.io.Settings;
import net.imglib2.RealPoint;
import org.scijava.vecmath.Vector3d;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class TestTransformPlanes {

    public static void main( String[] args ) {
        // String filePath = "Z:\\schwab\\microCT\\PLATY_K1_2K_1um\\crosshair\\before_target_flipped_vertical.json";
        // String newPath = "Z:\\schwab\\microCT\\PLATY_K1_2K_1um_registration\\cropped_data\\transformed_before_planes.json";
        // Gson gson = new Gson();
        // try {
        //     FileReader fileReader = new FileReader(filePath);
        //     Settings settingsToSave = gson.fromJson(fileReader, Settings.class);
        //     System.out.println("here");
        //
        //     Vector3d cropTransform = new Vector3d(611, 674, 62);
        //     double[] cropTransformDouble = new double[3];
        //     cropTransform.get(cropTransformDouble);
        //     for (String key: settingsToSave.getPlanePoints().keySet()) {
        //         Vector3d currentVector = settingsToSave.getPlanePoints().get(key);
        //         currentVector.sub(cropTransform);
        //     }
        //
        //     for (RealPoint point: settingsToSave.getBlockVertices()) {
        //         point.move(-cropTransform.getX(), 0);
        //         point.move(-cropTransform.getY(), 1);
        //         point.move(-cropTransform.getZ(), 2);
        //     }
        //
        //     for (RealPoint point: settingsToSave.getPoints()) {
        //         point.move(-cropTransform.getX(), 0);
        //         point.move(-cropTransform.getY(), 1);
        //         point.move(-cropTransform.getZ(), 2);
        //     }
        //
        //     for (String key: settingsToSave.getNamedVertices().keySet()) {
        //         RealPoint point = settingsToSave.getNamedVertices().get(key);
        //         point.move(-cropTransform.getX(), 0);
        //         point.move(-cropTransform.getY(), 1);
        //         point.move(-cropTransform.getZ(), 2);
        //     }
        //
        //     System.out.println("here");
        //
        //     FileWriter fileWriter = new FileWriter(newPath);
        //     Gson gsonOut = new GsonBuilder().setPrettyPrinting().create();
        //     gsonOut.toJson(settingsToSave, fileWriter);
        //     fileWriter.flush();
        //     fileWriter.close();
        //
        // } catch (FileNotFoundException e1) {
        //     e1.printStackTrace();
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }


    }

}
