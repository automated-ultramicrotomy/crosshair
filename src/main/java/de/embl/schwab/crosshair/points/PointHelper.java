package de.embl.schwab.crosshair.points;

import ij3d.Content;
import net.imglib2.RealPoint;
import vib.BenesNamedPoint;
import vib.PointList;

import java.util.Iterator;

public class PointHelper {
    public static void removePointFrom3DViewer ( Content imageContent, RealPoint point ) {

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
