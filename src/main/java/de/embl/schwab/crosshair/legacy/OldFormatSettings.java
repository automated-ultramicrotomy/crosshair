package de.embl.schwab.crosshair.legacy;

import net.imglib2.RealPoint;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OldFormatSettings {

    public final Map<String, Vector3d> planeNormals = new HashMap<>();
    public final Map<String, Vector3d> planePoints = new HashMap<>();
    public final Map<String, RealPoint> namedVertices = new HashMap<>();
    public final ArrayList<RealPoint> points = new ArrayList<>();
    public final ArrayList<RealPoint> blockVertices = new ArrayList<>();
    public Color3f targetPlaneColour;
    public Color3f blockPlaneColour;
    public float targetTransparency;
    public float blockTransparency;
    public float imageTransparency;
    public Color3f imageColour;
    // 3D Viewer Transfer function
    public int[] redLut;
    public int[] greenLut;
    public int[] blueLut;
    public int[] alphaLut;
}
