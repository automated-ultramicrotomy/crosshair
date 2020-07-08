package de.embl.cba.targeting;

import bdv.util.*;
import ij.ImagePlus;
import ij.plugin.FolderOpener;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.behaviors.InteractiveBehavior;
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

//TODO - click remvoe of vertex points
//TODO - make sure removal also removes from named_vertices
//TODO - can deselect point
//TODO - make it so assigning top left etc, checks if thw point is assigned to any other label, then removes this
// stops ability to have multilple assignments on each point in bdv
//TODO - add in checks for e.g. right number of points, one of each assignment, target and block normals etc
//TODO - use doubles where possible, convert to float at end to try and avoid inaccuracies
//TODO - does ctrl+f fail if you are already there?
//TODO - more sensible placement of varibles / structure
//TODO - plaen updates a bit redundant, compartmentalise more
//TODO - check min /max of image content, doesn't seem to exactly align to global axes? Is this an issue, it will throw off all my intersections right?
//Add some buttons for e.g. reset view, cnetre view for microtome, centre view for sample etc
//check points are on block plane
//TODO need extra requiremnt that that they the out of block normal points towards knife


public class Crosshair
{

	private int trackPlane = 0;
	private final Image3DUniverse universe;
	private final Content imageContent;
	private final PlaneManager planeManager;
	private final MicrotomeManager microtomeManager;
	private final BdvHandle bdvHandle;
	private final BdvStackSource bdvStackSource;

	public Crosshair (String imageLocation) {

		final ImagePlus imagePlus = FolderOpener.open(imageLocation, "");

		universe = new Image3DUniverse();
		imageContent = universe.addContent(imagePlus, Content.VOLUME);
		universe.addInteractiveBehavior(new CustomBehaviour(universe, imageContent));
		imageContent.setTransparency(0.7F);
		imageContent.setLocked(true);
		imageContent.showPointList(true);
		universe.show();

		final Img wrap = ImageJFunctions.wrap(imagePlus);
		bdvStackSource = BdvFunctions.show(wrap, "raw");
		bdvStackSource.setDisplayRange(0, 255);
		bdvHandle = bdvStackSource.getBdvHandle();

		this.planeManager = new PlaneManager( bdvStackSource, universe, imageContent );
		this.microtomeManager = new MicrotomeManager( planeManager, universe, imageContent );

		installBehaviours();

		JFrame jFrame = new JFrame( "Crosshair");
		jFrame.setPreferredSize(new Dimension( 600, 800 ));
		jFrame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		PlanePanel planePanel = new PlanePanel(planeManager);
		jFrame.add(planePanel);
		jFrame.setVisible( true );
	}

	private void installBehaviours() {
		final Behaviours behaviours = new Behaviours(new InputTriggerConfig());
		behaviours.install(bdvHandle.getTriggerbindings(), "target");

		bdvHandle.getViewerPanel().addTransformListener(new TransformListener<AffineTransform3D>() {
			@Override
			public void transformChanged(AffineTransform3D affineTransform3D) {
				if ( trackPlane == 1 )
				{
					planeManager.updatePlaneOnTransformChange(affineTransform3D, "target");
				} else if (trackPlane == 2) {
					planeManager.updatePlaneOnTransformChange(affineTransform3D, "block");
				}
			}
		});

		behaviours.behaviour( (ClickBehaviour) (x, y ) -> {
			if (trackPlane == 0) {
				trackPlane = 1;
			} else if (trackPlane == 1) {
				trackPlane = 0;
			}
		}, "toggle targeting plane update", "shift T" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			if (trackPlane == 0) {
				trackPlane = 2;
			} else if (trackPlane == 2) {
				trackPlane = 0;
			}
		}, "toggle block plane update", "shift F" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			if (trackPlane == 0) {
				planeManager.moveViewToNamedPlane("target");
			}
		}, "zoom to targeting plane", "ctrl T" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			if (trackPlane == 0) {
				planeManager.moveViewToNamedPlane("block");
			}
		}, "zoom to block plane", "ctrl F" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			planeManager.addRemoveCurrentPositionPoints();
		}, "add point", "P" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			ArrayList<Vector3d> planeDefinition = fitPlaneToPoints(planeManager.getPoints());
			planeManager.updatePlane(planeDefinition.get(0), planeDefinition.get(1), "block");
		}, "fit to points", "K" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			planeManager.addRemoveCurrentPositionBlockVertices();
		}, "add block vertex", "V" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			planeManager.setSelectedVertexCurrentPosition();
		}, "select point", "ctrl L" );


		PointsOverlaySizeChange pointOverlay = new PointsOverlaySizeChange();
		pointOverlay.setPoints(planeManager.getPoints(), planeManager.getBlockVertices(),
				planeManager.getSelectedVertex(), planeManager.getNamedVertices());
		BdvFunctions.showOverlay(pointOverlay, "point_overlay", Bdv.options().addTo(bdvStackSource));
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
		final String INPUT_FOLDER = "C:\\Users\\meechan\\Documents\\test_3d";
		//	public static final String INPUT_FOLDER = "C:\\Users\\meechan\\Documents\\test_stack";
		new Crosshair(INPUT_FOLDER);
	}
}
