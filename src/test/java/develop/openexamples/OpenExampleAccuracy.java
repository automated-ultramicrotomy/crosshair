package develop.openexamples;

import de.embl.schwab.crosshair.ui.command.MeasureTargetingAccuracyCommand;

import java.io.File;

public class OpenExampleAccuracy {
    public static void main( String[] args ) {
        MeasureTargetingAccuracyCommand command = new MeasureTargetingAccuracyCommand();
        command.beforeTargetingXml = new File( "C:\\Users\\meechan\\Documents\\temp\\azumi_data\\before.xml");
        command.registeredAfterTargetingXml = new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\analysis\\after_registered.xml");
        command.crosshairSettingsJson = new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\EM04463_01_new_format_settings.json" );
        command.crosshairSolutionJson = new File( "C:\\Users\\meechan\\Documents\\temp\\azumi_data\\EM04463_01_solution.json.json" );
        command.run();
    }
}
