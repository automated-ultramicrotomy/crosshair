package de.embl.cba.target;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import customnode.*;
import de.embl.cba.swing.PopupMenu;
import ij.ImagePlus;
import ij.plugin.FolderOpener;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.behaviors.InteractiveBehavior;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;
import org.scijava.java3d.Bounds;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.ScrollBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.vecmath.*;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.*;

public class Universe3DExplorer
{
	public static final String INPUT_FOLDER = "Z:\\Kimberly\\Projects\\Targeting\\Data\\Raw\\MicroCT\\Targeting\\Course-1\\flipped";
//	public static final String INPUT_FOLDER = "Z:\\Kimberly\\Projects\\Targeting\\Data\\Derived\\test_stack";
	int track_plane = 0;

	public Universe3DExplorer() {
		final ImagePlus imagePlus = FolderOpener.open(INPUT_FOLDER, "");
//		imagePlus.show();

		final Image3DUniverse universe = new Image3DUniverse();
		final Content imageContent = universe.addContent(imagePlus, Content.VOLUME);
		universe.addInteractiveBehavior(new CustomBehaviour(universe, imageContent));

		imageContent.setTransparency(0.7F);
		imageContent.setLocked(true);
		universe.show();

		Point3d global_min = new Point3d();
		Point3d global_max = new Point3d();
		universe.getGlobalMinPoint(global_min);
		universe.getGlobalMaxPoint(global_max);
		double[] global_min_d = {global_min.getX(), global_min.getY(), global_min.getZ()};
		double[] global_max_d = {global_max.getX(), global_max.getY(), global_max.getZ()};

		final Img wrap = ImageJFunctions.wrap(imagePlus);
		final BdvStackSource bdvStackSource = BdvFunctions.show(wrap, "raw");
		bdvStackSource.setDisplayRange(0, 255);


		final BdvHandle bdvHandle = bdvStackSource.getBdvHandle();
		final Behaviours behaviours = new Behaviours(new InputTriggerConfig());
		behaviours.install(bdvHandle.getTriggerbindings(), "target");

		bdvHandle.getViewerPanel().addTransformListener(new TransformListener<AffineTransform3D>() {
			@Override
			public void transformChanged(AffineTransform3D affineTransform3D) {
				if ( track_plane == 1 )
				{
					update_plane(universe, affineTransform3D, global_min_d, global_max_d, "target");
				} else if (track_plane == 2) {
					update_plane(universe, affineTransform3D, global_min_d, global_max_d, "block");
				}
			}
		});

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			if (track_plane == 0) {
				track_plane = 1;
			} else if (track_plane == 1) {
				track_plane = 0;
			}
		}, "toggle target plane update", "shift T" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			if (track_plane == 0) {
				track_plane = 2;
			} else if (track_plane == 2) {
				track_plane = 0;
			}
		}, "toggle block plane update", "shift F" );

	}

	private void update_plane (Image3DUniverse universe, AffineTransform3D affineTransform3D, double[] global_min, double[] global_max,
										String plane_name) {

		final ArrayList< double[] > viewerPoints = new ArrayList<>();

		viewerPoints.add( new double[]{ 0, 0, 0 });
		viewerPoints.add( new double[]{ 0, 100, 0 });
		viewerPoints.add( new double[]{ 100, 0, 0 });

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
		ArrayList<Vector3d> intersection_points = calculate_intersections(global_min, global_max, plane_normal, plane_point);

		if (intersection_points.size() > 0) {
			System.out.println(intersection_points.size());
			ArrayList<Point3f> vector_points = new ArrayList<>();
			for (Vector3d d : intersection_points) {
				vector_points.add(new Point3f((float) d.getX(), (float) d.getY(), (float) d.getZ()));
			}

			if (universe.contains(plane_name)) {
				universe.removeContent(plane_name);
			}

			Color3f plane_color = null;
			if (plane_name == "target") {
				plane_color = new Color3f(0, 1, 0);
			} else if (plane_name == "block") {
				plane_color = new Color3f(0, 0, 1);
			}

			CustomTriangleMesh new_mesh = null;
			if (intersection_points.size() == 3) {
				new_mesh = new CustomTransparentTriangleMesh(vector_points, plane_color, 0.7f);
			} else if (intersection_points.size() > 3) {
				ArrayList<Point3f> triangles = calculate_triangles_from_points(intersection_points, plane_normal);
				new_mesh = new CustomTransparentTriangleMesh(triangles, plane_color, 0.7f);
			}
			Content meshContent = universe.addCustomMesh(new_mesh, plane_name);
			meshContent.setVisible(true);
			meshContent.setLocked(true);
		}
	}

	private ArrayList<Point3f> calculate_triangles_from_points (ArrayList<Vector3d> intersections, Vector3d plane_normal) {
		Vector3d centroid = new Vector3d(new double[] {0,0,0});
		for (Vector3d v : intersections) {
			centroid.add(v);
		}
		centroid.setX(centroid.getX()/intersections.size());
		centroid.setY(centroid.getY()/intersections.size());
		centroid.setZ(centroid.getZ()/intersections.size());

		Vector3d centroid_to_point = new Vector3d();
		centroid_to_point.sub(intersections.get(0), centroid);

		Double[] signed_angles = new Double[intersections.size()];
//		angle of point to itself is zero
		signed_angles[0] = 0.0;
		for (int i=1; i<intersections.size(); i++) {
			Vector3d centroid_to_current_point = new Vector3d();
			centroid_to_current_point.sub(intersections.get(i), centroid);
			signed_angles[i] = calculate_signed_angle(centroid_to_point, centroid_to_current_point, plane_normal);
		}

		// convert all intersections to point3f
		ArrayList<Point3f> intersections_3f = new ArrayList<>();
		for (Vector3d d : intersections) {
			intersections_3f.add(vector3d_to_point3f(d));
		}

		// order intersections_without_root with respect ot the signed angles
		ArrayList<point_angle> points_and_angles = new ArrayList<>();
		for (int i = 0; i<intersections_3f.size(); i++) {
			points_and_angles.add(new point_angle(intersections_3f.get(i), signed_angles[i]));
		}

		Collections.sort(points_and_angles, (p1, p2) -> p1.getAngle().compareTo(p2.getAngle()));

		ArrayList<Point3f> triangles = new ArrayList<>();
		for (int i = 1; i<points_and_angles.size() - 1; i++) {
			triangles.add(points_and_angles.get(0).getPoint());
			triangles.add(points_and_angles.get(i).getPoint());
			triangles.add(points_and_angles.get(i + 1).getPoint());
		}

		return triangles;
	}

	private Point3f vector3d_to_point3f (Vector3d vector) {
		Point3f new_point = new Point3f((float) vector.getX(), (float) vector.getY(), (float) vector.getZ());
		return new_point;
	}

	private double calculate_signed_angle (Vector3d vector1, Vector3d vector2, Vector3d plane_normal) {
		double unsigned_angle = vector1.angle(vector2);
		Vector3d cross_vector1_vector2 = new Vector3d();
		cross_vector1_vector2.cross(vector1, vector2);

		double sign = plane_normal.dot(cross_vector1_vector2);
		if (sign < 0) {
			return -unsigned_angle;
		} else {
			return unsigned_angle;
			}
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

	private class point_angle {
		private Point3f point;
		private Double angle;

		public point_angle (Point3f point, Double angle) {
			this.point = point;
			this.angle = angle;
		}

		public Point3f getPoint () {
			return point;
		}

		public Double getAngle () {
			return angle;
		}
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
