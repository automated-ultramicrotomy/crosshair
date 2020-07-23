package de.embl.cba.targeting;

import bdv.BigDataViewer;
import bdv.ij.OpenImagePlusPlugIn;
import bdv.util.*;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.FolderOpener;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.behaviors.InteractiveBehavior;
import io.scif.img.ImgOpener;
import net.imglib2.RealPoint;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;
import org.scijava.java3d.Transform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.vecmath.*;
import vib.BenesNamedPoint;
import vib.PointList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;

import static de.embl.cba.targeting.GeometryUtils.*;
import static de.embl.cba.targeting.utils.printImageMinMax;

//TODO - more sensible placement of varibles / structure
//TODO - make plane update as efficient as possible
//Add some buttons for e.g. reset view, cnetre view for microtome, centre view for sample etc
//check points are on block plane
//TODO need extra requiremnt that that they the out of block normal points towards knife
//TODO - no plane updates when they aren't visible
//TODO - maybe explicitly round in microtome manager to 4dp (otherwise a longer number typed is transmitted, but isn't
// displayed - could be confusing
//TODO - add save / load of planes and points
//TODO stop view shiting when you move the microtome
//TODO - option to change transparency of image volume render?
//TODO - can make volume render visible or invisible
//TODO - proper checks for entry exit of cutting mode e.g. already have a s
//TODO - initial point - not general to case where target plane intersects with block face e.g. you're just chipping off a
// corner - some vertex points above, some below. Need to think about approaching from a distance.
// TODO - check angle updates - wrote very quickly

// TODO - orient cutting simulation so edge vector at bottom
// TODO -more sensible placement of centre - perhaps leave it so someone can place centre where they want and then cut through?
// would be useful if want to look at particular seciton in detail.
// TODO - there's an issue with the solutions!! (think it's fixed now)
//TODO - colour change on alignment, only set, if not already that colour?
// TODO - prettify interface
// TODO - align microtome view when enter
// TODO - perhaps add another plane entry for cutting plane so can change colour / visiblity etc

public class Crosshair
{
	private final Image3DUniverse universe;
	private final Content imageContent;
	private final PlaneManager planeManager;
	private final MicrotomeManager microtomeManager;
	private final PlanePanel planePanel;
	private final BdvHandle bdvHandle;
	private final BdvStackSource bdvStackSource;

	public Crosshair (ImagePlus imagePlus) {

		universe = new Image3DUniverse();
		imageContent = universe.addContent(imagePlus, Content.VOLUME);
		universe.addInteractiveBehavior(new CustomBehaviour(universe, imageContent));
		imageContent.setTransparency(0.7F);
		imageContent.setLocked(true);
		imageContent.showPointList(true);
		universe.show();

		// the global min of the image is often not (0,0,0), looks like this is calculated only from pixels != 0
		// as here: https://github.com/fiji/3D_Viewer/blob/ed05e4b2275ad6ad7c94b0e22f4789ebd3472f4d/src/main/java/voltex/VoltexGroup.java
		// Still places so (0,0) of image == (0,0) in global coordinate system, just bounding box is wrapped tight to
		// only regions of the image > 0

		// want this to take scaling of image into account like the open current image command does for bdv - see load method
		// https://github.com/bigdataviewer/bigdataviewer_fiji/blob/62926d53664c156d7bda925fd74c7f1d7f7a603c/src/main/java/bdv/ij/OpenImagePlusPlugIn.java
		// set scaling as here: https://github.com/bigdataviewer/bigdataviewer-workshop/blob/master/src/main/java/bdv/workshop/E03MultipleSources.java
		// source transform method here: https://github.com/bigdataviewer/bigdataviewer-vistools/blob/df9405e4bf3fe156d6ab60152b9a62f0e21e63bf/src/main/java/bdv/util/BdvOptions.java
		final double pw = imagePlus.getCalibration().pixelWidth;
		final double ph = imagePlus.getCalibration().pixelHeight;
		final double pd = imagePlus.getCalibration().pixelDepth;
		System.out.println(pw);
		System.out.println(ph);
		System.out.println(pd);
		final Img wrap = ImageJFunctions.wrap(imagePlus);
		bdvStackSource = BdvFunctions.show(wrap, "raw", Bdv.options()
		.sourceTransform(pw, ph, pd));
		// TODO - make generic? Not just 8 bit - see open current image bdv command
		bdvStackSource.setDisplayRange(0, 255);
		bdvHandle = bdvStackSource.getBdvHandle();

		this.planeManager = new PlaneManager( bdvStackSource, universe, imageContent );
		this.microtomeManager = new MicrotomeManager( planeManager, universe, imageContent, bdvStackSource );

		installBehaviours();
		PointsOverlaySizeChange pointOverlay = new PointsOverlaySizeChange();
		pointOverlay.setPoints(planeManager.getPoints(), planeManager.getBlockVertices(),
				planeManager.getSelectedVertex(), planeManager.getNamedVertices());
		BdvFunctions.showOverlay(pointOverlay, "PointOverlay", Bdv.options().addTo(bdvStackSource));

		JFrame jFrame = new JFrame( "Crosshair");
		jFrame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		// main panel
		JPanel mainPane = new JPanel();
		mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));
		mainPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		mainPane.setOpaque(true);
		jFrame.setContentPane(mainPane);

		planePanel = new PlanePanel(planeManager);
		PointsPanel pointsPanel = new PointsPanel(imageContent, pointOverlay, bdvHandle);
		ImagesPanel imagesPanel = new ImagesPanel(imageContent, pointsPanel);
		VertexAssignmentPanel vertexAssignmentPanel = new VertexAssignmentPanel(planeManager);
		MicrotomePanel microtomePanel = new MicrotomePanel(microtomeManager);
		microtomePanel.setParentFrame(jFrame);
		microtomeManager.setMicrotomePanel(microtomePanel);
		microtomeManager.setVertexAssignmentPanel(vertexAssignmentPanel);
		mainPane.add(imagesPanel);
		mainPane.add(planePanel);
		mainPane.add(pointsPanel);
		mainPane.add(vertexAssignmentPanel);
		mainPane.add(microtomePanel);
		//TODO - add a point panel - change visiblity of both sets of points, and their colour in bdv
