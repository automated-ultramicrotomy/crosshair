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
		final BdvStackSource bdvStackSource = BdvFunctions.show(wrap, "raw");
		bdvStackSource.setDisplayRange(0, 255);
		bdvHandle = bdvStackSource.getBdvHandle();

		this.planeManager = new PlaneManager( bdvHandle, universe, imageContent );
		this.microtomeManager = new MicrotomeManager( planeManager, universe, imageContent );

		installBehaviours(points, blockVertices, selectedVertex, bdvStackSource);
	}

	private void installBehaviours(ArrayList<RealPoint> points, ArrayList<RealPoint> blockVertices, RealPoint selectedVertex, BdvStackSource bdvStackSource) {
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
				double[] target_normal = new double[3];
				plane_normals.get("target").get(target_normal);

				double[] target_centroid = new double[3];
				plane_centroids.get("target").get(target_centroid);

				moveToPosition(bdvStackSource, target_centroid, 0);
				levelCurrentView(bdvStackSource, target_normal);
			}
		}, "zoom to targeting plane", "ctrl T" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			if (trackPlane == 0) {
				double[] block_normal = new double[3];
				plane_normals.get("block").get(block_normal);

				double[] block_centroid = new double[3];
				plane_centroids.get("block").get(block_centroid);

				moveToPosition(bdvStackSource, block_centroid, 0);
				levelCurrentView(bdvStackSource, block_normal);
			}
		}, "zoom to block plane", "ctrl F" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
//			Could we just do this with the xy coordinate - here I'm following the bdv viewer workshop example
			RealPoint point = new RealPoint(3);
			bdvHandle.getViewerPanel().getGlobalMouseCoordinates(point);

			final AffineTransform3D transform = new AffineTransform3D();
			bdvHandle.getViewerPanel().getState().getViewerTransform( transform );
			double[] pointViewerCoords = convertToViewerCoordinates(point, transform);

			boolean distanceMatch = false;
			for ( int i = 0; i < points.size(); i++ )
			{
				double[] currentPointViewerCoords = convertToViewerCoordinates(points.get(i), transform);
				double distance = distanceBetweenPoints(pointViewerCoords, currentPointViewerCoords);
				if (distance < 5) {
					double[] chosenPointCoord = new double[3];
					points.get(i).localize(chosenPointCoord);
					int pointIndex = imageContent.getPointList().indexOfPointAt(chosenPointCoord[0], chosenPointCoord[1], chosenPointCoord[2], imageContent.getLandmarkPointSize());
					imageContent.getPointList().remove(pointIndex);

//					There's a bug in how the 3D viewer displays points after one is removed. Currently, it just stops
//					displaying the first point added (rather than the one you actually removed).
//					Therefore here I remove all points and re-add them, to get the viewer to reset how it draws
//					the points. Perhaps there's a more efficient way to get around this?
					PointList currentPointList = imageContent.getPointList().duplicate();
					imageContent.getPointList().clear();
					for (Iterator<BenesNamedPoint> it = currentPointList.iterator(); it.hasNext(); ) {
						BenesNamedPoint p = it.next();
						imageContent.getPointList().add(p);
					}

					points.remove(i);
					bdvHandle.getViewerPanel().requestRepaint();

					distanceMatch = true;
					break;
				}

			}

			if (!distanceMatch) {
				points.add(point);
				bdvHandle.getViewerPanel().requestRepaint();

				//TODO - check properly that these positions match between two viewers
				double[] position = new double[3];
				point.localize(position);
				imageContent.getPointList().add("", position[0], position[1], position[2]);
			}
		}, "add point", "P" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			ArrayList<Vector3d> planeDefinition = fitPlaneToPoints(points);
			planeManager.updatePlane(planeDefinition.get(0), planeDefinition.get(1), "block");
		}, "fit to points", "K" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
//			Could we just do this with the xy coordinate - here I'm following the bdv viewer workshop example
			RealPoint point = new RealPoint(3);
			bdvHandle.getViewerPanel().getGlobalMouseCoordinates(point);
			blockVertices.add(point);
			bdvHandle.getViewerPanel().requestRepaint();

			//TODO - check properly that these positions match between two viewers
			double [] position = new double[3];
			point.localize(position);
			imageContent.getPointList().add("", position[0],position[1],position[2]);

			//TODO - remove
			Point3d min = new Point3d();
			Point3d max = new Point3d();
			imageContent.getMax(max);
			imageContent.getMin(min);
			System.out.println(max.toString());
			System.out.println(min.toString());
			Transform3D translate = new Transform3D();
			Transform3D rotate = new Transform3D();
			imageContent.getLocalTranslate(translate);
			imageContent.getLocalRotate(rotate);
			System.out.println(translate.toString());
			System.out.println(rotate.toString());
			rotate.transform(min);
			rotate.transform(max);
			translate.transform(min);
			translate.transform(max);
			List<Point3f> transformedPoints = new ArrayList<>();
			Point3f poi = new Point3f((float) min.getX(), (float) min.getY(), (float) min.getZ());
			transformedPoints.add(poi);
			Point3f poi2 = new Point3f((float) max.getX(), (float) max.getY(), (float) max.getZ());
			transformedPoints.add(poi2);
			universe.removeContent("yo");
			universe.addPointMesh(transformedPoints, new Color3f(0, 1, 0), "yo");
		}, "add block vertex", "V" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
//			Could we just do this with the xy coordinate - here I'm following the bdv viewer workshop example
			RealPoint point = new RealPoint(3);
			bdvHandle.getViewerPanel().getGlobalMouseCoordinates(point);

			final AffineTransform3D transform = new AffineTransform3D();
			bdvHandle.getViewerPanel().getState().getViewerTransform( transform );
			double[] pointViewerCoords = convertToViewerCoordinates(point, transform);

			for ( int i = 0; i < blockVertices.size(); i++ ) {
				double[] currentPointViewerCoords = convertToViewerCoordinates(blockVertices.get(i), transform);
				double distance = distanceBetweenPoints(pointViewerCoords, currentPointViewerCoords);
				if (distance < 5) {
					selectedVertex.setPosition(blockVertices.get(i));
					double[] test = new double[3];
					selectedVertex.localize(test);
					bdvHandle.getViewerPanel().requestRepaint();
					break;
				}
			}

		}, "select point", "ctrl L" );


		PointsOverlaySizeChange pointOverlay = new PointsOverlaySizeChange();
		pointOverlay.setPoints(points, blockVertices, selectedVertex, named_vertices);
		BdvFunctions.showOverlay(pointOverlay, "point_overlay", Bdv.options().addTo(bdvStackSource));
	}



	private double[] convertToViewerCoordinates(RealPoint point, AffineTransform3D transform) {
		final double[] lPos = new double[ 3 ];
		final double[] gPos = new double[ 3 ];
		// get point position (in microns etc)
		point.localize(lPos);
		// get point position in viewer (I guess in pixel units?), so gpos[2] is the distance in pixels
		// from the current view plane
		transform.apply(lPos, gPos);

		return gPos;
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
