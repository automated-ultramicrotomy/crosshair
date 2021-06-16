package de.embl.schwab.crosshair.ui.command;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.schwab.crosshair.Crosshair;
import ij.IJ;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

import static de.embl.cba.tables.ij3d.UniverseUtils.addSourceToUniverse;

@Plugin(type = Command.class, menuPath = "Plugins>Crosshair>Analysis>Measure Targeting Accuracy" )
public class MeasureTargetingAccuracyCommand implements Command {

    @Parameter(label="Before targeting image xml")
    public File beforeTargetingXml;

    @Parameter(label="Registered after targeting image xml")
    public File registeredAfterTargetingXml;

    @Parameter(label="Crosshair json")
    public File crosshairJson;

    @Override
    public void run() {
        if ( !beforeTargetingXml.getAbsolutePath().endsWith(".xml") ||
                !registeredAfterTargetingXml.getAbsolutePath().endsWith(".xml") ) {
            IJ.log("Not an xml file");
        } else if ( !crosshairJson.getAbsolutePath().endsWith(".json") ) {
            IJ.log("Not a json file");
        } else {
            final LazySpimSource beforeSource = new LazySpimSource("before", beforeTargetingXml.getAbsolutePath() );
            BdvStackSource beforeStackSource = BdvFunctions.show( beforeSource, 1);
            beforeStackSource.setDisplayRange(0, 255);

            final LazySpimSource afterSource = new LazySpimSource("after", registeredAfterTargetingXml.getAbsolutePath() );
            BdvStackSource afterStackSource = BdvFunctions.show( afterSource, 1, BdvOptions.options().addTo( beforeStackSource ));
            afterStackSource.setDisplayRange(0, 255);

            Image3DUniverse universe = new Image3DUniverse();
            universe.show();

            String unit = beforeSource.getVoxelDimensions().unit();

            for ( Source source: new Source[]{beforeSource, afterSource}) {
                // Set to arbitrary colour
                ARGBType colour = new ARGBType(ARGBType.rgba(0, 0, 0, 0));
                Content imageContent = addSourceToUniverse(universe, source, 300 * 300 * 300, Content.VOLUME, colour, 0.7f, 0, 255);
                // Reset colour to default for 3D viewer
                imageContent.setColor(null);
            }

            // new Crosshair(bdvStackSource, universe, imageContent, unit);
        }
    }

    public static void main( String[] args ) {
        // final ImageJ ij = new ImageJ();
        // ij.ui().showUI();
        MeasureTargetingAccuracyCommand command = new MeasureTargetingAccuracyCommand();
        command.beforeTargetingXml = new File( "C:\\Users\\meechan\\Documents\\temp\\azumi_data\\before.xml");
        command.registeredAfterTargetingXml = new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\analysis\\after_registered.xml");
        command.crosshairJson = new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\EM04463_01_settings.json" );
        command.run();
    }
}

