package de.embl.schwab.crosshair.settings;

import java.util.Map;

/**
 * Class to hold all settings for displayed planes and images (e.g. display settings, normals, points...)
 * These settings can be saved/read from .json files.
 */
public class Settings {
    public Map<String, PlaneSettings> planeNameToSettings;
    public Map< String, ImageContentSettings> imageNameToSettings;
}
