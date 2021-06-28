package de.embl.schwab.crosshair.legacy;

import com.google.gson.Gson;


import java.io.FileReader;
import java.io.IOException;

public class OldFormatSettingsReader {

    public OldFormatSettingsReader() {}

    public OldFormatSettings readSettings( String filePath ) {
        Gson gson = new Gson();
        try ( FileReader fileReader = new FileReader(filePath) ) {
            return gson.fromJson(fileReader, OldFormatSettings.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
