package develop;

import de.embl.schwab.crosshair.legacy.SettingsFormatConverter;

import java.io.File;

public class ConvertOldFormatSettingsToNew {
    public static void main( String[] args )
    {
        // new SettingsFormatConverter( new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\EM04463_01_settings.json"),
        //         new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\EM04463_01_new_format_settings.json") ).convertOldSettingsToNew();

        new SettingsFormatConverter( new File("C:\\Users\\meechan\\Documents\\Repos\\crosshair\\src\\test\\resources\\legacy\\exampleBlock.json"),
                new File("C:\\Users\\meechan\\Documents\\Repos\\crosshair\\src\\test\\resources\\exampleBlock.json" ) ).convertOldSettingsToNew();

    }


}
