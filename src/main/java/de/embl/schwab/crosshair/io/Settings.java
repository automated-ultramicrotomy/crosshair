package de.embl.schwab.crosshair.io;

import de.embl.schwab.crosshair.plane.Plane;
import de.embl.schwab.crosshair.plane.PlaneSettings;
import net.imglib2.RealPoint;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Settings {
    public List<PlaneSettings> planeSettings;
    public Map<String, ImageContentSettings> imageNameToSettings;
}
