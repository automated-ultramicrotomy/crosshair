package de.embl.schwab.crosshair;

import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.java3d.Transform3D;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static de.embl.schwab.crosshair.utils.BdvUtils.addSourceToUniverse;

/**
 * Test helper class to initiate a BigDataViewer + 3D viewer window displaying a test image.
 *
 * Note: this uses static fields to ensure that the same Bdv/3D window can be re-used across all the tests. This speeds
 * up the tests (as initiating the viewers is quite slow) + also avoids heap errors when run via github actions. Even
 * when running all the relevant close methods e.g. universe.close(), universe.cleanup(),
 * bdvStackSource.getBdvHandle().close() some references still seem to remain open, which eventually causes a
 * heap/memory error if multiple windows are opened sequentially on github actions. Re-using a single window is a
 * workaround for this - but it would be good to find the underlying cause at some point + report the issue to these
 * external libraries.
 */
public class BdvAnd3DViewer {
    private static BdvStackSource bdvStackSource;
    private static AffineTransform3D initialViewerTransform;
    private static Image3DUniverse universe;
    private static Content imageContent;

    public static BdvStackSource getBdvStackSource() {
        if (bdvStackSource == null) {
            createBdvAnd3DViewer();
        }
        return bdvStackSource;
    }

    public static AffineTransform3D getInitialViewerTransform() {
        if (initialViewerTransform == null) {
            createBdvAnd3DViewer();
        }
        return initialViewerTransform;
    }

    public static Image3DUniverse getUniverse() {
        if (universe == null) {
            createBdvAnd3DViewer();
        }
        return universe;
    }

    public static Content getImageContent() {
        if (imageContent == null) {
            createBdvAnd3DViewer();
        }
        return imageContent;
    }

    /**
     * Reset Bdv + 3D viewer to their initial state i.e. reset any applied transforms, remove any additional meshes
     * to only leave the test image.
     */
    public static void reset() {
        if (universe != null & imageContent != null) {
            reset3DViewer();
        }

        if (bdvStackSource != null & initialViewerTransform != null) {
            resetBdv();
        }
    }

    private static void createBdvAnd3DViewer() {
        ClassLoader classLoader = BdvAnd3DViewer.class.getClassLoader();
        File imageFile = new File(classLoader.getResource("exampleBlock.xml").getFile());
        LazySpimSource imageSource = new LazySpimSource("raw", imageFile.getAbsolutePath());

        // Setup 3D viewer
        universe = new Image3DUniverse();
        imageContent = addSourceToUniverse(universe, imageSource, 300 * 300 * 300,
                Content.VOLUME, 0, 255 );
        universe.show();

        // Setup BigDataViewer
        bdvStackSource = BdvFunctions.show(imageSource, 1);

        // There are slight differences in the default bdv size and transform from BdvFunctions.show between Github CI +
        // local running. To avoid any discrepancies, set the size and transform directly here:
        int width = 800;
        int height = 577;
        bdvStackSource.getBdvHandle().getViewerPanel().setSize(width, height);
        bdvStackSource.getBdvHandle().getViewerPanel().getDisplay().setSize(width, height);

        initialViewerTransform = new AffineTransform3D();
        initialViewerTransform.set(
                0.47261361983944955, 0.0, 0.0, 112.4425,
                0.0, 0.47261361983944955, 0.0, 0.9424999999999955,
                0.0, 0.0, 0.47261361983944955, -188.25276146804035
        );
        resetBdv();
    }

    private static void reset3DViewer() {
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

        // reset image content orientation
        imageContent.setTransform(new Transform3D());

        // reset 3D viewer view
        universe.resetView();
    }

    private static void resetBdv() {
        bdvStackSource.getBdvHandle().getViewerPanel().state().setViewerTransform(initialViewerTransform);
    }
}