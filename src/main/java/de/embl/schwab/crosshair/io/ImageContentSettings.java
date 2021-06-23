package de.embl.schwab.crosshair.io;

import org.scijava.vecmath.Color3f;

public class ImageContentSettings {
    public float imageTransparency;
    public Color3f imageColour;
    // 3D Viewer Transfer function
    public int[] redLut;
    public int[] greenLut;
    public int[] blueLut;
    public int[] alphaLut;

    public ImageContentSettings( float imageTransparency, Color3f imageColour, int[] redLut, int[] greenLut, int[] blueLut,
                          int[] alphaLut ) {
        this.imageTransparency = imageTransparency;
        this.imageColour = imageColour;
        this.redLut = redLut;
        this.greenLut = greenLut;
        this.blueLut = blueLut;
        this.alphaLut = alphaLut;
    }
}
