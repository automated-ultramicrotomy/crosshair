package develop;

import customnode.CustomPointMesh;
import ij3d.Image3DUniverse;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Point3f;

import java.util.ArrayList;

public class TestPointMesh {
    public static void main( String[] args ) {
        Image3DUniverse universe = new Image3DUniverse();
        ArrayList<Point3f> points = new ArrayList<>();
        points.add( new Point3f( 1, 1, 1));
        points.add( new Point3f( 1, 0, 1));
        points.add( new Point3f( 0, 0, 0));

        final CustomPointMesh tmesh = new CustomPointMesh(points, new Color3f(1, 0,0), 0);
        tmesh.setPointSize(10);
        universe.addCustomMesh( tmesh, "ponts");

        universe.show();
        tmesh.addPoint( new Point3f(0, 1, 0));
    }
}
