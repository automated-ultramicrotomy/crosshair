package de.embl.cba.targeting;

import bdv.util.*;
import customnode.*;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.popup.BdvPopupMenus;
import ij.ImagePlus;
import ij.plugin.FolderOpener;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.behaviors.InteractiveBehavior;
import net.imglib2.RealLocalizable;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;

import static de.embl.cba.targeting.GeometryUtils.*;
import static de.embl.cba.targeting.plane_update_utils.update_plane;
import static de.embl.cba.targeting.plane_update_utils.update_plane_on_transform_change;
import static java.lang.Math.sqrt;

public class Universe3DExplorer
{
//	public static final String INPUT_FOLDER = "Z:\\Kimberly\\Projects\\Targeting\\Data\\Raw\\MicroCT\\Targeting\\Course-1\\flipped";
//	public static final String INPUT_FOLDER = "Z:\\Kimberly\\Projects\\Targeting\\Data\\Derived\\test_stack";
	public static final String INPUT_FOLDER = "C:\\Users\\meechan\\Documents\\test_3d";
//	public static final String INPUT_FOLDER = "C:\\Users\\meechan\\Documents\\test_stack";
	int track_plane = 0;
	public AffineTransform3D current_target_plane_view = null;
	public AffineTransform3D current_block_plane_view = null;
	Map<String, Vector3d> plane_normals = new HashMap<>();
	Map<String, Vector3d> plane_points = new HashMap<>();
	Map<String, Vector3d> plane_centroids = new HashMap<>();
	Map<String, RealPoint> named_vertices = new HashMap<>();


