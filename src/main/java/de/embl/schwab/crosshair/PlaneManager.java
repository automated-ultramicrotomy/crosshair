package de.embl.schwab.crosshair;

import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import customnode.CustomTriangleMesh;
import de.embl.schwab.crosshair.bdv.PointsOverlaySizeChange;
import de.embl.schwab.crosshair.utils.BdvUtils;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij.IJ;
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

import java.awt.Color;
import java.util.*;

import static de.embl.cba.bdv.utils.BdvUtils.getBdvWindowCentre;
import static de.embl.cba.bdv.utils.BdvUtils.moveToPosition;

public class PlaneManager {

    private int trackPlane = 0;
    private int pointMode = 0;
    private int vertexMode = 0;

    private double distanceBetweenPlanesThreshold;

    private final Map<String, Vector3d> planeNormals;
    private final Map<String, Vector3d> planePoints;
    private final Map<String, Vector3d> planeCentroids;
    private final Map<String, RealPoint> namedVertices;
    private final Map<String, RealPoint> selectedVertex;

    private final ArrayList<RealPoint> pointsToFitPlane;
    private final ArrayList<RealPoint> blockVertices;



    private final BdvHandle bdvHandle;
    private final BdvStackSource bdvStackSource;
    private final Image3DUniverse universe;
    private final Content imageContent;
    private PointsOverlaySizeChange pointOverlay;

    private Color3f targetPlaneColour;
    private Color3f blockPlaneColour;
    private Color3f alignedPlaneColour;

    private float targetTransparency;
    private float blockTransparency;

    private boolean targetVisible;
    private boolean blockVisible;


    public PlaneManager(BdvStackSource bdvStackSource, Image3DUniverse universe, Content imageContent) {
        planeNormals = new HashMap<>();
        planePoints = new HashMap<>();
        planeCentroids = new HashMap<>();
        selectedVertex = new HashMap<>();

        namedVertices = new HashMap<>();
        pointsToFitPlane = new ArrayList<>();
        blockVertices = new ArrayList<>();

        this.bdvStackSource = bdvStackSource;
        this.bdvHandle = bdvStackSource.getBdvHandle();
        this.universe = universe;
        this.imageContent = imageContent;

        targetPlaneColour = new Color3f(0, 1, 0);
        blockPlaneColour = new Color3f(0, 0, 1);
        alignedPlaneColour = new Color3f(1, 0, 0);
        targetTransparency = 0.7f;
        blockTransparency = 0.7f;

        targetVisible = true;
        blockVisible = true;

        // TODO - make this threshold user definable - makes sense for microns, but possibly not for other units
        distanceBetweenPlanesThreshold = 1E-10;
    }

    public Map<String, RealPoint>getSelectedVertex() {
        return selectedVertex;
    }

    public Map<String, Vector3d> getPlaneNormals() { return planeNormals; }

    public Map<String, Vector3d> getPlanePoints() { return planePoints; }

    public Map<String, Vector3d> getPlaneCentroids() { return planeCentroids; }

    public Map<String, RealPoint> getNamedVertices() {
        return namedVertices;
    }

    public ArrayList<RealPoint> getPointsToFitPlane() {return pointsToFitPlane;}
    public ArrayList<RealPoint> getBlockVertices() {return blockVertices;}
    public float getTargetTransparency() {return targetTransparency;}
    public float getBlockTransparency() {return blockTransparency;}
    public Color3f getTargetPlaneColour() {return targetPlaneColour;}
    public Color3f getBlockPlaneColour() {return blockPlaneColour;}
    public int getTrackPlane() {return trackPlane;}
    public void setTrackPlane(int track) {trackPlane = track;}

    public void setPointOverlay (PointsOverlaySizeChange pointOverlay) {
        this.pointOverlay = pointOverlay;
    }

    public int getPointMode() {
        return pointMode;
    }

