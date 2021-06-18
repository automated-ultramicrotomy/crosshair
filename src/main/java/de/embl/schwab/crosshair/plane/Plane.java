package de.embl.schwab.crosshair.plane;

import ij3d.Content;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3d;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

public class Plane {

    private String name;

    private Vector3d normal;
    private Vector3d point;

    private Vector3d centroid;

    private Color3f color;
    private float transparency;
    private boolean isVisible;

    private Content mesh; // the 3d custom triangle mesh representing the plane

    private ArrayList<JButton> buttonsAffectedByTracking; // these buttons must be disabled when this plane is tracked

    public Plane( String name, Vector3d normal, Vector3d point, Vector3d centroid, Content mesh, Color3f color,
                  float transparency, boolean isVisible ) {
        this.name = name;
        this.normal = normal;
        this.point = point;
        this.centroid = centroid;

        this.transparency = transparency;
        this.isVisible = isVisible;
        this.mesh = mesh;
        this.color = color;

        this.buttonsAffectedByTracking = new ArrayList<>();
    }

    public void updatePlane( Vector3d normal, Vector3d point, Vector3d centroid, Content mesh ) {
        this.normal = normal;
        this.point = point;
        this.centroid = centroid;
        this.mesh = mesh;
    }

    public String getName() {
        return name;
    }

    public Boolean isVisible() { return this.isVisible(); }

    public void setVisible(Boolean visible) {
        this.isVisible = visible;
        if ( mesh != null ) {
            mesh.setVisible(visible);
        }
    }

    public Color3f getColor() {
        return this.color;
    }

    public void setColor( Color color ) {
        this.color = new Color3f(color);
        if ( mesh != null ) {
            // make copy of colour to assign (using original interferes with changing colour later)
            mesh.setColor( this.color );
        }
    }

    public Float getTransparency() {
        return this.transparency;
    }

    public void setTransparency( Float transparency ) {
        this.transparency = transparency;
        if ( mesh != null ) {
            mesh.setTransparency(transparency);
        }
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
