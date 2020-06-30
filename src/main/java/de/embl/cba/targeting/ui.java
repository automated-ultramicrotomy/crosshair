package de.embl.cba.targeting;

import ij3d.Image3DUniverse;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.AxisAngle4f;
import org.scijava.vecmath.Matrix4f;
import org.scijava.vecmath.Vector3f;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ui extends JPanel {
    Image3DUniverse microtome_universe;
    private double knife_tilt;
    private double tilt;
    private double rotation;

    public ui ( Image3DUniverse microtome_universe) {
        this.microtome_universe = microtome_universe;
        JFrame microtome_panel = new JFrame("frame");
        JPanel p = new JPanel();

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
        arc_rotation.addChangeListener(new ArcListener());
        arc_rotation.setMajorTickSpacing(10);
        arc_rotation.setMinorTickSpacing(1);
        arc_rotation.setPaintTicks(true);
        arc_rotation.setPaintLabels(true);
        p.add(arc_rotation);

        JSlider holder_rotation = new JSlider(JSlider.HORIZONTAL, -180, 180, 0);
        holder_rotation.addChangeListener(new HolderListener());
        holder_rotation.setMajorTickSpacing(10);
        holder_rotation.setMinorTickSpacing(1);
        holder_rotation.setPaintTicks(true);
        holder_rotation.setPaintLabels(true);
        p.add(holder_rotation);


        microtome_panel.add(p);
        microtome_panel.show();

    }

    private Matrix4f make_matrix (double angle_degrees, Vector3f axis, Vector3f rotation_centre, Vector3f translation) {
        float angle_rad = (float) (angle_degrees * Math.PI / 180);
        Matrix4f m = new Matrix4f();
        compose(new AxisAngle4f(axis, angle_rad), rotation_centre, translation, m);
        return m;
    }

    //        Interactive transform setter in 3d viewer: https://github.com/fiji/3D_Viewer/blob/master/src/main/java/ij3d/gui/InteractiveTransformDialog.java
    //        setting of transform: https://github.com/fiji/3D_Viewer/blob/ed05e4b2275ad6ad7c94b0e22f4789ebd3472f4d/src/main/java/ij3d/Executer.java
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
            JSlider source = (JSlider) e.getSource();
            tilt = (double) source.getValue();
            Vector3f axis = new Vector3f(new float[] {1, 0, 0});
            Vector3f arc_centre = new Vector3f(new float[] {0,1,0});
            Vector3f translation = new Vector3f(new float[] {0,0,0});

            Matrix4f full_transform = make_matrix(tilt, axis, arc_centre, translation);
            microtome_universe.getContent("holder_back.stl").setTransform(new Transform3D(full_transform));
            microtome_universe.getContent("holder_front.stl").setTransform(new Transform3D(full_transform));
        }
    }

    class HolderListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();
            rotation = (double) source.getValue();
            Vector3f rotation_axis = new Vector3f(new float[] {0, 1, 0});
            Vector3f tilt_axis = new Vector3f(new float[] {1, 0, 0});
            Vector3f arc_centre = new Vector3f(new float[] {0,1,0});
            Vector3f translation = new Vector3f(new float[] {0,0,0});

            Matrix4f tilt_transform = make_matrix(tilt, tilt_axis, arc_centre, translation);
            Matrix4f rotation_transform = make_matrix(rotation, rotation_axis, arc_centre, translation);
            tilt_transform.mul(rotation_transform);
            microtome_universe.getContent("holder_front.stl").setTransform(new Transform3D(tilt_transform));
        }
    }

    public static void main( String[] args )
    {

    }



}
