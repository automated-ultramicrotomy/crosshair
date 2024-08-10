package de.embl.schwab.crosshair.settings;

import org.scijava.vecmath.Color3f;

/**
 * Class to hold all settings related to the image content (displayed in 3D viewer) e.g. display settings, name...
 */
public class ImageContentSettings {
    public String name;
    public float imageTransparency;
    public Color3f imageColour;
    // 3D Viewer Transfer function
    public int[] redLut;
    public int[] greenLut;
    public int[] blueLut;
    public int[] alphaLut;

    public ImageContentSettings( String name, float imageTransparency, Color3f imageColour, int[] redLut, int[] greenLut,
                                 int[] blueLut, int[] alphaLut ) {
        this.name = name;
        this.imageTransparency = imageTransparency;
        this.imageColour = imageColour;
        this.redLut = redLut;
        this.greenLut = greenLut;
        this.blueLut = blueLut;
        this.alphaLut = alphaLut;
    }
}
