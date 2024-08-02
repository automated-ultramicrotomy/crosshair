package de.embl.schwab.crosshair.legacy;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.FileReader;
import java.io.IOException;

/**
 * Class to read old format Crosshair settings
 */
public class OldFormatSettingsReader {

    private static final Logger logger = LoggerFactory.getLogger(OldFormatSettingsReader.class);

    /**
     * Create a settings reader
     */
    public OldFormatSettingsReader() {}

    /**
     * Read settings from a json file
     * @param filePath file path of settings json file
     * @return Crosshair settings (in the old format)
     */
    public OldFormatSettings readSettings( String filePath ) {
        Gson gson = new Gson();
        try ( FileReader fileReader = new FileReader(filePath) ) {
            return gson.fromJson(fileReader, OldFormatSettings.class);
        } catch (IOException e) {
            logger.error("Error reading settings", e);
        }

        return null;
    }

}
