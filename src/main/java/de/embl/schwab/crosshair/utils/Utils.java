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

    /**
     * Space out the three Crosshair windows, with the JFrame on the left (with controls), big data viewer in the
     * middle and the 3D viewer on the right.
     * Based on
     * https://github.com/mobie/mobie-viewer-fiji/blob/main/src/main/java/org/embl/mobie/ui/WindowArrangementHelper.java
     * @param frame JFrame containing controls
     * @param bdvHandle bdvHandle of the BigDataViewer window
     * @param universe universe of the 3D viewer
     */
    public static void spaceOutWindows( JFrame frame, BdvHandle bdvHandle, Image3DUniverse universe ) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Window viewFrame = SwingUtilities.getWindowAncestor(bdvHandle.getViewerPanel());

        // Make sure bigdataviewer + 3D viewer aren't taller than the screen
        int viewHeight = viewFrame.getHeight();
        if (viewHeight > screenSize.getHeight()) {
            viewHeight = (int) Math.floor(screenSize.getHeight());
        }

        // Place the 3D viewer in the top right corner, and set its width to a third of the remaining space
        // (after the controls width is taken into account)
        int width3DViewer = (int) Math.floor((screenSize.getWidth() - frame.getWidth())/3.0);
        universe.setSize(width3DViewer, viewHeight);
        universe.getWindow().setLocation((int) Math.floor(screenSize.getWidth() - width3DViewer),
                frame.getLocationOnScreen().y);

        // Fill any remaining width between the controls and 3D viewer with the bdv window
        viewFrame.setLocation(
                frame.getLocationOnScreen().x + frame.getWidth(),
                frame.getLocationOnScreen().y );
        int newViewWidth = (int) Math.floor(screenSize.width - width3DViewer - frame.getWidth());
        viewFrame.setSize(newViewWidth, viewHeight);
    }

    public static void resetCrossPlatformSwingLookAndFeel() {
        try {
            UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName() );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
