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
import ij.ImagePlus;

import java.awt.event.WindowEvent;

import static de.embl.schwab.crosshair.utils.BdvUtils.addSourceToUniverse;
import static de.embl.schwab.crosshair.utils.Utils.spaceOutWindows;

/**
 * Main entry point for interaction with Crosshair
 */
public class Crosshair {

	public static final String target = "target";
	public static final String block = "block";
	public static final String image = "image";

	// TODO - make generic? Not just 8 bit
	private final int min = 0;
	private final int max = 255;
	private final float transparency = 0.7f;

	private BdvHandle bdvHandle; // bdvHandle of the BigDataViewer window
	private Image3DUniverse universe; // universe of the 3D viewer
	private CrosshairFrame crosshairFrame; // crosshair control panel
	private Content imageContent; // image content displayed in 3D viewer
	private PlaneManager planeManager;
	private MicrotomeManager microtomeManager;
	private String unit; // pixel size unit e.g. mm

	/**
	 * Open Crosshair from a Source (normally from a bdv style file e.g. hdf5 / n5)
	 * @param imageSource image source
	 */
	public Crosshair(Source<Object> imageSource) {

		BdvStackSource bdvStackSource = BdvFunctions.show(imageSource, 1);
		Image3DUniverse universe = new Image3DUniverse();
		Content imageContent = addSourceToUniverse(universe, imageSource, 300 * 300 * 300,
				Content.VOLUME, min, max );
		universe.show();

		String unit = imageSource.getVoxelDimensions().unit();
		initialiseCrosshair(bdvStackSource, universe, imageContent, unit);
	}

	/**
	 * Open Crosshair from an ImagePlus (normally by pulling current image from ImageJ)
	 * @param imagePlus image
	 */
	public Crosshair(ImagePlus imagePlus) {

		Image3DUniverse universe = new Image3DUniverse();
		Content imageContent = universe.addContent(imagePlus, Content.VOLUME);
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

		this.bdvHandle = bdvStackSource.getBdvHandle();
		this.imageContent = imageContent;
		this.universe = universe;
		this.unit = unit;

		imageContent.setTransparency(transparency);
		imageContent.setLocked(true);
		imageContent.showPointList(true);
		universe.getPointListDialog().setVisible(false);

		// the global min of the image is often not (0,0,0), looks like this is calculated only from pixels != 0
		// as here: https://github.com/fiji/3D_Viewer/blob/ed05e4b2275ad6ad7c94b0e22f4789ebd3472f4d/src/main/java/voltex/VoltexGroup.java
		// Still places so (0,0) of image == (0,0) in global coordinate system, just bounding box is wrapped tight to
		// only regions of the image > 0

		planeManager = new PlaneManager(bdvStackSource, universe, imageContent);

		microtomeManager = new MicrotomeManager(planeManager, universe, imageContent, bdvStackSource, unit);
		new BdvBehaviours(bdvHandle, planeManager, microtomeManager);

		crosshairFrame = new CrosshairFrame(this);

		spaceOutWindows( crosshairFrame, bdvHandle, universe );
	}

	public BdvHandle getBdvHandle() {
		return bdvHandle;
	}

	public Content getImageContent() {
		return imageContent;
	}

	public Image3DUniverse getUniverse() {
		return universe;
	}

	public MicrotomeManager getMicrotomeManager() {
		return microtomeManager;
	}

	public PlaneManager getPlaneManager() {
		return planeManager;
	}

	public String getUnit() {
		return unit;
	}

	public CrosshairFrame getCrosshairFrame() { return crosshairFrame; }

	public void close() {
		System.out.println("CLOSING bdv");
		bdvHandle.close();
		System.out.println("CLOSING universe");
		universe.close();
		System.out.println("CLEANING universe");
		universe.cleanup();
		System.out.println("Close window");
		crosshairFrame.dispatchEvent(
				new WindowEvent(crosshairFrame, WindowEvent.WINDOW_CLOSING)
		);

		bdvHandle = null;
		universe = null;
		crosshairFrame = null;
		imageContent = null;
		planeManager = null;
		microtomeManager = null;
		unit = null;
	}
}