	public Universe3DExplorer() {
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
		//check points are on block plane
		final ImagePlus imagePlus = FolderOpener.open(INPUT_FOLDER, "");
//		imagePlus.show();

		final ArrayList<RealPoint> points = new ArrayList<>();
		final ArrayList<RealPoint> block_vertices = new ArrayList<>();
		final RealPoint selected_vertex = new RealPoint(3);

		final Image3DUniverse universe = new Image3DUniverse();
		final Content imageContent = universe.addContent(imagePlus, Content.VOLUME);
		universe.addInteractiveBehavior(new CustomBehaviour(universe, imageContent));

		imageContent.setTransparency(0.7F);
		imageContent.setLocked(true);
		imageContent.showPointList(true);
		universe.show();



		Point3d global_min = new Point3d();
		Point3d global_max = new Point3d();
		universe.getGlobalMinPoint(global_min);
		universe.getGlobalMaxPoint(global_max);
		double[] global_min_d = {global_min.getX(), global_min.getY(), global_min.getZ()};
		double[] global_max_d = {global_max.getX(), global_max.getY(), global_max.getZ()};

		final Image3DUniverse microtome_universe = new Image3DUniverse();
		String[] stl_files = {"/arc.stl", "/holder_back.stl", "/holder_front.stl", "/knife.stl"};
		for (String file: stl_files) {
			try {
				URL resource = Universe3DExplorer.class.getResource(file);
				String resource_file = Paths.get(resource.toURI()).toFile().getAbsolutePath();
				Map<String, CustomMesh> mesh_stl = MeshLoader.loadSTL(resource_file);
				for (String key : mesh_stl.keySet()) {
					System.out.println(key);
					// TODO - set as locked - should probably set my other custom meshes to be locked too?
					microtome_universe.addCustomMesh(mesh_stl.get(key), key);
					microtome_universe.getContent(key).setLocked(true);
				}
			} catch (URISyntaxException e) {
			System.out.println("Error reading from resource");
			e.printStackTrace();
			}
		}
		microtome_universe.show();

		final Img wrap = ImageJFunctions.wrap(imagePlus);
		final BdvStackSource bdvStackSource = BdvFunctions.show(wrap, "raw");
		bdvStackSource.setDisplayRange(0, 255);


		final BdvHandle bdvHandle = bdvStackSource.getBdvHandle();
		ui user = new ui(microtome_universe, universe, selected_vertex, named_vertices, bdvHandle, imageContent, plane_normals, plane_points, plane_centroids);
		final Behaviours behaviours = new Behaviours(new InputTriggerConfig());
		behaviours.install(bdvHandle.getTriggerbindings(), "target");

		bdvHandle.getViewerPanel().addTransformListener(new TransformListener<AffineTransform3D>() {
			@Override
			public void transformChanged(AffineTransform3D affineTransform3D) {
				if ( track_plane == 1 )
				{
					update_plane_on_transform_change(universe, imageContent, affineTransform3D, "target", plane_normals, plane_points, plane_centroids);
				} else if (track_plane == 2) {
					update_plane_on_transform_change(universe, imageContent, affineTransform3D, "block", plane_normals, plane_points, plane_centroids);
				}
			}
		});

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			if (track_plane == 0) {
				track_plane = 1;
			} else if (track_plane == 1) {
				track_plane = 0;
			}
		}, "toggle targeting plane update", "shift T" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			if (track_plane == 0) {
				track_plane = 2;
			} else if (track_plane == 2) {
				track_plane = 0;
			}
		}, "toggle block plane update", "shift F" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			if (track_plane == 0) {
				double[] target_normal = new double[3];
				plane_normals.get("target").get(target_normal);

				double[] target_centroid = new double[3];
				plane_centroids.get("target").get(target_centroid);

				moveToPosition(bdvStackSource, target_centroid, 0);
				levelCurrentView(bdvStackSource, target_normal);
			}
		}, "zoom to targeting plane", "ctrl T" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			if (track_plane == 0) {
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
			double[] point_viewer_coords = convert_to_viewer_coordinates(point, transform);

			boolean distance_match = false;
			for ( int i = 0; i < points.size(); i++ )
			{
				double[] current_point_viewer_coords = convert_to_viewer_coordinates(points.get(i), transform);
				double distance = distance_between_points(point_viewer_coords, current_point_viewer_coords);
				if (distance < 5) {
					double[] chosen_point_coord = new double[3];
					points.get(i).localize(chosen_point_coord);
					int point_index = imageContent.getPointList().indexOfPointAt(chosen_point_coord[0], chosen_point_coord[1], chosen_point_coord[2], imageContent.getLandmarkPointSize());
					imageContent.getPointList().remove(point_index);

//					There's a bug in how the 3D viewer displays points after one is removed. Currently, it just stops
//					displaying the first point added (rather than the one you actually removed).
//					Therefore here I remove all points and re-add them, to get the viewer to reset how it draws
//					the points. Perhaps there's a more efficient way to get around this?
					PointList current_point_list = imageContent.getPointList().duplicate();
					imageContent.getPointList().clear();
					for (Iterator<BenesNamedPoint> it = current_point_list.iterator(); it.hasNext(); ) {
						BenesNamedPoint p = it.next();
						imageContent.getPointList().add(p);
					}

					points.remove(i);
					bdvHandle.getViewerPanel().requestRepaint();

					distance_match = true;
					break;
				}

			}

			if (!distance_match) {
				points.add(point);
				bdvHandle.getViewerPanel().requestRepaint();

				//TODO - check properly that these positions match between two viewers
				double[] position = new double[3];
				point.localize(position);
				imageContent.getPointList().add("", position[0], position[1], position[2]);
			}
		}, "add point", "P" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			ArrayList<Vector3d> plane_definition = fit_plane_to_points(points);
			update_plane(universe, imageContent, plane_definition.get(0), plane_definition.get(1), "block", plane_normals, plane_points, plane_centroids);
		}, "fit to points", "K" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
//			Could we just do this with the xy coordinate - here I'm following the bdv viewer workshop example
			RealPoint point = new RealPoint(3);
			bdvHandle.getViewerPanel().getGlobalMouseCoordinates(point);
			block_vertices.add(point);
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
			List<Point3f> transformed_points = new ArrayList<>();
			Point3f poi = new Point3f((float) min.getX(), (float) min.getY(), (float) min.getZ());
			transformed_points.add(poi);
			Point3f poi2 = new Point3f((float) max.getX(), (float) max.getY(), (float) max.getZ());
			transformed_points.add(poi2);
			universe.removeContent("yo");
			universe.addPointMesh(transformed_points, new Color3f(0, 1, 0), "yo");
		}, "add block vertex", "V" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
