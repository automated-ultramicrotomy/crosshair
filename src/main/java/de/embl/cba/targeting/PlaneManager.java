package de.embl.cba.targeting;

import bdv.util.BdvHandle;
import customnode.CustomTransparentTriangleMesh;
import customnode.CustomTriangleMesh;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.targeting.GeometryUtils.*;
import static de.embl.cba.targeting.GeometryUtils.calculateTrianglesFromPoints;

public class PlaneManager {

    private final Map<String, Vector3d> planeNormals;
    private final Map<String, Vector3d> planePoints;
    private final Map<String, Vector3d> planeCentroids;
    private final Map<String, RealPoint> namedVertices;
    private final ArrayList<RealPoint> points;
    private final ArrayList<RealPoint> blockVertices;
    private final RealPoint selectedVertex;

    private final BdvHandle bdvHandle;
    private final Image3DUniverse universe;
    private final Content imageContent;


    public PlaneManager(BdvHandle bdvHandle, Image3DUniverse universe, Content imageContent) {
        planeNormals = new HashMap<>();
        planePoints = new HashMap<>();
        planeCentroids = new HashMap<>();
        namedVertices = new HashMap<>();
        points = new ArrayList<>();
        blockVertices = new ArrayList<>();
        selectedVertex = new RealPoint(3);
        this.bdvHandle = bdvHandle;
        this.universe = universe;
        this.imageContent = imageContent;
    }

    public RealPoint getSelectedVertex() {
        return selectedVertex;
    }

    public Map<String, RealPoint> getNamedVertices() {
        return namedVertices;
    }

    public Map<String, RealPoint> nameVertex (String name) {

        //TODO - figure a way to sort this - what's an empty value I can set to? Would need this also to
        // deselect points
//        if (selectedVertex == null) {
//            return;
//        }

        RealPoint selectedPointCopy = new RealPoint(selectedVertex);
        if (name.equals("top_left")) {
            renamePoint3D(imageContent, selectedVertex, "TL");
            namedVertices.put("top_left", selectedPointCopy);
            bdvHandle.getViewerPanel().requestRepaint();
        } else if (name.equals("top_right")) {
            renamePoint3D(imageContent, selectedVertex, "TR");
            namedVertices.put("top_right", selectedPointCopy);
            bdvHandle.getViewerPanel().requestRepaint();
        } else if (name.equals("bottom_left")) {
            renamePoint3D(imageContent, selectedVertex, "BL");
            namedVertices.put("bottom_left", selectedPointCopy);
            bdvHandle.getViewerPanel().requestRepaint();
        } else if (name.equals("bottom_right")) {
            renamePoint3D(imageContent, selectedVertex, "BR");
            namedVertices.put("bottom_right", selectedPointCopy);
            bdvHandle.getViewerPanel().requestRepaint();
        }
    }

    private void renamePoint3D(Content content, RealPoint point, String name) {
        double[] pointCoord = new double[3];
        point.localize(pointCoord);
        int pointIndex = content.getPointList().indexOfPointAt(pointCoord[0], pointCoord[1], pointCoord[2], content.getLandmarkPointSize());
        content.getPointList().rename(content.getPointList().get(pointIndex), name);
    }

    public void updatePlaneOnTransformChange(AffineTransform3D affineTransform3D, String planeName) {

        final ArrayList< double[] > viewerPoints = new ArrayList<>();

        viewerPoints.add( new double[]{ 0, 0, 0 });
        viewerPoints.add( new double[]{ 0, 100, 0 });
        viewerPoints.add( new double[]{ 100, 0, 0 });

        final ArrayList< double[] > globalPoints = new ArrayList<>();
        for ( int i = 0; i < 3; i++ )
        {
            globalPoints.add( new double[ 3 ] );
        }

        for ( int i = 0; i < 3; i++ )
        {
            affineTransform3D.inverse().apply( viewerPoints.get( i ), globalPoints.get( i ) );
        }

        Vector3d planeNormal = calculateNormalFromPoints(globalPoints);
        Vector3d planePoint = new Vector3d(globalPoints.get(0)[0], globalPoints.get(0)[1], globalPoints.get(0)[2]);

        updatePlane(planeNormal, planePoint, planeName);

    }

    // update planes on transform of the imagecontent, retain teh existing plane normals and points
    public void updatePlanesInPlace() {
        for (String key: planeNormals.keySet()) {
            updatePlane(planeNormals.get(key), planePoints.get(key), key);
        }
    }

    public void updatePlane(Vector3d planeNormal, Vector3d planePoint, String planeName) {

        //TODO - shift to use bounding box of image itself
    // Get bounding box of image, account for any transformation of teh image
        Point3d min = new Point3d();
        Point3d max = new Point3d();
        imageContent.getMax(max);
        imageContent.getMin(min);
        System.out.println(max.toString());
        double[] minCoord = new double[3];
        double[] maxCoord = new double[3];
        min.get(minCoord);
        max.get(maxCoord);
        System.out.println(max.toString());

//		TODO - remvoe image content
        ArrayList<Vector3d> intersectionPoints = calculateIntersections(minCoord, maxCoord, planeNormal, planePoint, imageContent, universe);

        if (intersectionPoints.size() > 0) {
            planeNormals.put(planeName, planeNormal);
            planePoints.put(planeName, planePoint);
            planeCentroids.put(planeName, getCentroid(intersectionPoints));

            // intersections were in local space, we want to display in the global so must account for any transformations
            // of the image
            Transform3D translate = new Transform3D();
            Transform3D rotate = new Transform3D();
            imageContent.getLocalTranslate(translate);
            imageContent.getLocalRotate(rotate);

            for (Vector3d point : intersectionPoints) {
                // convert to point > transfrom affects vectors differently
                Point3d intersect = new Point3d(point.getX(), point.getY(), point.getZ());
                rotate.transform(intersect);
                translate.transform(intersect);
                point.setX(intersect.getX());
                point.setY(intersect.getY());
                point.setZ(intersect.getZ());
            }

            Vector3d transformedNormal = new Vector3d(planeNormal.getX(), planeNormal.getY(), planeNormal.getZ());
            rotate.transform(transformedNormal);

            System.out.println(intersectionPoints.size());
            ArrayList<Point3f> vectorPoints = new ArrayList<>();
            for (Vector3d d : intersectionPoints) {
                vectorPoints.add(new Point3f((float) d.getX(), (float) d.getY(), (float) d.getZ()));
            }

            if (universe.contains(planeName)) {
                universe.removeContent(planeName);
            }

            Color3f planeColor = null;
            if (planeName.equals("target")) {
                planeColor = new Color3f(0, 1, 0);
            } else if (planeName.equals("block")) {
                planeColor = new Color3f(0, 0, 1);
            }

            CustomTriangleMesh newMesh = null;
            if (intersectionPoints.size() == 3) {
                newMesh = new CustomTransparentTriangleMesh(vectorPoints, planeColor, 0.7f);
            } else if (intersectionPoints.size() > 3) {
                ArrayList<Point3f> triangles = calculateTrianglesFromPoints(intersectionPoints, transformedNormal);
                newMesh = new CustomTransparentTriangleMesh(triangles, planeColor, 0.7f);
            }
            Content meshContent = universe.addCustomMesh(newMesh, planeName);
            meshContent.setVisible(true);
            meshContent.setLocked(true);
        }

    }
}
