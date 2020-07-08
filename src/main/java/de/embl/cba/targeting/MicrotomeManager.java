package de.embl.cba.targeting;

import customnode.CustomMesh;
import customnode.MeshLoader;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.abs;

//TODO - add all sliders up here?
//TODO - get sliders to handle doubles!! Also have a box to type value if don't want to use slider - more precise
//TODO - some variable for block has been initialised at least once before can control it with sliders
//TODO - save initial tilt / knife values when initialise clicked, so don't change with those sliders anymore

public class MicrotomeManager extends JPanel {

    private final Image3DUniverse universe;
    private final PlaneManager planeManager;
    private Map<String, CustomMesh> microtomeSTLs;
    private final Content imageContent;

    private double knifeTilt;
    private double tilt;
    private double rotation;

    private Vector3f rotationAxis;
    private Vector3f tiltAxis;
    private Vector3f initialArcCentre;
    private Vector3f initialKnifeCentre;
    private Vector3f currentKnifeCentre;
    private Vector3f currentArcCentre;

    private Matrix4d initialBlockTransform;
    private Matrix4d arcComponentsInitialTransform;
    private Matrix4d knifeInitialTransform;

    private JSlider initialKnifeAngle;
    private JSlider initialTiltAngle;
    private JSlider knifeRotation;
    private JSlider arcRotation;
    private JSlider holderRotation;

    public MicrotomeManager(PlaneManager planeManager, Image3DUniverse universe, Content imageContent) {

        this.planeManager = planeManager;
        this.universe = universe;
        this.imageContent = imageContent;

        rotationAxis = new Vector3f(new float[] {0, 1, 0});
        tiltAxis = new Vector3f(new float[] {1, 0, 0});
        initialArcCentre = new Vector3f(new float[] {0,1,0});
        initialKnifeCentre = new Vector3f(new float[] {0,-2,0});

        loadMicrotomeMeshes();

        JFrame microtomePanel = new JFrame("frame");
        JPanel p = new JPanel();

        ActionListener vertexListener = new vertexPointListener();
        JButton topLeft = new JButton("Top Left");
        topLeft.setActionCommand("top_left");
        topLeft.addActionListener(vertexListener);
        p.add(topLeft);

        JButton topRight = new JButton("Top Right");
        topRight.setActionCommand("top_right");
        topRight.addActionListener(vertexListener);
        p.add(topRight);

        JButton bottomLeft = new JButton("Bottom Left");
        bottomLeft.setActionCommand("bottom_left");
        bottomLeft.addActionListener(vertexListener);
        p.add(bottomLeft);

        JButton bottomRight = new JButton("Bottom Right");
        bottomRight.setActionCommand("bottom_right");
        bottomRight.addActionListener(vertexListener);
        p.add(bottomRight);

        ActionListener blockListener = new blockListener();
        JButton initialiseBlock = new JButton("Initialise block");
        initialiseBlock.setActionCommand("initialise_block");
        initialiseBlock.addActionListener(blockListener);
        p.add(initialiseBlock);

        // Initial knife angle
        initialKnifeAngle = new JSlider(JSlider.HORIZONTAL, -30, 30, 0);
        initialKnifeAngle.setMajorTickSpacing(10);
        initialKnifeAngle.setMinorTickSpacing(1);
        initialKnifeAngle.setPaintTicks(true);
        initialKnifeAngle.setPaintLabels(true);
        p.add(initialKnifeAngle);

        // Initial tilt angle
        initialTiltAngle = new JSlider(JSlider.HORIZONTAL, -20, 20, 0);
        initialTiltAngle.setMajorTickSpacing(10);
        initialTiltAngle.setMinorTickSpacing(1);
        initialTiltAngle.setPaintTicks(true);
        initialTiltAngle.setPaintLabels(true);
        p.add(initialTiltAngle);

//        Orientation of axes matches those in original blender file, object positions also match
//        Interactive transform setter in 3d viewer: https://github.com/fiji/3D_Viewer/blob/master/src/main/java/ij3d/gui/InteractiveTransformDialog.java
        knifeRotation = new JSlider(JSlider.HORIZONTAL, -30, 30, 0);
        knifeRotation.addChangeListener(new KnifeListener());
        knifeRotation.setMajorTickSpacing(10);
        knifeRotation.setMinorTickSpacing(1);
        knifeRotation.setPaintTicks(true);
        knifeRotation.setPaintLabels(true);
        p.add(knifeRotation);

        arcRotation = new JSlider(JSlider.HORIZONTAL, -20, 20, 0);
        arcRotation.addChangeListener(new HolderBackListener());
        arcRotation.setMajorTickSpacing(10);
        arcRotation.setMinorTickSpacing(1);
        arcRotation.setPaintTicks(true);
        arcRotation.setPaintLabels(true);
        p.add(arcRotation);

        holderRotation = new JSlider(JSlider.HORIZONTAL, -180, 180, 0);
        holderRotation.addChangeListener(new HolderFrontListener());
        holderRotation.setMajorTickSpacing(60);
        holderRotation.setMinorTickSpacing(1);
        holderRotation.setPaintTicks(true);
        holderRotation.setPaintLabels(true);
        p.add(holderRotation);

        microtomePanel.add(p);
        microtomePanel.show();

    }

