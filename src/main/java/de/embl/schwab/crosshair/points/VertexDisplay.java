package de.embl.schwab.crosshair.points;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import de.embl.schwab.crosshair.plane.Plane;
import de.embl.schwab.crosshair.points.overlays.Point3dOverlay;
import de.embl.schwab.crosshair.points.overlays.VertexPoints2dOverlay;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij.IJ;
import ij3d.Content;
import net.imglib2.RealPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static de.embl.schwab.crosshair.points.PointHelper.getCurrentMousePosition;
import static de.embl.schwab.crosshair.points.PointHelper.getMatchingPointWithinDistance;

/**
 * Class to control 2D and 3D display of vertices (i.e. named block face corners)
 * Each block plane will get its own VertexDisplay.
 */
public class VertexDisplay {

    private final ArrayList<RealPoint> vertices; // all vertex points placed on the block plane
    private final Map<VertexPoint, RealPoint> assignedVertices; // the subset of assigned vertices e.g. top left, top right...

    private transient boolean isVertexSelected;
    private transient RealPoint selectedVertex;

    private final VertexPoints2dOverlay vertex2dOverlay;
    private final Point3dOverlay vertex3dOverlay;
    private final Bdv bdv;
    private final String name;
    private final String sourceName;

    /**
     * Create a vertex display (starting with no vertices)
     * @param name Block plane name
     * @param bdv BigDataViewer window to show 2D vertices on
     * @param imageContent image content (displayed in 3D viewer) to show 3D vertices on
     */
    public VertexDisplay( String name, Bdv bdv, Content imageContent ) {
        this( new ArrayList<>(), new HashMap<>(), name, bdv, imageContent );
    }

    /**
     * Create a vertex display (starting with list of vertices)
     * @param vertices List of vertices
     * @param assignedVertices Map of vertex assignment to vertex (e.g. top left, top right..)
     * @param name Block plane name
     * @param bdv BigDataViewer window to show 2D vertices on
     * @param imageContent image content (displayed in 3D viewer) to show 3D vertices on
     */
    public VertexDisplay( ArrayList<RealPoint> vertices, Map<VertexPoint, RealPoint> assignedVertices,
                                    String name, Bdv bdv, Content imageContent ) {
        this.vertices = vertices;
        this.assignedVertices = assignedVertices;
        this.isVertexSelected = false;

        this.vertex2dOverlay = new VertexPoints2dOverlay( this );
        this.vertex3dOverlay = new Point3dOverlay( imageContent );
        this.bdv = bdv;
        this.name = name;
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

    /**
     * Assign selected vertex as top left, top right etc...
     * @param vertexPoint  Assignment
     */
    public void assignSelectedVertex(VertexPoint vertexPoint ) {
        if ( !isVertexSelected ) {
            IJ.log("No vertex selected");
        } else {
            assignVertex( vertexPoint, selectedVertex );
        }
    }

    /**
     * Assign given vertex as top left, top right etc...
     * @param vertexPoint Assignment
     * @param vertex vertex to assign
     */
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

    /**
     * Add vertex at current mouse position in BigDataViewer window - only if it lies on the given plane!
     * If there's already a vertex there, then remove it instead.
     * @param plane Plane vertex must lie on
     */
    public void addOrRemoveCurrentPositionFromVertices( Plane plane ) {
        // Check if on the current block plane
        RealPoint point = getCurrentMousePosition( bdv.getBdvHandle() );
        if ( plane.isPointOnPlane( point ) ) {
            addOrRemoveVertex( point );
        } else {
            IJ.log("Vertex points must lie on the " + name + " plane");
        }
    }

    /**
     * Add vertex at current mouse position in BigDataViewer window (no requirement to be on specific plane)
     * If there's already a vertex there, then remove it instead.
     */
    public void addOrRemoveCurrentPositionFromVertices() {
        RealPoint point = getCurrentMousePosition( bdv.getBdvHandle() );
        addOrRemoveVertex( point );
    }

    /**
     * If there's a vertex at the current mouse position (in BigDataViewer window), then toggle its selection status.
     */
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
