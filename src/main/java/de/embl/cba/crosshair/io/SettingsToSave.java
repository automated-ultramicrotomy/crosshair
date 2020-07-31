package de.embl.cba.crosshair.io;

import net.imglib2.RealPoint;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3d;

import java.util.ArrayList;
import java.util.Map;

public class SettingsToSave {

    private final Map<String, Vector3d> planeNormals;
    private final Map<String, Vector3d> planePoints;
    private final Map<String, RealPoint> namedVertices;
    private final ArrayList<RealPoint> points;
    private final ArrayList<RealPoint> blockVertices;
    private Color3f targetPlaneColour;
    private Color3f blockPlaneColour;
    private float targetTransparency;
    private float blockTransparency;
    private float imageTransparency;
    private Color3f imageColour;
//    Transfer function
    private int[] redLut;
    private int[] greenLut;
    private int[] blueLut;
    private int[] alphaLut;

    public SettingsToSave(Map<String, Vector3d> planeNormals, Map<String, Vector3d> planePoints,
                          Map<String, RealPoint> namedVertices,
                          ArrayList<RealPoint> points, ArrayList<RealPoint> blockVertices,
                          Color3f targetPlaneColour, Color3f blockPlaneColour, float targetTransparency,
                          float blockTransparency, float imageTransparency, Color3f imageColour,
                          int[] redLut, int[] greenLut, int[] blueLut, int[] alphaLut) {
        this.planeNormals = planeNormals;
        this.planePoints = planePoints;
        this.namedVertices = namedVertices;
        this.points = points;
        this.blockVertices = blockVertices;
        this.targetPlaneColour = targetPlaneColour;
        this.blockPlaneColour = blockPlaneColour;
        this.targetTransparency = targetTransparency;
        this.blockTransparency = blockTransparency;
        this.imageTransparency = imageTransparency;
        this.imageColour = imageColour;
        this.redLut = redLut;
        this.greenLut = greenLut;
        this.blueLut = blueLut;
        this.alphaLut = alphaLut;
    }

    public float getBlockTransparency() {
        return blockTransparency;
    }

    public float getImageTransparency() {
        return imageTransparency;
    }

    public float getTargetTransparency() {
        return targetTransparency;
    }

    public Color3f getBlockPlaneColour() {
        return blockPlaneColour;
    }

    public Color3f getImageColour() {
        return imageColour;
    }

    public Color3f getTargetPlaneColour() {
        return targetPlaneColour;
    }

    public ArrayList<RealPoint> getBlockVertices() {
        return blockVertices;
    }

    public ArrayList<RealPoint> getPoints() {
        return points;
    }

    public Map<String, RealPoint> getNamedVertices() {
        return namedVertices;
    }

    public Map<String, Vector3d> getPlaneNormals() {
        return planeNormals;
    }

    public Map<String, Vector3d> getPlanePoints() {
        return planePoints;
    }

    public int[] getRedLut() {
        return redLut;
    }

    public int[] getGreenLut() {
        return greenLut;
    }

    public int[] getBlueLut() {
        return blueLut;
    }

    public int[] getAlphaLut() {
        return alphaLut;
    }
}
