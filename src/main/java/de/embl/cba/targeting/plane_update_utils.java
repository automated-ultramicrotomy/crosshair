package de.embl.cba.targeting;

import customnode.CustomTransparentTriangleMesh;
import customnode.CustomTriangleMesh;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3d;

import java.util.ArrayList;
import java.util.Map;

import static de.embl.cba.targeting.GeometryUtils.*;
import static de.embl.cba.targeting.GeometryUtils.calculate_triangles_from_points;

public class plane_update_utils {

    public static void update_plane_on_transform_change(Image3DUniverse universe, Content imageContent, AffineTransform3D affineTransform3D,
                                                  String plane_name, Map<String, Vector3d> plane_normals,
                                                        Map<String, Vector3d> plane_points, Map<String, Vector3d> plane_centroids) {

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

        update_plane(universe, imageContent, plane_normal, plane_point, plane_name, plane_normals, plane_points, plane_centroids);

    }

    // update planes on transform of the imagecontent, retain teh existing plane normals and points
    public static void update_planes_in_place (Image3DUniverse universe, Content imageContent, Map<String, Vector3d> plane_normals,
                                               Map<String, Vector3d> plane_points, Map<String, Vector3d> plane_centroids) {
        for (String key: plane_normals.keySet()) {
            update_plane(universe, imageContent, plane_normals.get(key), plane_points.get(key), key, plane_normals, plane_points, plane_centroids);
        }
    }

    public static void update_plane (Image3DUniverse universe, Content imageContent, Vector3d plane_normal, Vector3d plane_point,
                              String plane_name, Map<String, Vector3d> plane_normals,
                                     Map<String, Vector3d> plane_points, Map<String, Vector3d> plane_centroids) {

        //TODO - shift to use bounding box of image itself
// Get bounding box of image, account for any transformation of teh image
        Point3d min = new Point3d();
        Point3d max = new Point3d();
        imageContent.getMax(max);
        imageContent.getMin(min);
        System.out.println(max.toString());
        double[] min_coord = new double[3];
        double[] max_coord = new double[3];
        min.get(min_coord);
        max.get(max_coord);
        System.out.println(max.toString());

//		TODO - remvoe image content
        ArrayList<Vector3d> intersection_points = calculate_intersections(min_coord, max_coord, plane_normal, plane_point, imageContent,universe);

        if (intersection_points.size() > 0) {
            plane_normals.put(plane_name, plane_normal);
            plane_points.put(plane_name, plane_point);
            plane_centroids.put(plane_name, get_centroid(intersection_points));

            // intersections were in local space, we want to display in the global so must account for any transformations
            // of the image
            Transform3D translate = new Transform3D();
            Transform3D rotate = new Transform3D();
            imageContent.getLocalTranslate(translate);
            imageContent.getLocalRotate(rotate);

            for (Vector3d point : intersection_points) {
                // convert to point > transfrom affects vectors differently
                Point3d intersect = new Point3d(point.getX(), point.getY(), point.getZ());
                rotate.transform(intersect);
                translate.transform(intersect);
                point.setX(intersect.getX());
                point.setY(intersect.getY());
                point.setZ(intersect.getZ());
            }

            Vector3d transformed_normal = new Vector3d(plane_normal.getX(), plane_normal.getY(), plane_normal.getZ());
            rotate.transform(transformed_normal);

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
                ArrayList<Point3f> triangles = calculate_triangles_from_points(intersection_points, transformed_normal);
                new_mesh = new CustomTransparentTriangleMesh(triangles, plane_color, 0.7f);
            }
            Content meshContent = universe.addCustomMesh(new_mesh, plane_name);
            meshContent.setVisible(true);
            meshContent.setLocked(true);
        }

    }
}