    public void setPointMode(int pointMode) {
        this.pointMode = pointMode;
        pointOverlay.setPointMode(pointMode);
    }

    public int getVertexMode() {
        return vertexMode;
    }

    public void setVertexMode(int vertexMode) {
        this.vertexMode = vertexMode;
        pointOverlay.setVertexMode(vertexMode);
    }

    public void setTargetPlaneColour (Color colour) {
        targetPlaneColour.set(colour);
        if (checkNamedPlaneExists("target")) {
            universe.getContent("target").setColor(new Color3f(targetPlaneColour));
        }
    }

    public void setTargetPlaneAlignedColour () {
        Color3f currentColour = universe.getContent("target").getColor();
        Color3f alignedColour = new Color3f(alignedPlaneColour);
        if (currentColour != alignedColour) {
            universe.getContent("target").setColor(alignedColour);
        }
    }

    public void setTargetPlaneNotAlignedColour() {
        Color3f currentColour = universe.getContent("target").getColor();
        Color3f notAlignedColour = new Color3f(targetPlaneColour);
        if (currentColour != notAlignedColour) {
            universe.getContent("target").setColor(notAlignedColour);
        }
    }

    public void setBlockPlaneColour (Color colour) {
        blockPlaneColour.set(colour);
        if (checkNamedPlaneExists("block")) {
            universe.getContent("block").setColor(new Color3f(blockPlaneColour));
        }
    }

    public void setTargetTransparency (float transparency) {
        targetTransparency = transparency;
        if (checkNamedPlaneExists("target")) {
            universe.getContent("target").setTransparency(targetTransparency);
        }
    }

    public void setBlockTransparency (float transparency) {
        blockTransparency = transparency;
        if (checkNamedPlaneExists("block")) {
            universe.getContent("block").setTransparency(blockTransparency);
        }
    }

    public void nameSelectedVertex(String name) {
        if (!selectedVertex.containsKey("selected")) {
            IJ.log("No vertex selected");
        } else {

            RealPoint selectedPointCopy = new RealPoint(selectedVertex.get("selected"));
            nameVertex(name, selectedPointCopy);
        }
    }

    public void nameVertex (String name, RealPoint vertex) {
        RealPoint vertexCopy = new RealPoint(vertex);
        if (name.equals("Top Left")) {
            renamePoint3D(imageContent, vertexCopy, "TL");
            addNamedVertexBdv(name, vertexCopy);
        } else if (name.equals("Top Right")) {
            renamePoint3D(imageContent, vertexCopy, "TR");
            addNamedVertexBdv(name, vertexCopy);
        } else if (name.equals("Bottom Left")) {
            renamePoint3D(imageContent, vertexCopy, "BL");
            addNamedVertexBdv(name, vertexCopy);
        } else if (name.equals("Bottom Right")) {
            renamePoint3D(imageContent, vertexCopy, "BR");
            addNamedVertexBdv(name, vertexCopy);
        }
    }

    private void addNamedVertexBdv (String vertexName, RealPoint point) {
        removeMatchingNamedVertices(point);
        namedVertices.put(vertexName, point);
        bdvHandle.getViewerPanel().requestRepaint();
    }

    private void removeMatchingNamedVertices (RealPoint point) {
        ArrayList<String> keysToRemove = new ArrayList<>();
        for (String key : namedVertices.keySet()) {
            if ( GeometryUtils.checkTwoRealPointsSameLocation(namedVertices.get(key), point)) {
                keysToRemove.add(key);
            }
        }

        for (String key : keysToRemove) {
            namedVertices.remove(key);
        }
    }

    private void removeMatchingSelectdVertices (RealPoint point) {
        if (selectedVertex.containsKey("selected")) {
            if (GeometryUtils.checkTwoRealPointsSameLocation(selectedVertex.get("selected"), point)) {
                selectedVertex.remove("selected");
            }
        }
    }

