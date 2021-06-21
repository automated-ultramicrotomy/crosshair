package de.embl.schwab.crosshair.plane;

import customnode.CustomTriangleMesh;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij3d.Content;
import ij3d.Image3DUniverse;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3d;

import java.util.ArrayList;

public class PlaneCreator {

    private Image3DUniverse universe; // universe to add all planes to
    private Content imageContent; // 3d image content used to define bounds of plane

    // alternate between green and blue to make it easier to see new planes
    private static int colourIndex = 0;

    public PlaneCreator( Image3DUniverse universe, Content imageContent ) {
        this.universe = universe;
        this.imageContent = imageContent;
    }

    public Plane createPlane( Vector3d planeNormal, Vector3d planePoint, String planeName ) {

        // intersection points with image bounds, these will form the vertices of the plane mesh
        ArrayList<Vector3d> intersectionPoints = calculateIntersectionPoints(planeNormal, planePoint);
        Vector3d planeCentroid = GeometryUtils.getCentroid(intersectionPoints);

        Color3f color = generateNewPlaneColor();
        float transparency = 0.7f;
        boolean isVisible = true;

        Content meshContent = createMeshContent( intersectionPoints, planeNormal, color, transparency, isVisible, planeName );

        return new Plane( planeName, planeNormal, planePoint, planeCentroid,
                meshContent, color, transparency, isVisible );
    }

    public void updatePlane( Plane plane, Vector3d newNormal, Vector3d newPoint ) {
        if ( universe.contains( plane.getName() ) ) {
            universe.removeContent( plane.getName() );
        }

        // intersection points with image bounds, these will form the vertices of the plane mesh
        ArrayList<Vector3d> intersectionPoints = calculateIntersectionPoints( newNormal, newPoint );
        Vector3d newCentroid = GeometryUtils.getCentroid(intersectionPoints);

        Content meshContent = createMeshContent( intersectionPoints, newNormal, plane.getColor(), plane.getTransparency(),
                plane.isVisible(), plane.getName() );

        plane.updatePlane( newNormal, newPoint, newCentroid, meshContent );
    }

    private Color3f generateNewPlaneColor() {
        // alternate between green and blue to make it easier to see new planes
        if ( colourIndex == 0 ) {
            colourIndex = 1;
            return new Color3f(0, 1, 0);
        } else {
            colourIndex = 0;
            return new Color3f(0, 0, 1);
        }
    }

    private Content createMeshContent( ArrayList<Vector3d> intersectionPoints, Vector3d planeNormal,
                                        Color3f color, float transparency, boolean isVisible, String planeName ) {
        Content meshContent;
        if (intersectionPoints.size() > 0) {
            CustomTriangleMesh mesh = createPlaneMesh( intersectionPoints, planeNormal,
                    color, transparency );
            meshContent = universe.addCustomMesh( mesh, planeName );
            meshContent.setLocked( true );
            meshContent.setVisible( isVisible );
        } else {
            meshContent = null;
        }

        return meshContent;
    }

    private CustomTriangleMesh createPlaneMesh(ArrayList<Vector3d> intersectionPoints, Vector3d planeNormal,
                                               Color3f color, float transparency ) {

        ArrayList<Point3f> vectorPoints = new ArrayList<>();
        for (Vector3d d : intersectionPoints) {
            vectorPoints.add(new Point3f((float) d.getX(), (float) d.getY(), (float) d.getZ()));
        }

        // must account for any transformation of the image
        Transform3D rotate = new Transform3D();
        imageContent.getLocalRotate(rotate);
        Vector3d transformedNormal = new Vector3d(planeNormal.getX(), planeNormal.getY(), planeNormal.getZ());
        rotate.transform(transformedNormal);

        CustomTriangleMesh newMesh = null;
        if (intersectionPoints.size() == 3) {
            newMesh = new CustomTriangleMesh( vectorPoints, color, transparency );
        } else if (intersectionPoints.size() > 3) {
            ArrayList<Point3f> triangles = GeometryUtils.calculateTrianglesFromPoints(intersectionPoints, transformedNormal);
            newMesh = new CustomTriangleMesh( triangles, color, transparency );
        }

        return newMesh;
    }

    private ArrayList<Vector3d> calculateIntersectionPoints(Vector3d planeNormal, Vector3d planePoint ) {
        Point3d min = new Point3d();
        Point3d max = new Point3d();
        imageContent.getMax(max);
        imageContent.getMin(min);
        double[] minCoord = new double[3];
        double[] maxCoord = new double[3];
        min.get(minCoord);
        max.get(maxCoord);

        ArrayList<Vector3d> intersectionPoints = GeometryUtils.calculateIntersections(minCoord, maxCoord, planeNormal, planePoint);

        if (intersectionPoints.size() > 0) {
            // intersections were in local space, we want to display in the global so must account for any transformations
            // of the image
            Transform3D translate = new Transform3D();
            Transform3D rotate = new Transform3D();
            imageContent.getLocalTranslate(translate);
            imageContent.getLocalRotate(rotate);

            for (Vector3d point : intersectionPoints) {
                // convert to point > transform affects vectors differently
                Point3d intersect = new Point3d(point.getX(), point.getY(), point.getZ());
                rotate.transform(intersect);
                translate.transform(intersect);
                point.setX(intersect.getX());
                point.setY(intersect.getY());
                point.setZ(intersect.getZ());
            }
        }

        return intersectionPoints;
    }
}