//			Could we just do this with the xy coordinate - here I'm following the bdv viewer workshop example
			RealPoint point = new RealPoint(3);
			bdvHandle.getViewerPanel().getGlobalMouseCoordinates(point);

			final AffineTransform3D transform = new AffineTransform3D();
			bdvHandle.getViewerPanel().getState().getViewerTransform( transform );
			double[] point_viewer_coords = convert_to_viewer_coordinates(point, transform);

			for ( int i = 0; i < block_vertices.size(); i++ ) {
				double[] current_point_viewer_coords = convert_to_viewer_coordinates(block_vertices.get(i), transform);
				double distance = distance_between_points(point_viewer_coords, current_point_viewer_coords);
				if (distance < 5) {
					selected_vertex.setPosition(block_vertices.get(i));
					double[] test = new double[3];
					selected_vertex.localize(test);
					bdvHandle.getViewerPanel().requestRepaint();
					break;
				}
			}

		}, "select point", "ctrl L" );



		PointsOverlaySizeChange point_overlay = new PointsOverlaySizeChange();
		point_overlay.setPoints(points, block_vertices, selected_vertex, named_vertices);
		BdvFunctions.showOverlay(point_overlay, "point_overlay", Bdv.options().addTo(bdvStackSource));
	}

	private double[] convert_to_viewer_coordinates (RealPoint point, AffineTransform3D transform) {
		final double[] lPos = new double[ 3 ];
		final double[] gPos = new double[ 3 ];
		// get point position (in microns etc)
		point.localize(lPos);
		// get point position in viewer (I guess in pixel units?), so gpos[2] is the distance in pixels
		// from the current view plane
		transform.apply(lPos, gPos);

		return gPos;
	}

	private double distance_between_points (double[] point1, double[] point2) {
		double sum = 0;
		for ( int j = 0; j < 3; j++ )
		{
			double diff =  point1[j] - point2[j];
			sum += diff*diff;
		}
		return sqrt(sum);
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
					save_current_lut(imageContent, "Z:\\Kimberly\\Projects\\Targeting\\Data\\Derived\\test_stack\\test_lut.txt");
					System.out.println("Saved current LUT to file");
					break;
				case KeyEvent.VK_U:
					set_lut_from_file(imageContent, "Z:\\Kimberly\\Projects\\Targeting\\Data\\Derived\\test_stack\\test_lut.txt");
					System.out.println("Set LUT from file");
					break;
			}

		}
	}

	private void save_current_lut(Content imageContent, String filepath) {

		int[] red_lut = new int[256];
		int[] green_lut = new int[256];
		int[] blue_lut = new int[256];
		int[] alpha_lut = new int[256];

		//	get LUT copies the current lut into the given array
		imageContent.getRedLUT(red_lut);
		imageContent.getGreenLUT(green_lut);
		imageContent.getBlueLUT(blue_lut);
		imageContent.getAlphaLUT(alpha_lut);

		try (FileOutputStream f = new FileOutputStream(filepath);
			ObjectOutput s = new ObjectOutputStream(f);) {
			s.writeObject(red_lut);
			s.writeObject(green_lut);
			s.writeObject(blue_lut);
			s.writeObject(alpha_lut);
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error writing to file");
			e.printStackTrace();
		}
	}

	private void set_lut_from_file (Content imageContent, String filepath) {

		try (FileInputStream in = new FileInputStream(filepath);
			 ObjectInputStream s = new ObjectInputStream(in);) {
			int[] red_lut = (int[]) s.readObject();
			int[] green_lut = (int[]) s.readObject();
			int[] blue_lut = (int[]) s.readObject();
			int[] alpha_lut = (int[]) s.readObject();
			imageContent.setLUT(red_lut, green_lut, blue_lut, alpha_lut);

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
		new Universe3DExplorer();
	}
}
