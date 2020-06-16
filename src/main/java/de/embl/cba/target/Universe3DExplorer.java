package de.embl.cba.target;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import customnode.CustomMesh;
import customnode.CustomPointMesh;
import customnode.CustomQuadMesh;
import customnode.CustomTriangleMesh;
import ij.ImagePlus;
import ij.plugin.FolderOpener;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.behaviors.InteractiveBehavior;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.java3d.Bounds;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3d;
import org.scijava.vecmath.Vector3f;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.*;

public class Universe3DExplorer
{
//	public static final String INPUT_FOLDER = "Z:\\Kimberly\\Projects\\Targeting\\Data\\Raw\\MicroCT\\Targeting\\Course-1\\flipped";
	public static final String INPUT_FOLDER = "Z:\\Kimberly\\Projects\\Targeting\\Data\\Derived\\test_stack";
	private final Content meshContent;

	public Universe3DExplorer()
	{
		final ImagePlus imagePlus = FolderOpener.open( INPUT_FOLDER, "" );
		imagePlus.show();

		final Image3DUniverse universe = new Image3DUniverse();
		final Content imageContent = universe.addContent( imagePlus, Content.VOLUME );
		universe.addInteractiveBehavior(new CustomBehaviour(universe, imageContent));

		imageContent.setTransparency( 0.7F );
		imageContent.setLocked( true );
		universe.show();

		Bounds image_bounds = imageContent.getBounds();
		System.out.println(image_bounds.toString());

		Point3d global_min = new Point3d();
		Point3d global_max = new Point3d();
		universe.getGlobalMinPoint(global_min);
		universe.getGlobalMaxPoint(global_max);
		double[] global_min_d = {global_min.getX(), global_min.getY(), global_min.getZ()};
		double[] global_max_d = {global_max.getX(), global_max.getY(), global_max.getZ()};

		final ArrayList< Point3f > points = new ArrayList<>();
		points.add( new Point3f( 16, 64, 134 ) );
		points.add( new Point3f( 176, 70, 134 ) );
		points.add( new Point3f( 91, 210, 134 ) );
		final CustomTriangleMesh mesh = new CustomTriangleMesh( points );
		meshContent = universe.addCustomMesh( mesh, "planeA" );
		meshContent.setVisible( true );
		meshContent.setLocked( true );

		final Img wrap = ImageJFunctions.wrap( imagePlus );
		final BdvStackSource bdvStackSource = BdvFunctions.show( wrap, "raw" );
		bdvStackSource.setDisplayRange( 0, 255 );


		final BdvHandle bdvHandle = bdvStackSource.getBdvHandle();
		final Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdvHandle.getTriggerbindings(), "target" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			final AffineTransform3D affineTransform3D = new AffineTransform3D();
			bdvHandle.getViewerPanel().getState().getViewerTransform( affineTransform3D );

			final ArrayList< double[] > viewerPoints = new ArrayList<>();

			viewerPoints.add( new double[]{ 0, 0, 0 });
			viewerPoints.add( new double[]{ 0, 100, 0 });
			viewerPoints.add( new double[]{ 100, 0, 0 });

//			final double[] clickPositionInViewerWindow = { x, y, 0 };

			final ArrayList< double[] > globalPoints = new ArrayList<>();
			for ( int i = 0; i < 3; i++ )
			{
				globalPoints.add( new double[ 3 ] );
			}

			for ( int i = 0; i < 3; i++ )
			{
				affineTransform3D.inverse().apply( viewerPoints.get( i ), globalPoints.get( i ) );
			}

			Vector3d plane_normal = calculate_normal_from_points(globalPoints);
			Vector3d plane_point = new Vector3d(globalPoints.get(0)[0], globalPoints.get(0)[1], globalPoints.get(0)[2]);
			ArrayList<Vector3d> intersection_points = calculate_intersections(global_min_d, global_max_d, plane_normal, plane_point);

			if (intersection_points.size() > 0) {
				System.out.println(intersection_points.size());
				ArrayList<Point3f> vector_points = new ArrayList<>();
				for (Vector3d d : intersection_points) {
					vector_points.add(new Point3f((float) d.getX(), (float) d.getY(), (float) d.getZ()));
				}

				// Alright this works - but it's a bit of a hack. Theoretically intersection of a cuboid and a plane can
				// have more vertices - I think up to six? So we need our meshes to deal with arbitrary numbers of points
				// perhaps add an epsilon in, sometimes it's missing points
				universe.removeContent("planeA");
				if (intersection_points.size() == 3) {
					final CustomTriangleMesh new_mesh = new CustomTriangleMesh(vector_points);
					Content meshContent2 = universe.addCustomMesh(new_mesh, "planeA");
					meshContent2.setVisible(true);
					meshContent2.setLocked(true);
				} else if (intersection_points.size() == 4) {
					final CustomQuadMesh new_mesh = new CustomQuadMesh(vector_points);
					Content meshContent2 = universe.addCustomMesh(new_mesh, "planeA");
					meshContent2.setVisible(true);
					meshContent2.setLocked(true);
				}

			}

		}, "update target plane", "shift T" );

	}

	private Vector3d calculate_normal_from_points (ArrayList<double[]> points) {
		double[] point_a = points.get(0);
		double[] point_b = points.get(1);
		double[] point_c = points.get(2);

		double[] vector_1 = new double[3];
		double[] vector_2 = new double[3];

		for ( int i = 0; i < 3; i++ ) {
			vector_1[i] = point_a[i] - point_b[i];
			vector_2[i] = point_c[i] - point_b[i];
		}

		Vector3d normal = new Vector3d();
		normal.cross(new Vector3d(vector_1), new Vector3d(vector_2));
		normal.normalize();

		return normal;
	}

	private ArrayList<Vector3d> calculate_intersections (double[] global_min, double[] global_max, Vector3d plane_normal, Vector3d plane_point) {
		ArrayList<Vector3d> bounding_box_points = new ArrayList<>();
		bounding_box_points.add(new Vector3d (global_min[0], global_min[1], global_min[2]));
		bounding_box_points.add(new Vector3d (global_min[0], global_min[1], global_max[2]));
		bounding_box_points.add(new Vector3d (global_min[0], global_max[1], global_min[2]));
		bounding_box_points.add(new Vector3d (global_min[0], global_max[1], global_max[2]));
		bounding_box_points.add(new Vector3d (global_max[0], global_min[1], global_min[2]));
		bounding_box_points.add(new Vector3d (global_max[0], global_min[1], global_max[2]));
		bounding_box_points.add(new Vector3d (global_max[0], global_max[1], global_min[2]));
		bounding_box_points.add(new Vector3d (global_max[0], global_max[1], global_max[2]));

		//enumerate all combos of two points on edges
		ArrayList<Vector3d[]> bounding_box_edges = new ArrayList<>();
		bounding_box_edges.add(new Vector3d[] {bounding_box_points.get(0), bounding_box_points.get(1)});
		bounding_box_edges.add(new Vector3d[] {bounding_box_points.get(0), bounding_box_points.get(4)});
		bounding_box_edges.add(new Vector3d[] {bounding_box_points.get(1), bounding_box_points.get(5)});
		bounding_box_edges.add(new Vector3d[] {bounding_box_points.get(4), bounding_box_points.get(5)});
		bounding_box_edges.add(new Vector3d[] {bounding_box_points.get(7), bounding_box_points.get(5)});
		bounding_box_edges.add(new Vector3d[] {bounding_box_points.get(3), bounding_box_points.get(7)});
		bounding_box_edges.add(new Vector3d[] {bounding_box_points.get(7), bounding_box_points.get(6)});
		bounding_box_edges.add(new Vector3d[] {bounding_box_points.get(6), bounding_box_points.get(4)});
		bounding_box_edges.add(new Vector3d[] {bounding_box_points.get(2), bounding_box_points.get(0)});
		bounding_box_edges.add(new Vector3d[] {bounding_box_points.get(2), bounding_box_points.get(6)});
		bounding_box_edges.add(new Vector3d[] {bounding_box_points.get(2), bounding_box_points.get(3)});
		bounding_box_edges.add(new Vector3d[] {bounding_box_points.get(1), bounding_box_points.get(3)});

		ArrayList<Vector3d> intersection_points = new ArrayList<>();

		// check for intersection of plane with all points - if four intersect, return these as teh four points
		// deals with case where plane is on the bounding box edges
		ArrayList<Boolean> intersects = new ArrayList<>();
		for (Vector3d[] v: bounding_box_edges) {
			if (check_vector_lies_in_plane(v[0], v[1], plane_normal, plane_point)) {
				intersection_points.add(v[0]);
				intersection_points.add(v[1]);
				continue;
			// parallel but doesn't lie in plane so no intersections
			} else if (check_vector_plane_parallel(v[0], v[1], plane_normal, plane_point)) {
				continue;
			} else {
				Vector3d intersection = calculate_vector_plane_intersection(v[0], v[1], plane_normal, plane_point);
				if (intersection.length() > 0) {
					intersection_points.add(intersection);
				}
			}
		}

		// get rid of any repeat points
		Set<Vector3d> set = new HashSet<>(intersection_points);
		intersection_points.clear();
		intersection_points.addAll(set);

		return intersection_points;

	}
