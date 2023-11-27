package de.embl.schwab.crosshair;

import bdv.util.*;
import bdv.viewer.Source;
import de.embl.schwab.crosshair.bdv.BdvBehaviours;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.ui.swing.CrosshairFrame;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import ij.ImagePlus;

import static de.embl.cba.tables.ij3d.UniverseUtils.addSourceToUniverse;
import static de.embl.schwab.crosshair.utils.Utils.spaceOutWindows;

// TODO - neaten up code structure, possibly clearer labelling of which coordinate system & units are being used
// TODO - check degree symbols work on mac

// Possible improvements to add
// TODO - add cutting-plane to target distance in cutting mode (would be nice check for me for distances, and could be useful for folks to plan their runs)
// TODO - make plane update as efficient as possible
// TODO - Add some buttons for e.g. reset view, centre view for microtome, centre view for sample etc
// TODO - Add a check that target plane is behind block face (or intersects it)
// TODO - make GOTOs match normals properly? Issue is imglib2 uses a coordinate system from top left so normal vector t calculates is into page
// not out of it, like our target normals are set?
// TODO - make generic for any bit depth - currently only accepts 8 bit
// TODO - make it so transparency panel doesn't appear if plane not initialised
// TODO - do checks for legitimacy of values when load from settings? e.g. that vertices lie on the block plane
// TODO - make so doesn't show windows until all loaded?
// TODO - check against original blender solution. Make mock file for case of intersection with block face - check how two solutions compare


public class Crosshair {

	public static final String target = "target";
	public static final String block = "block";
	public static final String image = "image";

	// TODO - make generic? Not just 8 bit
	private final int min = 0;
	private final int max = 255;
	private final float transparency = 0.7f;

	/**
	 * Open Crosshair from a Source (normally from a bdv style file e.g. hdf5 / n5)
	 * @param imageSource image source
	 */
	public Crosshair(Source<Object> imageSource) {

		BdvStackSource bdvStackSource = BdvFunctions.show(imageSource, 1);
		Image3DUniverse universe = new Image3DUniverse();
		universe.show();

		String unit = imageSource.getVoxelDimensions().unit();

		// Set to arbitrary colour
		ARGBType colour =  new ARGBType( ARGBType.rgba( 0, 0, 0, 0 ) );
		Content imageContent = addSourceToUniverse(universe, imageSource, 300 * 300 * 300,
				Content.VOLUME, colour, transparency, min, max );
		// Reset colour to default for 3D viewer
		imageContent.setColor(null);

		initialiseCrosshair(bdvStackSource, universe, imageContent, unit);
	}

	/**
	 * Open Crosshair from an ImagePlus (normally by pulling current image from ImageJ)
	 * @param imagePlus image
	 */
	public Crosshair(ImagePlus imagePlus) {

		Image3DUniverse universe = new Image3DUniverse();
		Content imageContent = universe.addContent(imagePlus, Content.VOLUME);
		imageContent.setTransparency(transparency);
		universe.show();

		final double pw = imagePlus.getCalibration().pixelWidth;
		final double ph = imagePlus.getCalibration().pixelHeight;
		final double pd = imagePlus.getCalibration().pixelDepth;
		final String unit = imagePlus.getCalibration().getUnit();

		final Img wrap = ImageJFunctions.wrap(imagePlus);
		BdvStackSource bdvStackSource = BdvFunctions.show(wrap, "raw", Bdv.options()
				.sourceTransform(pw, ph, pd));

		initialiseCrosshair(bdvStackSource, universe, imageContent, unit);
	}

	private void initialiseCrosshair(BdvStackSource bdvStackSource, Image3DUniverse universe, Content imageContent, String unit) {

		bdvStackSource.setDisplayRange(min, max);

		BdvHandle bdvHandle = bdvStackSource.getBdvHandle();
		imageContent.setLocked(true);
		imageContent.showPointList(true);
		universe.getPointListDialog().setVisible(false);

		// the global min of the image is often not (0,0,0), looks like this is calculated only from pixels != 0
		// as here: https://github.com/fiji/3D_Viewer/blob/ed05e4b2275ad6ad7c94b0e22f4789ebd3472f4d/src/main/java/voltex/VoltexGroup.java
		// Still places so (0,0) of image == (0,0) in global coordinate system, just bounding box is wrapped tight to
		// only regions of the image > 0

		PlaneManager planeManager = new PlaneManager(bdvStackSource, universe, imageContent);

		MicrotomeManager microtomeManager = new MicrotomeManager(planeManager, universe, imageContent, bdvStackSource, unit);
		new BdvBehaviours(bdvHandle, planeManager, microtomeManager);

		CrosshairFrame crosshairFrame = new CrosshairFrame(universe, imageContent, planeManager, microtomeManager, bdvHandle, unit);

		spaceOutWindows( bdvHandle, crosshairFrame, universe );
	}
	
}
