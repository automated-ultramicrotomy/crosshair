package de.embl.schwab.crosshair.plane;

import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3d;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

public class Plane {

    // alternate between green and blue to make it easier to see new planes
    private static int colourIndex = 0;

    private String name;

    private Vector3d normal;
    private Vector3d point;

    private Vector3d centroid;

    private Color3f color;
    private Float transparency;
    private Boolean visible;

    private ArrayList<JButton> buttonsAffectedByTracking; // these buttons must be disabled when this plane is tracked

    public Plane( String name, Vector3d normal, Vector3d point, Vector3d centroid ) {
        this.name = name;
        this.normal = normal;
        this.point = point;
        this.centroid = centroid;

        this.transparency = 0.7f;
        this.visible = true;

        // alternate between green and blue to make it easier to see new planes
        if ( colourIndex == 0 ) {
            this.color = new Color3f(0, 1, 0);
            colourIndex = 1;
        } else {
            this.color = new Color3f(0, 0, 1);
            colourIndex = 0;
        }

        this.buttonsAffectedByTracking = new ArrayList<>();
    }

    public void updatePlane( Vector3d normal, Vector3d point, Vector3d centroid ) {
        this.normal = normal;
        this.point = point;
        this.centroid = centroid;
    }

    public String getName() {
        return name;
    }

    public Boolean isVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Color3f getColor() {
        return color;
    }

    public void setColor( Color color ) {
        // make copy of colour to assign (using original interferes with changing colour later)
        this.color.set( new Color3f( color ) );
    }

    public Float getTransparency() {
        return transparency;
    }

    public void setTransparency( Float transparency ) {
        this.transparency = transparency;
    }

    public Vector3d getCentroid() {
        return centroid;
    }

    public Vector3d getNormal() {
        return normal;
    }

    public Vector3d getPoint() {
        return point;
    }

    public ArrayList<JButton> getButtonsAffectedByTracking() {
        return buttonsAffectedByTracking;
    }

    public void addButtonAffectedByTracking(JButton jButton ) {
        buttonsAffectedByTracking.add( jButton );
    }
}