    private void loadMicrotomeMeshes() {
        microtomeSTLs = new HashMap<>();
        String[] stlFiles = {"/arc.stl", "/holder_back.stl", "/holder_front.stl", "/knife.stl"};
        for (String file: stlFiles) {
            try {
                URL resource = Crosshair.class.getResource(file);
                String resourceFile = Paths.get(resource.toURI()).toFile().getAbsolutePath();

                Map<String, CustomMesh> currentStl = MeshLoader.loadSTL(resourceFile);
                // in case there are multiple objects in single stl file
                for (String key : currentStl.keySet()) {
                    microtomeSTLs.put(key, currentStl.get(key));
                }
            } catch (URISyntaxException e) {
                System.out.println("Error reading from resource");
                e.printStackTrace();
            }
        }
    }

    class vertexPointListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            planeManager.nameVertex(e.getActionCommand());
        }
    }

    class blockListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            //TODO - setup so doesn't reload scale if just change inital values
            for (String key : microtomeSTLs.keySet()) {
                System.out.println(key);
                // TODO - set as locked - should probably set my other custom meshes to be locked too?
                if (!universe.contains(key)) {
                    universe.addCustomMesh(microtomeSTLs.get(key), key);
                    universe.getContent(key).setLocked(true);
                }
            }

            // scale meshes / position them
            Point3d minArc = new Point3d();
            Point3d maxArc = new Point3d();
            universe.getContent("arc.stl").getMax(maxArc);
            universe.getContent("arc.stl").getMin(minArc);
            double heightArc = abs(maxArc.getZ() - minArc.getZ());

            Point3d minImage = new Point3d();
            Point3d maxImage = new Point3d();
            imageContent.getMax(maxImage);
            imageContent.getMin(minImage);
            ArrayList<Double> dims = new ArrayList<>();
            dims.add(abs(maxImage.getX() - minImage.getX()));
            dims.add(abs(maxImage.getY() - minImage.getY()));
            dims.add(abs(maxImage.getZ() - minImage.getZ()));
            double endHeight = Collections.max(dims);

            double scaleFactor = endHeight / heightArc;

            Matrix4d scaleMatrix = new Matrix4d(scaleFactor, 0, 0, 0,
                    0, scaleFactor, 0, 0,
                    0, 0, scaleFactor, 0,
                    0, 0, 0, 1);

            Transform3D trans = new Transform3D(scaleMatrix);

            Vector3d maxImageVector = new Vector3d();
            maxImageVector.sub(new Vector3d(maxImage.getX(), maxImage.getY(), maxImage.getZ()),
                                    new Vector3d(minImage.getX(), minImage.getY(), minImage.getZ()));
            double maxImageDistance = maxImageVector.length();

            // translate so front of holder one max image distance away from 0,0,0
            // location of min holder front is approximate from original blender file
            Point3d minHolderFront = new Point3d(0, 3,0);

            // hodler front after scaling
            Point3d holderFrontAfter = new Point3d(minHolderFront.getX(), minHolderFront.getY(), minHolderFront.getZ());
            trans.transform(holderFrontAfter);
            double yHolderFront = holderFrontAfter.getY();



            double translateY = maxImageDistance - yHolderFront;

            Matrix4d translateArcComponents = new Matrix4d(1, 0, 0, 0,
                    0, 1, 0, translateY,
                    0,0,1,0,
                    0,0,0,1);

            translateArcComponents.mul(scaleMatrix);
            Transform3D arcComponentsTransform = new Transform3D(translateArcComponents);
            String[] arcComponents = new String[] {"arc.stl", "holder_front.stl", "holder_back.stl"};
            for (String key : arcComponents) {
                universe.getContent(key).setTransform(arcComponentsTransform);
            }
            arcComponentsInitialTransform = translateArcComponents;

            // arc centre after scaling
            Point3d arcC = new Point3d(initialArcCentre.getX(), initialArcCentre.getY(), initialArcCentre.getZ());
            arcComponentsTransform.transform(arcC);
            currentArcCentre = new Vector3f((float) arcC.getX(), (float) arcC.getY(), (float) arcC.getZ());

            //TODO - set arc centre and knife centre to value after scaling and translation

            // Set knife to same distance
            // hodler front after scaling
            Point3d knifeCentreAfter = new Point3d(initialKnifeCentre.getX(), initialKnifeCentre.getY(), initialKnifeCentre.getZ());
            trans.transform(knifeCentreAfter);
            double yKnife = knifeCentreAfter.getY();
            System.out.println("yknife");
            System.out.println(yKnife);

            double translateK = -maxImageDistance - yKnife;

            Matrix4d translateKnife = new Matrix4d(1, 0, 0, 0,
                    0, 1, 0, translateK,
                    0,0,1,0,
                    0,0,0,1);

            translateKnife.mul(scaleMatrix);
            Transform3D knifeTransform = new Transform3D(translateKnife);
            universe.getContent("knife.stl").setTransform(knifeTransform);
            knifeInitialTransform = translateKnife;

            // knife centre after scaling
            Point3d knifeC = new Point3d(initialKnifeCentre.getX(), initialKnifeCentre.getY(), initialKnifeCentre.getZ());
            knifeTransform.transform(knifeC);
            currentKnifeCentre = new Vector3f((float) knifeC.getX(), (float) knifeC.getY(), (float) knifeC.getZ());

            initialBlockTransform = setupBlockOrientation(initialKnifeAngle.getValue());
            knifeRotation.setValue(initialKnifeAngle.getValue());
            arcRotation.setValue(initialTiltAngle.getValue());
            holderRotation.setValue(0);
            planeManager.updatePlanesInPlace();

        }
    }

    private Matrix4d setupBlockOrientation(double initialKnifeAngle) {
        //reset translation / rotation in case it has been modified
        imageContent.setTransform(new Transform3D());

        Map<String, RealPoint> namedVertices = planeManager.getNamedVertices();

        // check normal in right orientation, coming out of block surface
        double[] topLeft = new double[3];
        double[] bottomLeft = new double[3];
        double[] bottomRight = new double[3];
        namedVertices.get("top_left").localize(topLeft);
        namedVertices.get("bottom_left").localize(bottomLeft);
        namedVertices.get("bottom_right").localize(bottomRight);

        Vector3d bottomEdgeVector = new Vector3d();
        bottomEdgeVector.sub(new Vector3d(bottomRight), new Vector3d(bottomLeft));

        double lengthEdge = bottomEdgeVector.length();

        Vector3d upLeftSideVector = new Vector3d();
        upLeftSideVector.sub(new Vector3d(topLeft), new Vector3d(bottomLeft));

        // bottom edge cross up left side, gives a normal that points out of teh block surface
        Vector3d blockNormal = new Vector3d();
        blockNormal.cross(bottomEdgeVector, upLeftSideVector);
        blockNormal.normalize();

        Vector3d endBlockNormal = new Vector3d(0, -1, 0);
        Vector3d endEdgeVector = new Vector3d(1, 0, 0);
        //TODO - currently hard code knife angle, make user parameter
        AxisAngle4d initialKnifeOffset = new AxisAngle4d(new Vector3d(0, 0, 1), initialKnifeAngle * Math.PI / 180);
        Matrix4d matrixInitialKnifeOffset = new Matrix4d();
        matrixInitialKnifeOffset.set(initialKnifeOffset);
        Transform3D initialKnifeTransform = new Transform3D(matrixInitialKnifeOffset);

        initialKnifeTransform.transform(endBlockNormal);
        endBlockNormal.normalize();
        initialKnifeTransform.transform(endEdgeVector);
        endEdgeVector.normalize();

        // normalise just in case
        bottomEdgeVector.normalize();

        // what is transform to bring block normal to be end block normal & edge vector to be end edge vector?
        //TODO - maybe translate so centre of edge vector == centre of knife location
        Rotation endRotation = new Rotation(new Vector3D(blockNormal.getX(), blockNormal.getY(), blockNormal.getZ()),
                new Vector3D(bottomEdgeVector.getX(), bottomEdgeVector.getY(), bottomEdgeVector.getZ()),
                new Vector3D(endBlockNormal.getX(), endBlockNormal.getY(), endBlockNormal.getZ()),
                new Vector3D(endEdgeVector.getX(), endEdgeVector.getY(), endEdgeVector.getZ()));


        // convert back to scijava conventions
        double[][] endRotMatrix = endRotation.getMatrix();
        Matrix4d scijavaFormMatrix = new Matrix4d(endRotMatrix[0][0], endRotMatrix[0][1], endRotMatrix[0][2], 0,
                endRotMatrix[1][0],endRotMatrix[1][1],endRotMatrix[1][2], 0,
                endRotMatrix[2][0],endRotMatrix[2][1],endRotMatrix[2][2], 0,
                0,0,0,1);

        // initial position of bottom edge centre
        Vector3d bottomEdgeCentre = new Vector3d(bottomLeft);
        bottomEdgeCentre.add(new Vector3d(bottomEdgeVector.getX() * 0.5 * lengthEdge,
                bottomEdgeVector.getY() * 0.5 * lengthEdge,
                bottomEdgeVector.getZ() * 0.5 * lengthEdge));
        // vector from initial to final position of bottom edge centre
        Vector3d endBottomEdgeCentre = new Vector3d(0, 0, 0);
        endBottomEdgeCentre.sub(bottomEdgeCentre);

        //final transform
        Matrix4d finalSetupTransform = new Matrix4d();
        // rotate about the initial position of bottom edge centre, then translate bottom edge centre to (0,0,0)
        compose(scijavaFormMatrix, new Vector3d(bottomEdgeCentre.getX(), bottomEdgeCentre.getY(), bottomEdgeCentre.getZ()), new Vector3d(endBottomEdgeCentre.getX(), endBottomEdgeCentre.getY(), endBottomEdgeCentre.getZ()), finalSetupTransform);
        imageContent.setTransform(new Transform3D(finalSetupTransform));
        // change so view rotates about (0,0,0)
        universe.centerAt(new Point3d());

        return finalSetupTransform;
    }

        //as here for recalculate global min max
        // https://github.com/fiji/3D_Viewer/blob/c1cba02d475a05c94aebe322c2d5d76790907d6b/src/main/java/ij3d/Image3DUniverse.java
    private double[] calculateCentre(Content imageContent) {
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



    //        The two methods below are adapted from the imagej 3d viewer
    //        Interactive transform setter in 3d viewer: https://github.com/fiji/3D_Viewer/blob/master/src/main/java/ij3d/gui/InteractiveTransformDialog.java
    //        setting of transform: https://github.com/fiji/3D_Viewer/blob/ed05e4b2275ad6ad7c94b0e22f4789ebd3472f4d/src/main/java/ij3d/Executer.java
    private Matrix4f makeMatrix(double angleDegrees, Vector3f axis, Vector3f rotationCentre, Vector3f translation) {
        float angleRad = (float) (angleDegrees * Math.PI / 180);
        Matrix4f m = new Matrix4f();
        compose(new AxisAngle4f(axis, angleRad), rotationCentre, translation, m);
        return m;
    }

    private Matrix4d makeMatrix(double angleDegrees, Vector3d axis, Vector3d rotationCentre, Vector3d translation) {
        double angleRad = angleDegrees * Math.PI / 180;
        Matrix4d m = new Matrix4d();
        compose(new AxisAngle4d(axis, angleRad), rotationCentre, translation, m);
        return m;
    }


    public void compose(final AxisAngle4f rot, final Vector3f origin,
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

    public void compose(final AxisAngle4d rot, final Vector3d origin,
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

    public void compose(final Matrix4d rot, final Vector3d origin,
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
            knifeTilt = (double) source.getValue();
            Vector3f axis = new Vector3f(new float[] {0, 0, 1});

            Vector3f translation = new Vector3f(new float[] {0,0,0});

            Matrix4f fullTransform = makeMatrix(knifeTilt, axis, currentKnifeCentre, translation);

            Matrix4f initialTransformForKnife = new Matrix4f(knifeInitialTransform);
            fullTransform.mul(initialTransformForKnife);
            universe.getContent("knife.stl").setTransform(new Transform3D(fullTransform));
             }
    }

    class ArcListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
        }

        public void updateTilt(double tilt, double rotation, Vector3f tiltAxis, Vector3f rotationAxis, Vector3f arcCentre) {
            Vector3f translation = new Vector3f(new float[] {0,0,0});

            Matrix4f initialTransformForArc = new Matrix4f(arcComponentsInitialTransform);

            Matrix4f tiltTransform = makeMatrix(tilt, tiltAxis, arcCentre, translation);
            Matrix4f rotationTransform = makeMatrix(rotation, rotationAxis, arcCentre, translation);
            // account for scaling
            Matrix4f holderBackTransform = new Matrix4f(tiltTransform);
            holderBackTransform.mul(initialTransformForArc);
            universe.getContent("holder_back.stl").setTransform(new Transform3D(holderBackTransform));
            tiltTransform.mul(rotationTransform);
            tiltTransform.mul(initialTransformForArc);
            universe.getContent("holder_front.stl").setTransform(new Transform3D(tiltTransform));

            // transform for block > must account for initial tilt
            Matrix4f blockTiltTransform = makeMatrix(tilt - initialTiltAngle.getValue(), tiltAxis, arcCentre, translation);
            blockTiltTransform.mul(rotationTransform);
            Matrix4f init_transform = new Matrix4f(initialBlockTransform);
            blockTiltTransform.mul(init_transform);

            imageContent.setTransform(new Transform3D(blockTiltTransform));
            planeManager.updatePlanesInPlace();

        }
    }

    class HolderBackListener extends ArcListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();
            tilt = (double) source.getValue();
            updateTilt(tilt, rotation, tiltAxis, rotationAxis, currentArcCentre);
        }
    }

    class HolderFrontListener extends ArcListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();
            rotation = (double) source.getValue();
            updateTilt(tilt, rotation, tiltAxis, rotationAxis, currentArcCentre);
        }
    }
}