package de.embl.schwab.crosshair.plane;

import bdv.util.Bdv;
import customnode.CustomTriangleMesh;
import de.embl.schwab.crosshair.points.PointsToFitPlaneDisplay;
import de.embl.schwab.crosshair.points.VertexDisplay;
import de.embl.schwab.crosshair.points.overlays.Point3dOverlay;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.PlaneSettings;
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

    private final Image3DUniverse universe; // universe to add all planes to
    private final Content imageContent; // 3d image content used to define bounds of plane
    private final Bdv bdv; // bdv instance to assign to plane, needed to keep 2d point overlays up to date
    private final Point3dOverlay point3dOverlay; // 3d point overlay to assign to plane

    public PlaneCreator(Image3DUniverse universe, Content imageContent, Bdv bdv, Point3dOverlay point3dOverlay ) {
        this.universe = universe;
        this.imageContent = imageContent;
        this.bdv = bdv;
        this.point3dOverlay = point3dOverlay;
    }

    private class CentroidAndMesh {
        public Vector3d centroid;
        public Content mesh;
    }

    public Plane createPlane( PlaneSettings planeSettings ) {

        PointsToFitPlaneDisplay pointsToFitPlaneDisplay = new PointsToFitPlaneDisplay(
                planeSettings.pointsToFitPlane, planeSettings.name, bdv, point3dOverlay );

        if ( isOrientationSet( planeSettings ) ) {
            CentroidAndMesh centroidAndMesh = createCentroidAndMesh(planeSettings);
            return new Plane( planeSettings, centroidAndMesh.centroid, centroidAndMesh.mesh, pointsToFitPlaneDisplay );
        } else {
            return new Plane( planeSettings, null, null, pointsToFitPlaneDisplay );
        }
    }

    public BlockPlane createBlockPlane( BlockPlaneSettings blockPlaneSettings ) {

        PointsToFitPlaneDisplay pointsToFitPlaneDisplay = new PointsToFitPlaneDisplay(
                blockPlaneSettings.pointsToFitPlane, blockPlaneSettings.name, bdv, point3dOverlay );
        VertexDisplay vertexDisplay = new VertexDisplay(
                blockPlaneSettings.vertices, blockPlaneSettings.assignedVertices, blockPlaneSettings.name, bdv, point3dOverlay );

        if ( isOrientationSet( blockPlaneSettings ) ) {
            CentroidAndMesh centroidAndMesh = createCentroidAndMesh( blockPlaneSettings );
            return new BlockPlane(blockPlaneSettings, centroidAndMesh.centroid, centroidAndMesh.mesh,
                    pointsToFitPlaneDisplay, vertexDisplay);
        } else {
            return new BlockPlane( blockPlaneSettings, null, null, pointsToFitPlaneDisplay, vertexDisplay );
        }
    }

    private boolean isOrientationSet( PlaneSettings settings ) {
        return settings.normal != null && settings.point != null;
    }

    public void updatePlaneOrientation( Plane plane, Vector3d newNormal, Vector3d newPoint ) {
        if ( universe.contains( plane.getName() ) ) {
            universe.removeContent( plane.getName() );
        }

        // intersection points with image bounds, these will form the vertices of the plane mesh
        ArrayList<Vector3d> intersectionPoints = calculateIntersectionPoints( newNormal, newPoint );
        Vector3d newCentroid = GeometryUtils.getCentroid(intersectionPoints);

        Content meshContent = createMeshContent( intersectionPoints, newNormal, plane.getColor(), plane.getTransparency(),
                plane.isVisible(), plane.getName() );

        plane.updatePlaneOrientation( newNormal, newPoint, newCentroid, meshContent );
    }

    private CentroidAndMesh createCentroidAndMesh( PlaneSettings settings ) {
        CentroidAndMesh centroidAndMesh = new CentroidAndMesh();

        // intersection points with image bounds, these will form the vertices of the plane mesh
        ArrayList<Vector3d> intersectionPoints = calculateIntersectionPoints( settings.normal,
                settings.point) ;
        centroidAndMesh.centroid = GeometryUtils.getCentroid(intersectionPoints);

        Content meshContent = createMeshContent( intersectionPoints, settings.normal,
                settings.color, settings.transparency, settings.isVisible,
                settings.name );
        centroidAndMesh.mesh = meshContent;

        return centroidAndMesh;
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
