package de.embl.schwab.crosshair.ui.command;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.targetingaccuracy.TargetingAccuracy;
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
            new TargetingAccuracy( beforeTargetingXml, registeredAfterTargetingXml, crosshairJson );
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