//	https://stackoverflow.com/questions/5666222/3d-line-plane-intersection
	private Vector3d calculate_vector_plane_intersection (Vector3d point1, Vector3d point2, Vector3d plane_normal, Vector3d plane_point) {
		Vector3d point_to_point = new Vector3d();
		point_to_point.sub(point2, point1);
		double dot_product_vector_plane_normal = point_to_point.dot(plane_normal);

		Vector3d plane_to_point_vector = new Vector3d();
		plane_to_point_vector.sub(point1, plane_point);
		double dot_product_plane_to_point_plane_normal = plane_to_point_vector.dot(plane_normal);
		double factor = -dot_product_plane_to_point_plane_normal / dot_product_vector_plane_normal;

		Vector3d result = new Vector3d();

		if (factor < 0 || factor > 1) {
			return result;
		}

		point_to_point.setX(point_to_point.getX()*factor);
		point_to_point.setY(point_to_point.getY()*factor);
		point_to_point.setZ(point_to_point.getZ()*factor);
		result.add(point1, point_to_point);
		return result;
	}

	private boolean check_vector_lies_in_plane (Vector3d point1, Vector3d point2, Vector3d plane_normal, Vector3d plane_point) {
		// vector between provided points
		boolean vector_plane_parallel = check_vector_plane_parallel (point1, point2, plane_normal, plane_point);
		boolean point_plane_intersect = check_point_plane_intersection(point1, plane_normal, plane_point);

		return vector_plane_parallel && point_plane_intersect;
	}

	private boolean check_vector_plane_parallel (Vector3d point1, Vector3d point2, Vector3d plane_normal, Vector3d plane_point) {
		// vector between provided points
		Vector3d point_to_point = new Vector3d();
		point_to_point.sub(point1, point2);

		double dot_product = point_to_point.dot(plane_normal);
		return dot_product == 0;
	}

	private boolean check_point_plane_intersection (Vector3d point, Vector3d plane_normal, Vector3d plane_point) {
		Vector3d point_to_plane_vector = new Vector3d();
		point_to_plane_vector.sub(plane_point, point);

		double dot_product = point_to_plane_vector.dot(plane_normal);
		return dot_product == 0;
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
