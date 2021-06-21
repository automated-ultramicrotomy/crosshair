package de.embl.schwab.crosshair.plane;

import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij.IJ;
import ij3d.Content;
import net.imglib2.RealPoint;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3d;
import vib.BenesNamedPoint;
import vib.PointList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BlockPlane extends Plane {


    private final ArrayList<RealPoint> vertices; // all vertex points placed on the block plane
    private final Map<String, RealPoint> namedVertices; // a subset of vertices, that are named top right, top left etc.

    private boolean isVertexSelected;
    private int selectedVertexIndex;

    public BlockPlane( String name, Vector3d normal, Vector3d point, Vector3d centroid, Content mesh,
                       Color3f color, float transparency, boolean isVisible ) {
        super(name, normal, point, centroid, mesh, color, transparency, isVisible);
        this.vertices = new ArrayList<>();
        this.namedVertices = new HashMap<>();
        this.isVertexSelected = false;
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

    public void addRemoveCurrentPositionBlockVertices () {
        // Check if on the current block plane
        RealPoint point = getCurrentPosition();
        double[] position = new double[3];
        point.localize(position);

        Plane blockPlane = planeNameToPlane.get( Crosshair.block );
        double distanceToPlane = GeometryUtils.distanceFromPointToPlane(new Vector3d(position),
                blockPlane.getNormal(), blockPlane.getPoint() );

        // units may want to be more or less strict
        if (distanceToPlane < distanceBetweenPlanesThreshold) {
            addRemovePointFromPointList(blockVertices, point);
        } else {
            IJ.log("Vertex points must lie on the block plane");
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
}
