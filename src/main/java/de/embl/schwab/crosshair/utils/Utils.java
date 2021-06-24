package de.embl.schwab.crosshair.utils;

import bdv.util.BdvHandle;
import ij3d.Content;
import ij3d.Image3DUniverse;
import org.scijava.vecmath.Point3d;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class Utils {

    public static void printImageMinMax (Content imageContent) {
        Point3d min = new Point3d();
        Point3d max = new Point3d();
        imageContent.getMax(max);
        imageContent.getMin(min);
        System.out.println(min.toString());
        System.out.println(max.toString());
    }

    public static int findIndexOfMaxMin (ArrayList<Double> values, String MinMax) {
        double chosenValue = 0;
        if (MinMax.equals("max")) {
            chosenValue = Collections.max(values);
        } else if (MinMax.equals("min")) {
            chosenValue = Collections.min(values);
        }

        int result = 0;
        for (int i=0; i<values.size(); i++) {
            if (values.get(i) == chosenValue) {
                result = i;
                break;
            }
        }
        return result;

    }

    public static void spaceOutWindows( BdvHandle bdvHandle, JFrame frame, Image3DUniverse universe ) {
        // Space out windows like here:
        // https://github.com/mobie/mobie-viewer-fiji/blob/9f7367902cc0bd01e089f7ce40cdcf0ee0325f1e/src/main/java/de/embl/cba/mobie/ui/viewer/SourcesPanel.java#L369
        Window viewFrame = SwingUtilities.getWindowAncestor(bdvHandle.getViewerPanel());
        viewFrame.setLocation(
                frame.getLocationOnScreen().x + frame.getWidth(),
                frame.getLocationOnScreen().y );

        universe.getWindow().setLocation(viewFrame.getLocationOnScreen().x + viewFrame.getWidth(),
                viewFrame.getLocation().y);
    }
}
