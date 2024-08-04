package de.embl.schwab.crosshair;

import bdv.util.Bdv;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestHelpers {

    public static void reset3DViewer(Image3DUniverse universe, Content imageContent) {
        // remove all content (apart from image) from 3D viewer
        List<String> contentsToRemove = new ArrayList<String>();
        for (Object content: universe.getContents()) {
            String contentName = ((Content) content).getName();
            if (!Objects.equals(contentName, imageContent.getName())) {
                contentsToRemove.add(contentName);
            }
        }
        for (String contentName: contentsToRemove) {
            universe.removeContent(contentName);
        }

        // reset 3D viewer view
        universe.resetView();
    }

    public static void resetBdv(Bdv bdvHandle, AffineTransform3D initialTransform) {
        bdvHandle.getBdvHandle().getViewerPanel().state().setViewerTransform(initialTransform);
    }
}