    private void renamePoint3D(Content content, RealPoint point, String name) {
        // rename any points with that name to "" to enforce only one point with each name
        BenesNamedPoint existingPointWithName = content.getPointList().get(name);
        if (existingPointWithName != null) {
            content.getPointList().rename(existingPointWithName, "");
        }

        double[] pointCoord = new double[3];
        point.localize(pointCoord);
        int pointIndex = content.getPointList().indexOfPointAt(pointCoord[0], pointCoord[1], pointCoord[2], content.getLandmarkPointSize());
        content.getPointList().rename(content.getPointList().get(pointIndex), name);
    }

    public boolean checkNamedPlaneExists(String name) {
        return planeNormals.containsKey(name);
    }

    public void updatePlaneOnTransformChange(AffineTransform3D affineTransform3D, String planeName) {
        ArrayList<Vector3d> planeDefinition = getPlaneDefinitionFromViewTransform(affineTransform3D);
        updatePlane(planeDefinition.get(0), planeDefinition.get(1), planeName);
    }

    public void updatePlaneCurrentView (String planeName) {
        ArrayList<Vector3d> planeDefinition = getPlaneDefinitionOfCurrentView();
        updatePlane(planeDefinition.get(0), planeDefinition.get(1), planeName);
    }

    public void redrawCurrentPlanes () {
        for (String planeName: planeNormals.keySet()) {
            updatePlane(planeNormals.get(planeName), planePoints.get(planeName), planeName );
        }
    }

    public ArrayList<Vector3d> getPlaneDefinitionOfCurrentView () {
        final AffineTransform3D transform = new AffineTransform3D();
        bdvHandle.getViewerPanel().getState().getViewerTransform( transform );

        ArrayList<Vector3d> planeDefinition = getPlaneDefinitionFromViewTransform(transform);

        return planeDefinition;
    }

    private ArrayList<Vector3d> getPlaneDefinitionFromViewTransform(AffineTransform3D affineTransform3D) {
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

        ArrayList<Vector3d> planeDefinition = new ArrayList<>();

        Vector3d planeNormal = GeometryUtils.calculateNormalFromPoints(globalPoints);
        Vector3d planePoint = new Vector3d(globalPoints.get(0)[0], globalPoints.get(0)[1], globalPoints.get(0)[2]);
        planeDefinition.add(planeNormal);
        planeDefinition.add(planePoint);

        return planeDefinition;
    }

    public double[] getGlobalViewCentre () {
        final AffineTransform3D transform = new AffineTransform3D();
        bdvHandle.getViewerPanel().getState().getViewerTransform( transform );
        double[] centrePointView = getBdvWindowCentre(bdvStackSource);
        double[] centrePointGlobal = new double[3];
        transform.inverse().apply(centrePointView, centrePointGlobal);

        return centrePointGlobal;
    }

    public void updatePlane(Vector3d planeNormal, Vector3d planePoint, String planeName) {

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
            planeNormals.put(planeName, planeNormal);
            planePoints.put(planeName, planePoint);
            planeCentroids.put(planeName, GeometryUtils.getCentroid(intersectionPoints));

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

            Vector3d transformedNormal = new Vector3d(planeNormal.getX(), planeNormal.getY(), planeNormal.getZ());
            rotate.transform(transformedNormal);

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
                // make copy of colour to assign (using original interferes with changing colour later)
                planeColor = new Color3f(targetPlaneColour);
                transparency = targetTransparency;

            } else if (planeName.equals("block")) {
                planeColor = new Color3f(blockPlaneColour);
                transparency = blockTransparency;
            }

