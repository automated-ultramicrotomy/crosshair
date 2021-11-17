package de.embl.schwab.crosshair.points;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import de.embl.schwab.crosshair.plane.Plane;
import de.embl.schwab.crosshair.points.overlays.Point3dOverlay;
import de.embl.schwab.crosshair.points.overlays.VertexPoints2dOverlay;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij.IJ;
import net.imglib2.RealPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static de.embl.schwab.crosshair.points.PointHelper.getCurrentMousePosition;
import static de.embl.schwab.crosshair.points.PointHelper.getMatchingPointWithinDistance;

public class VertexDisplay {

    private final ArrayList<RealPoint> vertices; // all vertex points placed on the block plane
    private final Map<VertexPoint, RealPoint> assignedVertices; // the subset of assigned vertices e.g. top left, top right...

    private transient boolean isVertexSelected;
    private transient RealPoint selectedVertex;

    private VertexPoints2dOverlay vertex2dOverlay;
    private Point3dOverlay vertex3dOverlay;
    private Bdv bdv;
    private String sourceName;

    public VertexDisplay( String name, Bdv bdv, Point3dOverlay point3dOverlay ) {
        this( new ArrayList<>(), new HashMap<>(), name, bdv, point3dOverlay );
    }

    public VertexDisplay( ArrayList<RealPoint> vertices, Map<VertexPoint, RealPoint> assignedVertices,
                                    String name, Bdv bdv, Point3dOverlay vertex3dOverlay ) {
        this.vertices = vertices;
        this.assignedVertices = assignedVertices;
        this.isVertexSelected = false;

        this.vertex2dOverlay = new VertexPoints2dOverlay( this );
        this.vertex3dOverlay = vertex3dOverlay;
        this.bdv = bdv;
        this.sourceName = name + "-vertex_points";

        BdvFunctions.showOverlay( vertex2dOverlay, sourceName,
                Bdv.options().addTo(bdv) );


        for ( RealPoint vertex: vertices ) {
            vertex3dOverlay.addPoint( vertex );
        }

        for ( Map.Entry<VertexPoint, RealPoint> entry : assignedVertices.entrySet() ) {
            displayAssignedVertex( entry.getKey(), entry.getValue() );
        }
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

    public VertexPoints2dOverlay get2dOverlay() {
        return vertex2dOverlay;
    }

    public String getSourceName() {
        return sourceName;
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

        displayAssignedVertex( vertexPoint, vertex );
    }

    private void displayAssignedVertex( VertexPoint vertexPoint, RealPoint vertex ) {
        RealPoint vertexCopy = new RealPoint( vertex );
        vertex3dOverlay.renamePoint3D( vertexCopy, vertexPoint.toShortString() );
        bdv.getBdvHandle().getViewerPanel().requestRepaint();
    }

    // enforce lies on certain plane
    public void addOrRemoveCurrentPositionFromVertices( Plane plane ) {
        // Check if on the current block plane
        RealPoint point = getCurrentMousePosition( bdv.getBdvHandle() );
        if ( plane.isPointOnPlane( point ) ) {
            addOrRemoveVertex( point );
        } else {
            IJ.log("Vertex points must lie on the block plane");
        }
    }

    // don't enforce lies on certain plane
    public void addOrRemoveCurrentPositionFromVertices() {
        RealPoint point = getCurrentMousePosition( bdv.getBdvHandle() );
        addOrRemoveVertex( point );
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

            bdv.getBdvHandle().getViewerPanel().requestRepaint();
        }
    }

    private void addOrRemoveVertex( RealPoint point ) {
        // remove point if within a certain distance of an existing point, otherwise add point
        RealPoint matchingPointWithinDistance = getMatchingPointWithinDistance( vertices, point, bdv.getBdvHandle());

        if ( matchingPointWithinDistance != null ) {
            removeVertex( matchingPointWithinDistance );
        } else {
            addVertex( point );
        }
    }

    public void addVertex( RealPoint point ) {
        vertices.add( point );
        vertex3dOverlay.addPoint( point );
        bdv.getBdvHandle().getViewerPanel().requestRepaint();
    }

    public void removeVertex( RealPoint point ) {
        vertices.remove( point );
        vertex3dOverlay.removePoint( point );

        if ( selectedVertex != null && selectedVertex.equals( point ) ) {
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
        vertex3dOverlay.removePoints( vertices );

        assignedVertices.clear();
        vertices.clear();
        isVertexSelected = false;
        selectedVertex = null;
        bdv.getBdvHandle().getViewerPanel().requestRepaint();
    }
}
