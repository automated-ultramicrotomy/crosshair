package de.embl.schwab.crosshair.ui.command;

import de.embl.schwab.crosshair.targetingaccuracy.TargetingAccuracy;
import ij.IJ;
import mpicbg.spim.data.SpimDataException;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static de.embl.schwab.crosshair.utils.Utils.resetCrossPlatformSwingLookAndFeel;

/**
 * ImageJ command to open targeting accuracy workflow
 */
@Plugin(type = Command.class, menuPath = "Plugins>Crosshair>Analysis>Measure Targeting Accuracy" )
public class MeasureTargetingAccuracyCommand implements Command {

    private static final Logger logger = LoggerFactory.getLogger(MeasureTargetingAccuracyCommand.class);

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
        resetCrossPlatformSwingLookAndFeel();

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
                logger.error("Error opening targeting accuracy", e);
            }
        }
    }
}

