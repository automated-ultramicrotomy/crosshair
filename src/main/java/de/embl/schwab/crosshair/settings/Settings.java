package de.embl.schwab.crosshair.settings;

import java.util.Map;

/**
 * Class to hold all settings for displayed planes and images (e.g. display settings, normals, points...)
 * These settings can be saved/read from .json files.
 */
public class Settings {
    public Map<String, PlaneSettings> planeNameToSettings;
    public Map< String, ImageContentSettings> imageNameToSettings;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final Settings other = (Settings) obj;
        if (!planeNameToSettings.equals(other.planeNameToSettings)) {
            return false;
        }
        if (!imageNameToSettings.equals(other.imageNameToSettings)) {
            return false;
        }
        return true;
    }
}