//		jFrame.add(new JSeparator());
//		jFrame.add(new JSeparator());
		jFrame.pack();
		jFrame.setVisible( true );

		//TODO - remove
		printImageMinMax(imageContent);
	}

	// GUI - try like https://stackoverflow.com/questions/16067894/how-to-arrange-multiple-panels-in-jframe
	private void installBehaviours() {
		final Behaviours behaviours = new Behaviours(new InputTriggerConfig());
		behaviours.install(bdvHandle.getTriggerbindings(), "target");

		bdvHandle.getViewerPanel().addTransformListener(new TransformListener<AffineTransform3D>() {
			@Override
			public void transformChanged(AffineTransform3D affineTransform3D) {
				if ( planeManager.getTrackPlane() == 1 )
				{
					planeManager.updatePlaneOnTransformChange(affineTransform3D, "target");
				} else if (planeManager.getTrackPlane() == 2) {
					planeManager.updatePlaneOnTransformChange(affineTransform3D, "block");
				}
			}
		});

		behaviours.behaviour( (ClickBehaviour) (x, y ) -> {
			if (planeManager.getTrackPlane() == 0 & planeManager.getVisiblityNamedPlane("target") & !microtomeManager.checkMicrotomeMode()) {
				planeManager.setTrackPlane(1);
				// TODO - update plane here
			} else if (planeManager.getTrackPlane() == 1) {
				planeManager.setTrackPlane(0);
			} else {
				System.out.println("Microtome mode must be inactive, and plane visible, to track");
			}
		}, "toggle targeting plane update", "shift T" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			if (planeManager.getTrackPlane() == 0 & planeManager.getVisiblityNamedPlane("block") & !microtomeManager.checkMicrotomeMode()) {
				// check if there are already vertex points
				if (planeManager.getBlockVertices().size() > 0) {
					int result = JOptionPane.showConfirmDialog(null,"If you track the block plane, you will lose all current vertex points", "Are you sure?",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE);
					if(result == JOptionPane.YES_OPTION){
						planeManager.removeAllBlockVertices();
						planeManager.setTrackPlane(2);
						//TODO - update plane here
					}
				} else {
					planeManager.setTrackPlane(2);
					//TODO - update plane here
				}
			} else if (planeManager.getTrackPlane() == 2) {
				planeManager.setTrackPlane(0);
			} else {
				System.out.println("Microtome mode must be inactive, and plane visible, to track");
			}
		}, "toggle block plane update", "shift F" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			if (!microtomeManager.checkMicrotomeMode() & planeManager.getTrackPlane() == 0) {
				planeManager.addRemoveCurrentPositionPoints();
			} else {
				System.out.println("Microtome mode must be inactive, and not tracking plane, to change points");
			}
		}, "add point", "P" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			if (planeManager.getTrackPlane() == 0 & !microtomeManager.checkMicrotomeMode()) {
				if (planeManager.getBlockVertices().size() > 0) {
					int result = JOptionPane.showConfirmDialog(null, "If you fit the block plane to points, you will lose all current vertex points", "Are you sure?",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE);
					if (result == JOptionPane.YES_OPTION) {
						planeManager.removeAllBlockVertices();
						ArrayList<Vector3d> planeDefinition = fitPlaneToPoints(planeManager.getPoints());
						planeManager.updatePlane(planeDefinition.get(0), planeDefinition.get(1), "block");
					}
				} else {
						ArrayList<Vector3d> planeDefinition = fitPlaneToPoints(planeManager.getPoints());
						planeManager.updatePlane(planeDefinition.get(0), planeDefinition.get(1), "block");
					}
			} else {
				System.out.println("Can only fit to points, when not tracking a plane and microtome mode is inactive");
			}
		}, "fit to points", "K" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			if (!microtomeManager.checkMicrotomeMode() & planeManager.getTrackPlane() == 0 & planeManager.checkNamedPlaneExists("block")) {
				planeManager.addRemoveCurrentPositionBlockVertices();
			} else {
				System.out.println("Microtome mode must be inactive, block plane must exit, and not tracking plane, to change points");
			}
		}, "add block vertex", "V" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			planeManager.toggleSelectedVertexCurrentPosition();
		}, "select point", "button1" );

	}