            CustomTriangleMesh newMesh = null;
            if (intersectionPoints.size() == 3) {
                newMesh = new CustomTriangleMesh(vectorPoints, planeColor, transparency);
            } else if (intersectionPoints.size() > 3) {
                ArrayList<Point3f> triangles = GeometryUtils.calculateTrianglesFromPoints(intersectionPoints, transformedNormal);
                newMesh = new CustomTriangleMesh(triangles, planeColor, transparency);
            }
            Content meshContent = universe.addCustomMesh(newMesh, planeName);
            meshContent.setLocked(true);
            if (planeName.equals("target")) {
                meshContent.setVisible(targetVisible);
            } else if (planeName.equals("block")) {
                meshContent.setVisible(blockVisible);
            }

        }
    }

        public void moveViewToNamedPlane (String name) {
            // check if you're already at the plane
            ArrayList<Vector3d> planeDefinition = getPlaneDefinitionOfCurrentView();
            Vector3d currentPlaneNormal = planeDefinition.get(0);
            Vector3d currentPlanePoint = planeDefinition.get(1);

            boolean normalsParallel = GeometryUtils.checkVectorsParallel(planeNormals.get(name), currentPlaneNormal);
            double distanceToPlane = GeometryUtils.distanceFromPointToPlane(currentPlanePoint, planeNormals.get(name), planePoints.get(name));

            // units may want to be more or less strict
            // necessary due to double precision, will very rarely get exactly the same value
            boolean pointInPlane = distanceToPlane < distanceBetweenPlanesThreshold;

            if (normalsParallel & pointInPlane) {
                IJ.log("Already at that plane");
            } else {
                double[] targetNormal = new double[3];
                planeNormals.get(name).get(targetNormal);

                double[] targetCentroid = new double[3];
                planeCentroids.get(name).get(targetCentroid);
                moveToPosition(bdvStackSource, targetCentroid, 0, 0);
                if (!normalsParallel) {
                    BdvUtils.levelCurrentView(bdvStackSource, targetNormal);
                }
            }
        }

        public void addRemoveCurrentPositionPointsToFitPlane() {
            RealPoint point = getCurrentPosition();
            addRemovePointFromPointList(pointsToFitPlane, point);
        }

        public void addRemoveCurrentPositionBlockVertices () {
            // Check if on the current block plane
            RealPoint point = getCurrentPosition();
            double[] position = new double[3];
            point.localize(position);
            double distanceToPlane = GeometryUtils.distanceFromPointToPlane(new Vector3d(position), planeNormals.get("block"), planePoints.get("block"));

            // units may want to be more or less strict
            if (distanceToPlane < distanceBetweenPlanesThreshold) {
                addRemovePointFromPointList(blockVertices, point);
            } else {
                IJ.log("Vertex points must lie on the block plane");
            }
        }

        public void addRemovePointFromPointList(ArrayList<RealPoint> points, RealPoint point) {
        // remove point if already present, otherwise add point
            double[] pointViewerCoords = convertToViewerCoordinates(point);

            boolean distanceMatch = false;
            for ( int i = 0; i < points.size(); i++ )
            {
                RealPoint currentPoint = points.get(i);
                double[] currentPointViewerCoords = convertToViewerCoordinates(currentPoint);
                double distance = GeometryUtils.distanceBetweenPoints(pointViewerCoords, currentPointViewerCoords);
                if (distance < 5) {
                    removePointFrom3DViewer(currentPoint);
                    // remove matching points from named vertices
                    removeMatchingNamedVertices(currentPoint);
                    // remove matching points from selected vertices
                    removeMatchingSelectdVertices(currentPoint);
                    points.remove(i);
                    bdvHandle.getViewerPanel().requestRepaint();

                    distanceMatch = true;
                    break;
                }

            }

            if (!distanceMatch) {
                points.add(point);
                bdvHandle.getViewerPanel().requestRepaint();

                double[] position = new double[3];
                point.localize(position);
                imageContent.getPointList().add("", position[0], position[1], position[2]);
            }

        }
    public void removeAllBlockVertices() {
        for (RealPoint point : blockVertices) {
            removePointFrom3DViewer(point);
        }
        namedVertices.clear();
        blockVertices.clear();
        selectedVertex.clear();
        bdvHandle.getViewerPanel().requestRepaint();
    }

    public void removeAllPointsToFitPlane() {
        for (RealPoint point : pointsToFitPlane) {
            removePointFrom3DViewer(point);
        }
        pointsToFitPlane.clear();
        bdvHandle.getViewerPanel().requestRepaint();
    }

    public void removeNamedPlane (String name) {
        if (checkNamedPlaneExists(name)) {
        //        remove block vertices as these are tied to a particular plane (unlike the points)
            if (name.equals("block")) {
                removeAllBlockVertices();
            }

            planeNormals.remove(name);
            planeCentroids.remove(name);
            planePoints.remove(name);

            universe.removeContent(name);
        }

    }

    private void removePointFrom3DViewer (RealPoint point) {
        // remove from 3D view and bdv
        double[] chosenPointCoord = new double[3];
        point.localize(chosenPointCoord);

        int pointIndex = imageContent.getPointList().indexOfPointAt(chosenPointCoord[0], chosenPointCoord[1], chosenPointCoord[2], imageContent.getLandmarkPointSize());
        imageContent.getPointList().remove(pointIndex);

        //		There's a bug in how the 3D viewer displays points after one is removed. Currently, it just stops
        //		displaying the first point added (rather than the one you actually removed).
        //		Therefore here I remove all points and re-add them, to get the viewer to reset how it draws
        //		the points. Perhaps there's a more efficient way to get around this?
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

    public void toggleSelectedVertexCurrentPosition () {
        double[] pointViewerCoords = getCurrentPositionViewerCoordinates();
        for ( int i = 0; i < blockVertices.size(); i++ ) {
            double[] currentPointViewerCoords = convertToViewerCoordinates(blockVertices.get(i));
            double distance = GeometryUtils.distanceBetweenPoints(pointViewerCoords, currentPointViewerCoords);
            if (distance < 5) {
                RealPoint selection = new RealPoint(3);
                selection.setPosition(blockVertices.get(i));

                // if point already selected, deselect it, otherwise add
                if (selectedVertex.containsKey("selected")) {
                    if ( GeometryUtils.checkTwoRealPointsSameLocation(selectedVertex.get("selected"), selection)) {
                        selectedVertex.clear();
                        bdvHandle.getViewerPanel().requestRepaint();
                        break;
                    } else {
                        selectedVertex.put("selected", selection);
                        bdvHandle.getViewerPanel().requestRepaint();
                        break;
                    }
                } else {
                    selectedVertex.put("selected", selection);
                    bdvHandle.getViewerPanel().requestRepaint();
                    break;
                }
            }
        }
    }

    public void toggleTargetVisbility () {
        if (checkNamedPlaneExists("target")) {
            if (targetVisible) {
                universe.getContent("target").setVisible(false);
                targetVisible = false;
            } else {
                universe.getContent("target").setVisible(true);
                targetVisible = true;
            }
        }
    }

    public void toggleBlockVisbility () {
        if (checkNamedPlaneExists("block")) {
            if (blockVisible) {
                universe.getContent("block").setVisible(false);
                blockVisible = false;
            } else {
                universe.getContent("block").setVisible(true);
                blockVisible = true;
            }
        }
    }

    public Boolean getVisiblityNamedPlane (String name) {
        if (name.equals("target")) {
            return targetVisible;
        } else if (name.equals("block")) {
            return blockVisible;
        } else {
            return null;
        }
    }

    public boolean checkAllPlanesPointsDefined() {
        boolean targetExists = checkNamedPlaneExists("target");
        boolean blockExists = checkNamedPlaneExists("block");

        boolean allVerticesExist = true;
        String[] vertexPoints = {"Top Left", "Top Right", "Bottom Left", "Bottom Right"};
        for (String vertexName: vertexPoints) {
            if (!namedVertices.containsKey(vertexName)) {
                allVerticesExist = false;
                break;
            }
        }

        if (targetExists & blockExists & allVerticesExist) {
            return true;
        } else {
            return false;
        }

    }
}
