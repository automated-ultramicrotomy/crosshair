package de.embl.schwab.crosshair;

import bdv.util.Bdv;
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

public class TestHelpers {

    public static class BdvAnd3DViewer {
        public BdvStackSource bdvStackSource;
        public AffineTransform3D initialViewerTransform;
        public Image3DUniverse universe;
        public Content imageContent;
    }

    public static BdvAnd3DViewer createBdvAnd3DViewer() {
        BdvAnd3DViewer result = new BdvAnd3DViewer();

        ClassLoader classLoader = TestHelpers.class.getClassLoader();
        File imageFile = new File(classLoader.getResource("exampleBlock.xml").getFile());
        LazySpimSource imageSource = new LazySpimSource("raw", imageFile.getAbsolutePath());

        result.bdvStackSource = BdvFunctions.show(imageSource, 1);
        result.initialViewerTransform = result.bdvStackSource.getBdvHandle().getViewerPanel().state().getViewerTransform();

        result.universe = new Image3DUniverse();
        result.imageContent = addSourceToUniverse(result.universe, imageSource, 300 * 300 * 300,
                Content.VOLUME, 0, 255 );
        result.universe.show();

        return result;
    }

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

        // reset image content orientation
        imageContent.setTransform(new Transform3D());

        // reset 3D viewer view
        universe.resetView();
    }

    public static void resetBdv(Bdv bdvHandle, AffineTransform3D initialTransform) {
        bdvHandle.getBdvHandle().getViewerPanel().state().setViewerTransform(initialTransform);
    }
}