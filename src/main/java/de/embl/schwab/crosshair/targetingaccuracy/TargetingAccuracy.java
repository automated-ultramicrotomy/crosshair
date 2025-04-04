package de.embl.schwab.crosshair.targetingaccuracy;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.Settings;
import de.embl.schwab.crosshair.settings.SettingsReader;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.solution.Solution;
import de.embl.schwab.crosshair.solution.SolutionReader;
import ij.IJ;
import ij3d.Content;
import ij3d.Image3DUniverse;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.registration.ViewTransform;
import net.imglib2.realtransform.AffineTransform3D;
import org.jogamp.java3d.Transform3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.embl.schwab.crosshair.utils.BdvUtils.addSourceToUniverse;
import static de.embl.schwab.crosshair.utils.Utils.spaceOutWindows;

/**
 * Main entry point for interaction with targeting accuracy workflow
 */
public class TargetingAccuracy {

    private static final Logger logger = LoggerFactory.getLogger(TargetingAccuracy.class);

    public static final String before = "before";
    public static final String after = "after";
    public static final String beforeBlock = "before block";
    public static final String afterBlock = "after block";
    public static final String beforeTarget = "before target";

    /**
     * Open targeting accuracy workflow
     * @param beforeTargetingXml bdv xml file for before targeting image
     * @param registeredAfterTargetingXml bdv xml file for after targeting image (registered to before image)
     * @param crosshairSettingsJson Crosshair settings json file used for this targeting run
     * @param crosshairSolutionJson Crosshair solution json file used for this targeting run
     */
    public TargetingAccuracy( File beforeTargetingXml, File registeredAfterTargetingXml,
                               File crosshairSettingsJson, File crosshairSolutionJson ) throws SpimDataException {

        final LazySpimSource beforeSource = new LazySpimSource("before", beforeTargetingXml.getAbsolutePath());
        final LazySpimSource afterSource = new LazySpimSource("after", registeredAfterTargetingXml.getAbsolutePath());

        String beforeUnit = beforeSource.getVoxelDimensions().unit();
        String afterUnit = afterSource.getVoxelDimensions().unit();

        if (!beforeUnit.equals(afterUnit)) {
            IJ.log("before and after images don't have the same units. Please correct the units and try again.");
            throw new UnsupportedOperationException("before and after images don't use the same units");
        } else {

            BdvStackSource beforeStackSource = BdvFunctions.show(beforeSource, 1);
            beforeStackSource.setDisplayRange(0, 255);
            BdvStackSource afterStackSource = BdvFunctions.show(afterSource, 1, BdvOptions.options().addTo(beforeStackSource));
            afterStackSource.setDisplayRange(0, 255);

            Image3DUniverse universe = new Image3DUniverse();
            universe.show();

            Map<String, Content> imageNameToContent = new HashMap<>();
            Source[] sources = new Source[]{beforeSource, afterSource};
            String[] sourceNames = new String[]{TargetingAccuracy.before, TargetingAccuracy.after};
            for (int i = 0; i < sources.length; i++) {
                Content imageContent = addSourceToUniverse(
                        universe, sources[i], 300 * 300 * 300, Content.VOLUME, 0, 255);
                imageContent.setLocked(true);
                imageContent.showPointList(true);
                universe.getPointListDialog().setVisible(false);

                imageNameToContent.put( sourceNames[i], imageContent );
            }

            accountForTransforms( registeredAfterTargetingXml, imageNameToContent.get( TargetingAccuracy.after ) );

            // we use the before image content to define the extent of the planes. The before x-ray should be the largest,
            // and so give an extent that covers both comfortably
            PlaneManager planeManager = new PlaneManager(
                    beforeStackSource, universe, imageNameToContent.get( TargetingAccuracy.before ), beforeUnit
            );
            new AccuracyBdvBehaviours( beforeStackSource.getBdvHandle(), planeManager );

            Solution solution = new SolutionReader().readSolution( crosshairSolutionJson.getAbsolutePath() );

            TargetingAccuracyFrame accuracyFrame = new TargetingAccuracyFrame( universe, imageNameToContent, planeManager,
                    beforeStackSource.getBdvHandle(), beforeUnit, solution );

            SettingsReader reader = new SettingsReader();
            Settings settings = reader.readSettings( crosshairSettingsJson.getAbsolutePath() );
            // rename the image from crosshair to 'before', so it displays nicely in the panel
            settings.imageNameToSettings.get( Crosshair.image ).name = TargetingAccuracy.before;
            // rename planes to nicer names for display
            settings.planeNameToSettings.get( Crosshair.target ).name = TargetingAccuracy.beforeTarget;
            settings.planeNameToSettings.get( Crosshair.block ).name = TargetingAccuracy.beforeBlock;

            // make before target into a BlockPlane, so it can also have vertices for the point to plane distance measure
            settings.planeNameToSettings.put( Crosshair.target,
                    new BlockPlaneSettings( settings.planeNameToSettings.get( Crosshair.target )) );

            try {
                reader.loadSettings( settings, planeManager,
                        accuracyFrame.getImagesPanel().getImageNameToContent() );
                if ( !accuracyFrame.getOtherPanel().check3DPointsVisible() ) {
                    accuracyFrame.getOtherPanel().toggleVisiblity3DPoints();
                }
            } catch (MicrotomeManager.IncorrectMicrotomeConfiguration e) {
                logger.error(e.getMessage(), e);
            }

            spaceOutWindows(accuracyFrame, beforeStackSource.getBdvHandle(), universe);

        }
    }

    private void accountForTransforms( File registeredAfterTargetingXml, Content imageContent ) throws SpimDataException {
        // The after image is registered to the before, so I also need to shift its 3d to match

        // read any transforms directly from the xml
        SpimDataMinimal spimDataMinimal = new XmlIoSpimDataMinimal().load( registeredAfterTargetingXml.getAbsolutePath() );
        List<ViewTransform> transforms = spimDataMinimal.getViewRegistrations().getViewRegistrationsOrdered().get(0).getTransformList();

        // take every transform into account apart from the last (assuming the last is the base transform i.e. voxel size only)
        AffineTransform3D affine = new AffineTransform3D();
        for ( int i=0; i<transforms.size() - 1; i ++ ) {
            affine.concatenate( transforms.get(i).asAffine3D() );
        }

        double[] flatMatrix = new double[16];
        double[][] matrix = new double[4][4];
        affine.toMatrix( matrix );

        for ( int i=0; i<matrix.length; i++ ) {
            double[] matrixRow = matrix[i];
            for ( int j = 0; j< matrixRow.length; j++ ) {
                flatMatrix[ i*4 + j ] = matrixRow[j];
            }
        }

        Transform3D imageContentTransform = new Transform3D( flatMatrix );
        imageContent.setTransform( imageContentTransform );
    }


}
