package de.embl.schwab.crosshair.microtome;

import customnode.CustomTriangleMesh;
import ij3d.Image3DUniverse;
import org.junit.jupiter.api.Test;
import org.scijava.vecmath.Point3f;
import java.util.ArrayList;

class CuttingTest {

    /**
     * Test to ensure a plane perpendicular to the y axis can be added to the 3D
     * viewer without errors. This used to cause issues with generating the cutting plane when the knife
     * was set to 0 degrees.
     */
    @Test
    void addZPerpendicularPlaneTo3dViewer() {
        Image3DUniverse universe = new Image3DUniverse();
        String planeName = "test-plane";

        // length is 2 works, 1000 doesn't
        // Mesh of a plane perpendicular to the y axis
        float length = 1000;
        float ypos = -100;
        ArrayList<Point3f> triangles = new ArrayList<>();
        triangles.add(new Point3f(-length/2, ypos, length/2));
        triangles.add(new Point3f(length/2, ypos, length/2));
        triangles.add(new Point3f(-length/2, ypos, -length/2));
        triangles.add(new Point3f(length/2, ypos, length/2));
        triangles.add(new Point3f(-length/2, ypos, -length/2));
        triangles.add(new Point3f(length/2, ypos, -length/2));

        CustomTriangleMesh mesh = new CustomTriangleMesh( triangles );
        universe.addCustomMesh( mesh, planeName );

        assert universe.contains(planeName);
    }

}