//	behaviours for 3d window
	private class CustomBehaviour extends InteractiveBehavior {

		private Content imageContent;

		CustomBehaviour(Image3DUniverse universe, Content imageContent) {
			super(universe);
			this.imageContent = imageContent;
		}

		public void doProcess(KeyEvent e) {
			int id = e.getID();
			int key = e.getKeyCode();

			if (!((id == KeyEvent.KEY_RELEASED && e.isControlDown() && key == KeyEvent.VK_S) ||
					(id == KeyEvent.KEY_RELEASED && e.isControlDown() && key == KeyEvent.VK_U))) {
				super.doProcess(e);
				return;
			}

			switch (key) {
				case KeyEvent.VK_S:
					saveCurrentLut(imageContent, "Z:\\Kimberly\\Projects\\Targeting\\Data\\Derived\\test_stack\\test_lut.txt");
					System.out.println("Saved current LUT to file");
					break;
				case KeyEvent.VK_U:
					setLutFromFile(imageContent, "Z:\\Kimberly\\Projects\\Targeting\\Data\\Derived\\test_stack\\test_lut.txt");
					System.out.println("Set LUT from file");
					break;
			}

		}
	}

	private void saveCurrentLut(Content imageContent, String filepath) {

		int[] redLut = new int[256];
		int[] greenLut = new int[256];
		int[] blueLut = new int[256];
		int[] alphaLut = new int[256];

		//	get LUT copies the current lut into the given array
		imageContent.getRedLUT(redLut);
		imageContent.getGreenLUT(greenLut);
		imageContent.getBlueLUT(blueLut);
		imageContent.getAlphaLUT(alphaLut);

		try (FileOutputStream f = new FileOutputStream(filepath);
			ObjectOutput s = new ObjectOutputStream(f);) {
			s.writeObject(redLut);
			s.writeObject(greenLut);
			s.writeObject(blueLut);
			s.writeObject(alphaLut);
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error writing to file");
			e.printStackTrace();
		}
	}

	private void setLutFromFile(Content imageContent, String filepath) {

		try (FileInputStream in = new FileInputStream(filepath);
			 ObjectInputStream s = new ObjectInputStream(in);) {
			int[] redLut = (int[]) s.readObject();
			int[] greenLut = (int[]) s.readObject();
			int[] blueLut = (int[]) s.readObject();
			int[] alphaLut = (int[]) s.readObject();
			imageContent.setLUT(redLut, greenLut, blueLut, alphaLut);

		} catch (FileNotFoundException e) {
			System.out.println("File not found");
			e.printStackTrace();
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Error reading from file");
			e.printStackTrace();
		}
	}

	public static void main( String[] args )
	{
		//	public static final String INPUT_FOLDER = "Z:\\Kimberly\\Projects\\Targeting\\Data\\Raw\\MicroCT\\Targeting\\Course-1\\flipped";
		//	public static final String INPUT_FOLDER = "Z:\\Kimberly\\Projects\\Targeting\\Data\\Derived\\test_stack";
//		final String INPUT_FOLDER = "C:\\Users\\meechan\\Documents\\test_3d";
//		final String INPUT_FOLDER = "C:\\Users\\meechan\\Documents\\test_3d_larger_isotropic";
		final String INPUT_IMAGE = "C:\\Users\\meechan\\Documents\\test_3d_larger_anisotropic\\test_3d_larger_anisotropic.tif";
//		final String INPUT_IMAGE = "C:\\Users\\meechan\\Documents\\test_3d_sparse_image\\yu.tif";
		//	public static final String INPUT_FOLDER = "C:\\Users\\meechan\\Documents\\test_stack";
//		final ImagePlus imagePlus = FolderOpener.open(INPUT_FOLDER, "");

		ImagePlus imagePlus = IJ.openImage(INPUT_IMAGE);
		imagePlus.show();

		final ImagePlus currImage = WindowManager.getCurrentImage();
		new Crosshair(currImage);
	}
}
