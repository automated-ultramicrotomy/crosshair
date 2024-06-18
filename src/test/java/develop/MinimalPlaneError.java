package develop;

import customnode.CustomTriangleMesh;
import ij3d.Image3DUniverse;
import org.scijava.vecmath.Point3f;

import java.util.ArrayList;

public class MinimalPlaneError {

    public static void main( String[] args )
    {
        Image3DUniverse universe = new Image3DUniverse();

        // length is 2 works, 1000 doesn't
        // Mesh of a plane perpendicular to the z axis
        float length = 1000;
        float zpos = -100;
        ArrayList<Point3f> triangles = new ArrayList<>();
        triangles.add(new Point3f(-length/2, length/2, zpos));
        triangles.add(new Point3f(length/2, length/2, zpos));
        triangles.add(new Point3f(-length/2, -length/2, zpos));
        triangles.add(new Point3f(length/2, length/2, zpos));
        triangles.add(new Point3f(-length/2, -length/2, zpos));
        triangles.add(new Point3f(length/2, -length/2, zpos));

        CustomTriangleMesh mesh = new CustomTriangleMesh( triangles );
        universe.addCustomMesh( mesh, "test-plane" );

        universe.show();
    }
}
