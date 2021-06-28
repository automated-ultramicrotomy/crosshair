package de.embl.schwab.crosshair.ui.command;

import de.embl.schwab.crosshair.targetingaccuracy.TargetingAccuracy;
import ij.IJ;
import mpicbg.spim.data.SpimDataException;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>Crosshair>Analysis>Measure Targeting Accuracy" )
public class MeasureTargetingAccuracyCommand implements Command {

    @Parameter(label="Before targeting image xml")
    public File beforeTargetingXml;

    @Parameter(label="Registered after targeting image xml")
    public File registeredAfterTargetingXml;

    @Parameter(label="Crosshair settings json")
    public File crosshairSettingsJson;

    @Parameter(label="Crosshair solution json")
    public File crosshairSolutionJson;

    @Override
    public void run() {
        if ( !beforeTargetingXml.getAbsolutePath().endsWith(".xml") ||
                !registeredAfterTargetingXml.getAbsolutePath().endsWith(".xml") ) {
            IJ.log("Not an xml file");
        } else if ( !crosshairSettingsJson.getAbsolutePath().endsWith(".json") ||
                !crosshairSolutionJson.getAbsolutePath().endsWith(".json") ) {
            IJ.log("Not a json file");
        } else {
            try {
                new TargetingAccuracy( beforeTargetingXml, registeredAfterTargetingXml,
                        crosshairSettingsJson, crosshairSolutionJson );
            } catch (SpimDataException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main( String[] args ) {
        // final ImageJ ij = new ImageJ();
        // ij.ui().showUI();
        MeasureTargetingAccuracyCommand command = new MeasureTargetingAccuracyCommand();
        command.beforeTargetingXml = new File( "C:\\Users\\meechan\\Documents\\temp\\azumi_data\\before.xml");
        command.registeredAfterTargetingXml = new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\analysis\\after_registered.xml");
        command.crosshairSettingsJson = new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\EM04463_01_new_format_settings.json" );
        command.crosshairSolutionJson = new File( "C:\\Users\\meechan\\Documents\\temp\\azumi_data\\EM04463_01_solution.json.json" );
        command.run();
    }
}

