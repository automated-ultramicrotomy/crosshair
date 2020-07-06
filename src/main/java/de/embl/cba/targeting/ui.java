package de.embl.cba.targeting;

import bdv.util.BdvHandle;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.RealPoint;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class ui extends JPanel {
    private BdvHandle bdvHandle;
    Image3DUniverse microtome_universe;
    Image3DUniverse universe;
    Content imageContent;
    private double knife_tilt;
    private double tilt;
    private double rotation;
    Vector3f rotation_axis;
    Vector3f tilt_axis;
    Vector3f arc_centre;
    RealPoint selected_point;
    Map<String, RealPoint> pointmap;
    Map<String, Vector3d> plane_normals;
    Map<String, Vector3d> plane_points;
    Map<String, Vector3d> plane_centroids;

    //TODO - add all sliders up here?
    private JSlider initial_knife_angle;


    public ui (Image3DUniverse microtome_universe, Image3DUniverse universe, RealPoint selected_point, Map<String, RealPoint> pointmap,
               BdvHandle bdvHandle, Content imageContent, Map<String, Vector3d> plane_normals,
               Map<String, Vector3d> plane_points, Map<String, Vector3d> plane_centroids) {
        this.microtome_universe = microtome_universe;
        this.selected_point = selected_point;
        this.pointmap = pointmap;
        this.bdvHandle = bdvHandle;
        this.imageContent = imageContent;
        this.universe = universe;
        this.plane_normals = plane_normals;
        this.plane_points = plane_points;
        this.plane_centroids = plane_centroids;
        rotation_axis = new Vector3f(new float[] {0, 1, 0});
        tilt_axis = new Vector3f(new float[] {1, 0, 0});
        arc_centre = new Vector3f(new float[] {0,1,0});

        JFrame microtome_panel = new JFrame("frame");
        JPanel p = new JPanel();

        ActionListener vertex_listener = new vertex_point_listener();
        JButton top_left = new JButton("Top Left");
        top_left.setActionCommand("top_left");
        top_left.addActionListener(vertex_listener);
        p.add(top_left);
        JButton top_right = new JButton("Top Right");
        top_right.setActionCommand("top_right");
        top_right.addActionListener(vertex_listener);
        p.add(top_right);
        JButton bottom_left = new JButton("Bottom Left");
        bottom_left.setActionCommand("bottom_left");
        bottom_left.addActionListener(vertex_listener);
        p.add(bottom_left);
        JButton bottom_right = new JButton("Bottom Right");
        bottom_right.setActionCommand("bottom_right");
        bottom_right.addActionListener(vertex_listener);
        p.add(bottom_right);

        ActionListener block_listener = new block_listener();
        JButton initialise_block = new JButton("Initialise block");
        initialise_block.setActionCommand("initialise_block");
        initialise_block.addActionListener(block_listener);
        p.add(initialise_block);

        // Initial knife angle
        initial_knife_angle = new JSlider(JSlider.HORIZONTAL, -30, 30, 0);
        initial_knife_angle.setMajorTickSpacing(10);
        initial_knife_angle.setMinorTickSpacing(1);
        initial_knife_angle.setPaintTicks(true);
        initial_knife_angle.setPaintLabels(true);
        p.add(initial_knife_angle);

//        Orientation of axes matches those in original blender file, object positions also match
//        Interactive transform setter in 3d viewer: https://github.com/fiji/3D_Viewer/blob/master/src/main/java/ij3d/gui/InteractiveTransformDialog.java
        JSlider knife_rotation = new JSlider(JSlider.HORIZONTAL, -30, 30, 0);
        knife_rotation.addChangeListener(new KnifeListener());
        knife_rotation.setMajorTickSpacing(10);
        knife_rotation.setMinorTickSpacing(1);
        knife_rotation.setPaintTicks(true);
        knife_rotation.setPaintLabels(true);
        p.add(knife_rotation);

        JSlider arc_rotation = new JSlider(JSlider.HORIZONTAL, -20, 20, 0);
        arc_rotation.addChangeListener(new HolderBackListener());
        arc_rotation.setMajorTickSpacing(10);
        arc_rotation.setMinorTickSpacing(1);
        arc_rotation.setPaintTicks(true);
        arc_rotation.setPaintLabels(true);
        p.add(arc_rotation);

        JSlider holder_rotation = new JSlider(JSlider.HORIZONTAL, -180, 180, 0);
        holder_rotation.addChangeListener(new HolderFrontListener());
        holder_rotation.setMajorTickSpacing(60);
        holder_rotation.setMinorTickSpacing(1);
        holder_rotation.setPaintTicks(true);
        holder_rotation.setPaintLabels(true);
        p.add(holder_rotation);

        microtome_panel.add(p);
        microtome_panel.show();

    }



    class vertex_point_listener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
//        does this work? It's already initialised as an empty array of certain size?
            if (selected_point == null) {
                return;
            }
            RealPoint selected_point_copy = new RealPoint(selected_point);
            if ("top_left".equals(e.getActionCommand())) {
                rename_point_3D(imageContent, selected_point, "TL");
                pointmap.put("top_left", selected_point_copy);
                bdvHandle.getViewerPanel().requestRepaint();
            } else if ("top_right".equals(e.getActionCommand())) {
                rename_point_3D(imageContent, selected_point, "TR");
                pointmap.put("top_right", selected_point_copy);
                bdvHandle.getViewerPanel().requestRepaint();
            } else if ("bottom_left".equals(e.getActionCommand())) {
                rename_point_3D(imageContent, selected_point, "BL");
                pointmap.put("bottom_left", selected_point_copy);
                bdvHandle.getViewerPanel().requestRepaint();
            } else if ("bottom_right".equals(e.getActionCommand())) {
                rename_point_3D(imageContent, selected_point, "BR");
                pointmap.put("bottom_right", selected_point_copy);
                bdvHandle.getViewerPanel().requestRepaint();
            }
        }
    }

    class block_listener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
                setup_block_orientation(imageContent, universe, pointmap, initial_knife_angle.getValue());
                // TODO - update planes
            plane_update_utils.update_planes_in_place(universe, imageContent, plane_normals, plane_points, plane_centroids);

            }
    }

    private void setup_block_orientation (Content imageContent, Image3DUniverse universe,
                                          Map<String, RealPoint> named_vertices, double initial_knife_angle) {
        //reset translation / rotation in case it has been modified
        imageContent.setTransform(new Transform3D());

        // check normal in right orientation, coming out of block surface
        double[] top_left = new double[3];
        double[] bottom_left = new double[3];
        double[] bottom_right = new double[3];
        named_vertices.get("top_left").localize(top_left);
        named_vertices.get("bottom_left").localize(bottom_left);
        named_vertices.get("bottom_right").localize(bottom_right);

        Vector3d bottom_edge_vector = new Vector3d();
        bottom_edge_vector.sub(new Vector3d(bottom_right), new Vector3d(bottom_left));

        double length_edge = bottom_edge_vector.length();

        Vector3d up_left_side_vector = new Vector3d();
        up_left_side_vector.sub(new Vector3d(top_left), new Vector3d(bottom_left));

        // bottom edge cross up left side, gives a normal that points out of teh block surface
        Vector3d block_normal = new Vector3d();
        block_normal.cross(bottom_edge_vector, up_left_side_vector);
        block_normal.normalize();

        Vector3d end_block_normal = new Vector3d(0, -1, 0);
        Vector3d end_edge_vector = new Vector3d(1, 0, 0);
        //TODO - currently hard code knife angle, make user parameter
        AxisAngle4d initial_knife_offset = new AxisAngle4d(new Vector3d(0, 0, 1), initial_knife_angle * Math.PI / 180);
        Matrix4d matrix_initial_knife_offset = new Matrix4d();
        matrix_initial_knife_offset.set(initial_knife_offset);
        Transform3D initial_knife_transform = new Transform3D(matrix_initial_knife_offset);

        initial_knife_transform.transform(end_block_normal);
        end_block_normal.normalize();
        initial_knife_transform.transform(end_edge_vector);
        end_edge_vector.normalize();

        // normalise just in case
        bottom_edge_vector.normalize();

        // what is transform to bring block normal to be end block normal & edge vector to be end edge vector?
        //TODO - maybe translate so centre of edge vector == centre of knife location
        Rotation end_rotation = new Rotation(new Vector3D(block_normal.getX(), block_normal.getY(), block_normal.getZ()),
                new Vector3D(bottom_edge_vector.getX(), bottom_edge_vector.getY(), bottom_edge_vector.getZ()),
                new Vector3D(end_block_normal.getX(), end_block_normal.getY(), end_block_normal.getZ()),
                new Vector3D(end_edge_vector.getX(), end_edge_vector.getY(), end_edge_vector.getZ()));


        // convert back to scijava conventions
        double[][] end_rot_matrix = end_rotation.getMatrix();
        Matrix4d scijava_form_matrix = new Matrix4d(end_rot_matrix[0][0], end_rot_matrix[0][1], end_rot_matrix[0][2], 0,
                end_rot_matrix[1][0],end_rot_matrix[1][1],end_rot_matrix[1][2], 0,
                end_rot_matrix[2][0],end_rot_matrix[2][1],end_rot_matrix[2][2], 0,
                0,0,0,1);

        // initial position of bottom edge centre
        Vector3d bottom_edge_centre = new Vector3d(bottom_left);
        bottom_edge_centre.add(new Vector3d(bottom_edge_vector.getX() * 0.5 * length_edge,
                bottom_edge_vector.getY() * 0.5 * length_edge,
                bottom_edge_vector.getZ() * 0.5 * length_edge));
        // vector from initial to final position of bottom edge centre
        Vector3d end_bottom_edge_centre = new Vector3d(0, 0, 0);
        end_bottom_edge_centre.sub(bottom_edge_centre);

        //final transform
        Matrix4d final_setup_transform = new Matrix4d();
        // rotate about the initial position of bottom edge centre, then translate bottom edge centre to (0,0,0)
        compose(scijava_form_matrix, new Vector3d(bottom_edge_centre.getX(), bottom_edge_centre.getY(), bottom_edge_centre.getZ()), new Vector3d(end_bottom_edge_centre.getX(), end_bottom_edge_centre.getY(), end_bottom_edge_centre.getZ()), final_setup_transform);
        imageContent.setTransform(new Transform3D(final_setup_transform));
        // change so view rotates about (0,0,0)
        universe.centerAt(new Point3d());
    }

        //as here for recalculate global min max
        // https://github.com/fiji/3D_Viewer/blob/c1cba02d475a05c94aebe322c2d5d76790907d6b/src/main/java/ij3d/Image3DUniverse.java
    private double[] calculate_centre (Content imageContent) {
        final Point3d cmin = new Point3d();
        imageContent.getMin(cmin);
        final Point3d cmax = new Point3d();
        imageContent.getMax(cmax);

        double[] centre = new double[3];

        centre[0] = cmin.getX() + (cmax.getX() - cmin.getX()) / 2;
        centre[1] = cmin.getY() + (cmax.getY() - cmin.getY()) / 2;
        centre[2] = cmin.getZ() + (cmax.getZ() - cmin.getZ()) / 2;

        return centre;
    }

    private void rename_point_3D (Content content, RealPoint point, String name) {
        double[] point_coord = new double[3];
        point.localize(point_coord);
        int point_index = content.getPointList().indexOfPointAt(point_coord[0], point_coord[1], point_coord[2], content.getLandmarkPointSize());
        content.getPointList().rename(content.getPointList().get(point_index), name);
    }

    //        The two methods below are adapted from the imagej 3d viewer
    //        Interactive transform setter in 3d viewer: https://github.com/fiji/3D_Viewer/blob/master/src/main/java/ij3d/gui/InteractiveTransformDialog.java
    //        setting of transform: https://github.com/fiji/3D_Viewer/blob/ed05e4b2275ad6ad7c94b0e22f4789ebd3472f4d/src/main/java/ij3d/Executer.java
    private Matrix4f make_matrix (double angle_degrees, Vector3f axis, Vector3f rotation_centre, Vector3f translation) {
        float angle_rad = (float) (angle_degrees * Math.PI / 180);
        Matrix4f m = new Matrix4f();
        compose(new AxisAngle4f(axis, angle_rad), rotation_centre, translation, m);
        return m;
    }

    private Matrix4d make_matrix (double angle_degrees, Vector3d axis, Vector3d rotation_centre, Vector3d translation) {
        double angle_rad = angle_degrees * Math.PI / 180;
        Matrix4d m = new Matrix4d();
        compose(new AxisAngle4d(axis, angle_rad), rotation_centre, translation, m);
        return m;
    }


    public static void compose(final AxisAngle4f rot, final Vector3f origin,
                               final Vector3f translation, final Matrix4f ret)
    {
        ret.set(rot);
        final Vector3f trans = new Vector3f(origin);
        trans.scale(-1);
        ret.transform(trans);
        trans.add(translation);
        trans.add(origin);

        ret.setTranslation(trans);
    }

    public static void compose(final AxisAngle4d rot, final Vector3d origin,
                               final Vector3d translation, final Matrix4d ret)
    {
        ret.set(rot);
        final Vector3d trans = new Vector3d(origin);
        trans.scale(-1);
        ret.transform(trans);
        trans.add(translation);
        trans.add(origin);

        ret.setTranslation(trans);
    }

    public static void compose(final Matrix4d rot, final Vector3d origin,
                               final Vector3d translation, final Matrix4d ret)
    {
        ret.set(rot);
        final Vector3d trans = new Vector3d(origin);
        trans.scale(-1);
        ret.transform(trans);
        trans.add(translation);
        trans.add(origin);

        ret.setTranslation(trans);
    }

    class KnifeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();
            knife_tilt = (double) source.getValue();
            Vector3f axis = new Vector3f(new float[] {0, 0, 1});
            Vector3f knife_centre = new Vector3f(new float[] {0,-2,0});
            Vector3f translation = new Vector3f(new float[] {0,0,0});

            Matrix4f full_transform = make_matrix(knife_tilt, axis, knife_centre, translation);
            microtome_universe.getContent("knife.stl").setTransform(new Transform3D(full_transform));
             }
    }

    class ArcListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
        }

        public void update_tilt (double tilt, double rotation, Vector3f tilt_axis, Vector3f rotation_axis, Vector3f arc_centre) {
            Vector3f translation = new Vector3f(new float[] {0,0,0});

            Matrix4f tilt_transform = make_matrix(tilt, tilt_axis, arc_centre, translation);
            Matrix4f rotation_transform = make_matrix(rotation, rotation_axis, arc_centre, translation);
            microtome_universe.getContent("holder_back.stl").setTransform(new Transform3D(tilt_transform));
            tilt_transform.mul(rotation_transform);
            microtome_universe.getContent("holder_front.stl").setTransform(new Transform3D(tilt_transform));
        }
    }

    class HolderBackListener extends ArcListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();
            tilt = (double) source.getValue();
            update_tilt(tilt, rotation, tilt_axis, rotation_axis, arc_centre);
        }
    }

    class HolderFrontListener extends ArcListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();
            rotation = (double) source.getValue();
            update_tilt(tilt, rotation, tilt_axis, rotation_axis, arc_centre);
        }
    }

    public static void main( String[] args )
    {

    }



}
