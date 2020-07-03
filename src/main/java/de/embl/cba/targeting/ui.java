package de.embl.cba.targeting;

import bdv.util.BdvHandle;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.RealPoint;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.AxisAngle4f;
import org.scijava.vecmath.Matrix4f;
import org.scijava.vecmath.Vector3f;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

public class ui extends JPanel {
    private BdvHandle bdvHandle;
    Image3DUniverse microtome_universe;
    Content imageContent;
    private double knife_tilt;
    private double tilt;
    private double rotation;
    Vector3f rotation_axis;
    Vector3f tilt_axis;
    Vector3f arc_centre;
    RealPoint selected_point;
    Map<String, RealPoint> pointmap;


    public ui (Image3DUniverse microtome_universe, RealPoint selected_point, Map<String, RealPoint> pointmap,
               BdvHandle bdvHandle, Content imageContent) {
        this.microtome_universe = microtome_universe;
        this.selected_point = selected_point;
        this.pointmap = pointmap;
        this.bdvHandle = bdvHandle;
        this.imageContent = imageContent;
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
