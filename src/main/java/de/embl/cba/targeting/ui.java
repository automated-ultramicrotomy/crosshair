package de.embl.cba.targeting;

import ij3d.Image3DUniverse;
import org.scijava.java3d.Transform3D;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ui extends JPanel {
    Image3DUniverse microtome_universe;

    public ui ( Image3DUniverse microtome_universe) {
        this.microtome_universe = microtome_universe;
        JFrame yo = new JFrame("frame");
        JPanel p = new JPanel();
        JSlider rot = new JSlider(JSlider.HORIZONTAL, -30, 30, 0);
        rot.addChangeListener(new SliderListener());
        rot.setMajorTickSpacing(10);
        rot.setMinorTickSpacing(1);
        rot.setPaintTicks(true);
        rot.setPaintLabels(true);

        p.add(rot);
        yo.add(p);
        yo.show();
    }

    class SliderListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();
//            if (!source.getValueIsAdjusting()) {
                // Create a new Transform3D object
                Transform3D t3d = new Transform3D();

                double rotation = (double) source.getValue();
                System.out.println(rotation);

                // Make it a 45 degree rotation around the local y-axis
                double rotation_in_rad = rotation * Math.PI / 180;
                t3d.rotZ(rotation_in_rad);
                System.out.println(rotation_in_rad);

                // Apply the transformation to the Content. This concatenates
                // the previous present transformation with the specified one
                microtome_universe.getContent("knife.stl").setTransform(t3d);
            }
//        }
    }

    public static void main( String[] args )
    {

    }



}
