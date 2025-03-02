package de.embl.schwab.crosshair.settings;

import org.scijava.vecmath.Color3f;

import java.util.Arrays;
import java.util.Objects;

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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final ImageContentSettings other = (ImageContentSettings) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (this.imageTransparency != other.imageTransparency) {
            return false;
        }
        if (this.imageColour == null && other.imageColour != null) {
            return false;
        }
        if (this.imageColour != null && other.imageColour != null && !this.imageColour.equals(other.imageColour)) {
                return false;
        }
        if (!Arrays.equals(this.redLut, other.redLut)) {
            return false;
        }
        if (!Arrays.equals(this.greenLut, other.greenLut)) {
            return false;
        }
        if (!Arrays.equals(this.blueLut, other.blueLut)) {
            return false;
        }
        if (!Arrays.equals(this.alphaLut, other.alphaLut)) {
            return false;
        }

        return true;
    }
}
