package de.embl.cba.targeting;

import net.imglib2.RealPoint;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class GeometryUtils {
    public static void fit_plane_to_points (ArrayList<RealPoint> points) {
        // Fit plane - get normal and point on plane, then usual update plane jazz

    }

    public static ArrayList<Point3f> calculate_triangles_from_points (ArrayList<Vector3d> intersections, Vector3d plane_normal) {
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

    public static Point3f vector3d_to_point3f (Vector3d vector) {
        Point3f new_point = new Point3f((float) vector.getX(), (float) vector.getY(), (float) vector.getZ());
        return new_point;
    }

    public static double calculate_signed_angle (Vector3d vector1, Vector3d vector2, Vector3d plane_normal) {
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

    public static Vector3d calculate_normal_from_points (ArrayList<double[]> points) {
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

    public static ArrayList<Vector3d> calculate_intersections (double[] global_min, double[] global_max, Vector3d plane_normal, Vector3d plane_point) {
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
            } else if (check_vector_plane_parallel(v[0], v[1], plane_normal)) {
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
    public static Vector3d calculate_vector_plane_intersection (Vector3d point1, Vector3d point2, Vector3d plane_normal, Vector3d plane_point) {
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

    public static boolean check_vector_lies_in_plane (Vector3d point1, Vector3d point2, Vector3d plane_normal, Vector3d plane_point) {
        // vector between provided points
        boolean vector_plane_parallel = check_vector_plane_parallel (point1, point2, plane_normal);
        boolean point_plane_intersect = check_point_plane_intersection(point1, plane_normal, plane_point);

        return vector_plane_parallel && point_plane_intersect;
    }

    public static boolean check_vector_plane_parallel (Vector3d point1, Vector3d point2, Vector3d plane_normal) {
        // vector between provided points
        Vector3d point_to_point = new Vector3d();
        point_to_point.sub(point1, point2);

        double dot_product = point_to_point.dot(plane_normal);
        return dot_product == 0;
    }

    public static boolean check_point_plane_intersection (Vector3d point, Vector3d plane_normal, Vector3d plane_point) {
        Vector3d point_to_plane_vector = new Vector3d();
        point_to_plane_vector.sub(plane_point, point);

        double dot_product = point_to_plane_vector.dot(plane_normal);
        return dot_product == 0;
    }

}
