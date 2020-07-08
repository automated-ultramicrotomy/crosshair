package de.embl.cba.targeting;

import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
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
import vib.BenesNamedPoint;
import vib.PointList;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
    private final BdvStackSource bdvStackSource;
    private final Image3DUniverse universe;
    private final Content imageContent;

    private Color3f targetPlaneColour;
    private Color3f blockPlaneColour;

    private float targetTransparency;
    private float blockTransparency;


    public PlaneManager(BdvStackSource bdvStackSource, Image3DUniverse universe, Content imageContent) {
        planeNormals = new HashMap<>();
        planePoints = new HashMap<>();
        planeCentroids = new HashMap<>();

        namedVertices = new HashMap<>();
        points = new ArrayList<>();
        blockVertices = new ArrayList<>();
        selectedVertex = new RealPoint(3);
        this.bdvStackSource = bdvStackSource;
        this.bdvHandle = bdvStackSource.getBdvHandle();
        this.universe = universe;
        this.imageContent = imageContent;

        targetPlaneColour = new Color3f(0, 1, 0);
        blockPlaneColour = new Color3f(0, 0, 1);
        targetTransparency = 0.7f;
        blockTransparency = 0.7f;
    }

    public RealPoint getSelectedVertex() {
        return selectedVertex;
    }

    public Map<String, Vector3d> getPlaneNormals() { return planeNormals; }

    public Map<String, Vector3d> getPlanePoints() { return planePoints; }

    public Map<String, Vector3d> getPlaneCentroids() { return planeCentroids; }

    public Map<String, RealPoint> getNamedVertices() {
        return namedVertices;
    }

    public ArrayList<RealPoint> getPoints() {return points;}
    public ArrayList<RealPoint> getBlockVertices() {return blockVertices;}
    public float getTargetTransparency() {return targetTransparency;}
    public float getBlockTransparency() {return blockTransparency;}

    public void nameVertex (String name) {

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
            float transparency = 0.7f;
            if (planeName.equals("target")) {
                planeColor = targetPlaneColour;
                transparency = targetTransparency;

            } else if (planeName.equals("block")) {
                planeColor = blockPlaneColour;
                transparency = blockTransparency;
            }

            CustomTriangleMesh newMesh = null;
            if (intersectionPoints.size() == 3) {
                newMesh = new CustomTransparentTriangleMesh(vectorPoints, planeColor, transparency);
            } else if (intersectionPoints.size() > 3) {
                ArrayList<Point3f> triangles = calculateTrianglesFromPoints(intersectionPoints, transformedNormal);
                newMesh = new CustomTransparentTriangleMesh(triangles, planeColor, transparency);
            }
            Content meshContent = universe.addCustomMesh(newMesh, planeName);
            meshContent.setVisible(true);
            meshContent.setLocked(true);
        }
    }

        public void moveViewToNamedPlane (String name) {
            double[] targetNormal = new double[3];
            planeNormals.get(name).get(targetNormal);

            double[] targetCentroid = new double[3];
            planeCentroids.get(name).get(targetCentroid);

            moveToPosition(bdvStackSource, targetCentroid, 0);
            levelCurrentView(bdvStackSource, targetNormal);
        }

        public void addRemoveCurrentPositionPoints () {
            addRemoveCurrentPositionFromPointList(points);
        }

        public void addRemoveCurrentPositionBlockVertices () {
            addRemoveCurrentPositionFromPointList(blockVertices);
        }

        private void addRemoveCurrentPositionFromPointList (ArrayList<RealPoint> points) {
        // remove point if already present, otherwise add point
            RealPoint point = getCurrentPosition();
            double[] pointViewerCoords = convertToViewerCoordinates(point);

            boolean distanceMatch = false;
            for ( int i = 0; i < points.size(); i++ )
            {
                RealPoint currentPoint = points.get(i);
                double[] currentPointViewerCoords = convertToViewerCoordinates(currentPoint);
                double distance = distanceBetweenPoints(pointViewerCoords, currentPointViewerCoords);
                if (distance < 5) {
                    removePointFrom3DViewer(currentPoint);
                    points.remove(i);
                    bdvHandle.getViewerPanel().requestRepaint();

                    distanceMatch = true;
                    break;
                }

            }

            if (!distanceMatch) {
                points.add(point);
                bdvHandle.getViewerPanel().requestRepaint();

                //TODO - check properly that these positions match between two viewers
                double[] position = new double[3];
                point.localize(position);
                imageContent.getPointList().add("", position[0], position[1], position[2]);
            }

        }

    private void removePointFrom3DViewer (RealPoint point) {
        // remove from 3D view and bdv
        double[] chosenPointCoord = new double[3];
        point.localize(chosenPointCoord);

        int pointIndex = imageContent.getPointList().indexOfPointAt(chosenPointCoord[0], chosenPointCoord[1], chosenPointCoord[2], imageContent.getLandmarkPointSize());
        imageContent.getPointList().remove(pointIndex);

//					There's a bug in how the 3D viewer displays points after one is removed. Currently, it just stops
//					displaying the first point added (rather than the one you actually removed).
//					Therefore here I remove all points and re-add them, to get the viewer to reset how it draws
//					the points. Perhaps there's a more efficient way to get around this?
        PointList currentPointList = imageContent.getPointList().duplicate();
        imageContent.getPointList().clear();
        for (Iterator<BenesNamedPoint> it = currentPointList.iterator(); it.hasNext(); ) {
            BenesNamedPoint p = it.next();
            imageContent.getPointList().add(p);
        }
    }

    private RealPoint getCurrentPosition () {
        RealPoint point = new RealPoint(3);
        bdvHandle.getViewerPanel().getGlobalMouseCoordinates(point);
        return point;
    }

    private double[] getCurrentPositionViewerCoordinates () {
        RealPoint point = getCurrentPosition();
        double[] pointViewerCoords = convertToViewerCoordinates(point);
        return pointViewerCoords;
    }


    private double[] convertToViewerCoordinates (RealPoint point) {
        final AffineTransform3D transform = new AffineTransform3D();
        bdvHandle.getViewerPanel().getState().getViewerTransform( transform );

        final double[] lPos = new double[ 3 ];
        final double[] gPos = new double[ 3 ];
        // get point position (in microns etc)
        point.localize(lPos);
        // get point position in viewer (I guess in pixel units?), so gpos[2] is the distance in pixels
        // from the current view plane
        transform.apply(lPos, gPos);

        return gPos;
    }

    public void setSelectedVertexCurrentPosition () {
        double[] pointViewerCoords = getCurrentPositionViewerCoordinates();
        for ( int i = 0; i < blockVertices.size(); i++ ) {
            double[] currentPointViewerCoords = convertToViewerCoordinates(blockVertices.get(i));
            double distance = distanceBetweenPoints(pointViewerCoords, currentPointViewerCoords);
            if (distance < 5) {
                selectedVertex.setPosition(blockVertices.get(i));
                bdvHandle.getViewerPanel().requestRepaint();
                break;
            }
        }
    }

    public void setTargetPlaneColour (Color colour) {
        targetPlaneColour.set(colour);
//        inefficent - just update colour as is
        updatePlanesInPlace();
    }

    public void setBlockPlaneColour (Color colour) {
        blockPlaneColour.set(colour);
//        inefficent - just update colour as is
        updatePlanesInPlace();
    }

    public void setTargetTransparency (float transparency) {
        targetTransparency = transparency;
//        inefficent - just update transparency
        updatePlanesInPlace();
    }

    public void setBlockTransparency (float transparency) {
        blockTransparency = transparency;
//        inefficent - just update colour as is
        updatePlanesInPlace();
    }
}