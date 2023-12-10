package de.embl.schwab.crosshair.points.overlays;

import ij3d.Content;
import net.imglib2.RealPoint;
import vib.BenesNamedPoint;
import vib.PointList;

import java.util.Iterator;
import java.util.List;

/**
 * Class to manage display of points in the 3D viewer
 * Note: all 3D points / vertices for all planes are currently in the same point list. This means that overlapping
 * points are not allowed (while this is allowed in the 2D viewer, as each plane gets its own 2D overlay).
 * This is a limitation of ImageJ 3D Viewer. All point lists must be associated with an imagecontent (of which we
 * only have 1), and only one per imageContent. Swapping to SciView at some point would be a good plan
 * (this can be done with point meshes in the 3d viewer, but points can't be named, and are harder to remove)
 */
public class Point3dOverlay {

    private Content imageContent;

    /**
     * Make a 3D point overlay for the given image content
     * @param imageContent image content (displayed in 3D viewer) to associate points with
     */
    public Point3dOverlay( Content imageContent ) {
        this.imageContent = imageContent;
    }

    /**
     * Add point
     * @param point 3D point
     */
    public void addPoint( RealPoint point ) {
        double[] position = new double[3];
        point.localize(position);
        imageContent.getPointList().add("", position[0], position[1], position[2]);
    }

    /**
     * Remove given points
     * @param points List of 3D points to remove
     */
    public void removePoints( List<RealPoint> points ) {
        for( RealPoint point: points ) {
            removePointFromPointList( point );
        }

        refreshPoints();
    }

    /**
     * Remove given point
     * @param point 3D point
     */
    public void removePoint( RealPoint point ) {
        removePointFromPointList( point );
        refreshPoints();
    }

    private void removePointFromPointList( RealPoint point ) {
        double[] chosenPointCoord = new double[3];
        point.localize(chosenPointCoord);

        int pointIndex = imageContent.getPointList().indexOfPointAt(chosenPointCoord[0], chosenPointCoord[1], chosenPointCoord[2], imageContent.getLandmarkPointSize());
        imageContent.getPointList().remove(pointIndex);
    }

    private void refreshPoints() {
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

    /**
     * Rename the given 3D point
     * @param point 3D point to rename
     * @param name new name
     */
    public void renamePoint3D( RealPoint point, String name ) {
        // rename any points with that name to "" to enforce only one point with each name
        BenesNamedPoint existingPointWithName = imageContent.getPointList().get(name);
        if (existingPointWithName != null) {
            imageContent.getPointList().rename(existingPointWithName, "");
        }

        double[] pointCoord = new double[3];
        point.localize(pointCoord);
        int pointIndex = imageContent.getPointList().indexOfPointAt(
                pointCoord[0], pointCoord[1], pointCoord[2], imageContent.getLandmarkPointSize());
        imageContent.getPointList().rename(imageContent.getPointList().get(pointIndex), name);
    }
}
