package develop;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import com.google.gson.Gson;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.schwab.crosshair.PlaneManager;
import de.embl.schwab.crosshair.io.SettingsToSave;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij3d.Content;
import ij3d.Image3DUniverse;
import itc.converters.AffineTransform3DToFlatString;
import itc.converters.ElastixAffine3DToAffineTransform3D;
import itc.converters.ElastixEuler3DToAffineTransform3D;
import itc.transforms.elastix.ElastixAffineTransform3D;
import itc.transforms.elastix.ElastixEulerTransform3D;
import itc.transforms.elastix.ElastixTransform;
import itc.utilities.TransformUtils;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.vecmath.Vector3d;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import static de.embl.cba.tables.ij3d.UniverseUtils.addSourceToUniverse;

public class TestDisplayAll {

    public static void main( String[] args ) {

//        final LazySpimSource afterCrop = new LazySpimSource("after_crop", "Z:\\schwab\\microCT\\PLATY_K1_2K_1um_registration\\cropped_data\\crop_after_record\\after_crop.xml");
        final LazySpimSource beforeCrop = new LazySpimSource("before_crop", "Z:\\schwab\\microCT\\PLATY_K1_2K_1um_registration\\cropped_data\\crop_before_record\\before_crop.xml");
        final LazySpimSource elastix = new LazySpimSource("after_elastix", "Z:\\schwab\\microCT\\PLATY_K1_2K_1um_registration\\elastix_recorded_after_to_before_cropped\\euler\\transformed.xml");
        String beforePlanes = "Z:\\schwab\\microCT\\PLATY_K1_2K_1um_registration\\cropped_data\\transformed_before_planes.json";
        String afterPlanes = "Z:\\schwab\\microCT\\PLATY_K1_2K_1um_registration\\elastix_recorded_after_to_before_cropped\\euler\\fitted_planes.json";


        BdvStackSource bdvStackSource = BdvFunctions.show(beforeCrop, 1);
        BdvFunctions.show(elastix, 1, BdvOptions.options().addTo(bdvStackSource));
        bdvStackSource.setDisplayRange(0, 255);

        Image3DUniverse universe = new Image3DUniverse();
        universe.show();


        // Set to arbitrary colour
        ARGBType colour =  new ARGBType( ARGBType.rgba( 0, 0, 0, 0 ) );
        Content beforeContent = addSourceToUniverse(universe, beforeCrop, 300 * 300 * 300, Content.VOLUME, colour, 0.7f, 0, 255 );
        Content afterContent = addSourceToUniverse(universe, elastix, 300 * 300 * 300, Content.VOLUME, colour, 0.7f, 0, 255 );
        // Reset colour to default for 3D viewer
        beforeContent.setColor(null);
        afterContent.setColor(null);

        PlaneManager planeManager = new PlaneManager(bdvStackSource, universe, beforeContent);

        Gson gson = new Gson();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(beforePlanes);
            SettingsToSave beforeSettings = gson.fromJson(fileReader, SettingsToSave.class);
            fileReader = new FileReader(afterPlanes);
            SettingsToSave afterSettings = gson.fromJson(fileReader, SettingsToSave.class);
            Vector3d beforeNormal = beforeSettings.getPlaneNormals().get("target");
            Vector3d afterNormal = afterSettings.getPlaneNormals().get("block");
            planeManager.updatePlane(beforeNormal,  beforeSettings.getPlanePoints().get("target"), "before_target");
            planeManager.updatePlane(afterNormal,  afterSettings.getPlanePoints().get("block"), "after_block");

            double angle = GeometryUtils.convertToDegrees(beforeNormal.angle(afterNormal));
            if (angle > 90) {
                angle = 180 - angle;
            }
            System.out.println(angle);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }



    }
}