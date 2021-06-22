package de.embl.schwab.crosshair.plane;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import de.embl.schwab.crosshair.points.Point3dOverlay;
import de.embl.schwab.crosshair.points.VertexPoint;
import de.embl.schwab.crosshair.points.VertexPoints2dOverlay;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij.IJ;
import ij3d.Content;
import net.imglib2.RealPoint;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static de.embl.schwab.crosshair.points.PointHelper.*;

public class BlockPlane extends Plane {


    private final ArrayList<RealPoint> vertices; // all vertex points placed on the block plane
    private final Map<VertexPoint, RealPoint> assignedVertices; // the subset of assigned vertices e.g. top left, top right...

    private boolean isVertexSelected;
    private RealPoint selectedVertex;

    // visualisation 2d
    private VertexPoints2dOverlay vertexPoints2dOverlay;

    public BlockPlane( String name, Vector3d normal, Vector3d point, Vector3d centroid, Content mesh, Color3f color,
                       float transparency, boolean isVisible, Bdv bdv, Point3dOverlay point3dOverlay ) {

        super( name, normal, point, centroid, mesh, color, transparency, isVisible, bdv, point3dOverlay );
        this.vertices = new ArrayList<>();
        this.assignedVertices = new HashMap<>();
        this.isVertexSelected = false;

        this.vertexPoints2dOverlay = new VertexPoints2dOverlay( this );
        BdvFunctions.showOverlay( vertexPoints2dOverlay, name + "-vertex_points",
                Bdv.options().addTo(bdv) );
    }

    public boolean isVertexSelected() {
        return isVertexSelected;
    }

    public RealPoint getSelectedVertex() {
        return selectedVertex;
    }

    public ArrayList<RealPoint> getVertices() {
        return vertices;
    }

    public Map<VertexPoint, RealPoint> getAssignedVertices() {
        return assignedVertices;
    }

    public ArrayList<RealPoint> getVerticesExceptForSelected() {
        if ( isVertexSelected ) {
            ArrayList<RealPoint> nonSelectedVertices = new ArrayList<>();
            for ( RealPoint vertex: vertices ) {
                if ( vertex != selectedVertex ) {
                    nonSelectedVertices.add( vertex );
                }
            }
            return nonSelectedVertices;
        } else {
            return vertices;
        }
    }

    public VertexPoints2dOverlay getVertexPoints2dOverlay() {
        return vertexPoints2dOverlay;
    }

    public void assignSelectedVertex(VertexPoint vertexPoint ) {
        if ( !isVertexSelected ) {
            IJ.log("No vertex selected");
        } else {
            assignVertex( vertexPoint, selectedVertex );
        }
    }

    public void assignVertex( VertexPoint vertexPoint, RealPoint vertex ) {
        // enforce unique vertex point assignments i.e. remove any already assigned to that vertex
        removeAssignedVertex( vertex );
        assignedVertices.put( vertexPoint, vertex );

        RealPoint vertexCopy = new RealPoint( vertex );
        point3dOverlay.renamePoint3D( vertexCopy, vertexPoint.toShortString() );
        bdv.getBdvHandle().getViewerPanel().requestRepaint();
    }

    public void addOrRemoveCurrentPositionFromVertices() {
        // Check if on the current block plane
        RealPoint point = getCurrentMousePosition( bdv.getBdvHandle() );
        if ( isPointOnPlane( point ) ) {
            // remove point if within a certain distance of an existing point, otherwise add point
            RealPoint matchingPointWithinDistance = getMatchingPointWithinDistance( vertices, point, bdv.getBdvHandle());

            if ( matchingPointWithinDistance != null ) {
                removeVertex( point );
            } else {
                addVertex( point );
            }
        } else {
            IJ.log("Vertex points must lie on the block plane");
        }
    }

    public void toggleSelectedVertexCurrentPosition () {

        RealPoint point = getCurrentMousePosition( bdv.getBdvHandle() );
        RealPoint matchingPointWithinDistance = getMatchingPointWithinDistance( vertices, point, bdv.getBdvHandle());

        if ( matchingPointWithinDistance != null ) {
            // if selected, unselect
            if ( selectedVertex != null && selectedVertex == matchingPointWithinDistance && isVertexSelected ) {
                selectedVertex = null;
                isVertexSelected = false;
            // if unselected, select
            } else {
                selectedVertex = matchingPointWithinDistance;
                isVertexSelected = true;
            }
        }
    }

    public void addVertex( RealPoint point ) {
        vertices.add( point );
        point3dOverlay.addPoint( point );
        bdv.getBdvHandle().getViewerPanel().requestRepaint();
    }

    public void removeVertex( RealPoint point ) {
        vertices.remove( point );
        point3dOverlay.removePoint( point );

        if ( selectedVertex.equals( point ) ) {
            selectedVertex = null;
            isVertexSelected = false;
        }

        removeAssignedVertex( point );

        bdv.getBdvHandle().getViewerPanel().requestRepaint();
    }

    private void removeAssignedVertex( RealPoint point ) {
        ArrayList<VertexPoint> keysToRemove = new ArrayList<>();
        for ( VertexPoint vertexPoint : assignedVertices.keySet() ) {
            if ( GeometryUtils.checkTwoRealPointsSameLocation( assignedVertices.get( vertexPoint ), point )) {
                keysToRemove.add( vertexPoint );
            }
        }

        for ( VertexPoint key : keysToRemove) {
            assignedVertices.remove(key);
        }
    }

    public void removeAllVertices() {
        for ( RealPoint point : vertices ) {
            point3dOverlay.removePoint( point );
        }

        assignedVertices.clear();
        vertices.clear();
        isVertexSelected = false;
        selectedVertex = null;
        bdv.getBdvHandle().getViewerPanel().requestRepaint();
    }
}
