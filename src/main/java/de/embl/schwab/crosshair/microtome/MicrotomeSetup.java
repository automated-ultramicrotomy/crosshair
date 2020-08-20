package de.embl.schwab.crosshair.microtome;

import customnode.CustomMesh;
import customnode.Tube;
import de.embl.schwab.crosshair.PlaneManager;
import de.embl.schwab.crosshair.io.STLResourceLoader;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.RealPoint;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static de.embl.schwab.crosshair.utils.GeometryUtils.compose;
import static java.lang.Math.abs;

class MicrotomeSetup {

    private Microtome microtome;
    private final Image3DUniverse universe;
    private Content imageContent;
    private PlaneManager planeManager;

    private Vector3d initialArcCentre;
    private Vector3d initialKnifeCentre;
    // Point inside the back part of the holder - used to place end of rotation axis cylinder
    private Vector3d currentInsideHolderBack;
    // scale factor of initial scaling of all microtome components
    private double microtomeComponentsScaleFactor;

    private Map<String, CustomMesh> microtomeSTLs;

    public MicrotomeSetup (Microtome microtome) {
        this.microtome = microtome;
        this.universe = microtome.getUniverse();
        this.planeManager = microtome.getPlaneManager();
        this.imageContent = microtome.getImageContent();

        // Values from stl files from blender
        initialArcCentre = new Vector3d(0,1,0);
        initialKnifeCentre = new Vector3d(0,-2,0);

        loadMicrotomeMeshes();
    }

    private void loadMicrotomeMeshes() {
        // NOTE: Orientation of axes matches those in original blender file, object positions also match
        microtomeSTLs = new HashMap<>();
        String[] stlFiles = {"/arc.stl", "/holder_back.stl", "/holder_front.stl", "/knife.stl"};
        for (String file: stlFiles) {
            Map<String, CustomMesh> currentStl = STLResourceLoader.loadSTL(file);
            // in case there are multiple objects in single stl file
            for (String key : currentStl.keySet()) {
                microtomeSTLs.put(key, currentStl.get(key));
            }
        }
        microtome.setMicrotomeObjectNames(stlFiles);
    }

    public void initialiseMicrotome () {
        int microtomePiecesAdded = 0;
        //TODO - setup so doesn't reload scale if just change inital values

        for (String key : microtomeSTLs.keySet()) {
            // TODO - set as locked - should probably set my other custom meshes to be locked too?
            if (!universe.contains(key)) {
                universe.addCustomMesh(microtomeSTLs.get(key), key);
                universe.getContent(key).setLocked(true);
                microtomePiecesAdded += 1;
            } else {
                universe.getContent(key).setVisible(true);
            }
        }

        // resize microtome parts, if first time adding them
        if (microtomePiecesAdded > 0) {
            resizeMicrotomeParts();
        }

        if (!universe.contains("rotationAxis")) {
        //  width of knife is about 2 intially from blender file
            ArrayList<Point3f> tubeEnds = new ArrayList<>();
            Vector3d currentKnifeCentre = microtome.getCurrentKnifeCentre();
            tubeEnds.add(new Point3f((float) currentKnifeCentre.getX(),
                    (float) (currentKnifeCentre.getY() + (2*microtomeComponentsScaleFactor)),
                    (float) currentKnifeCentre.getZ()));
            tubeEnds.add(new Point3f((float) currentInsideHolderBack.getX(),
                    (float) currentInsideHolderBack.getY(), (float) currentInsideHolderBack.getZ()));
            universe.addCustomMesh(new Tube(tubeEnds, (float) (0.05*microtomeComponentsScaleFactor)), "rotationAxis");
            universe.getContent("rotationAxis").setVisible(false);
        } else {
            universe.getContent("rotationAxis").setVisible(false);
        }

        Matrix4d initialBlockTransform = setupBlockOrientation(microtome.getInitialKnifeAngle());
        microtome.setInitialBlockTransform(initialBlockTransform);

        // initialise target normals
        Vector3d initialTargetNormal = new Vector3d(planeManager.getPlaneNormals().get("target"));
        microtome.setInitialTargetNormal(initialTargetNormal);
        Vector3d currentTargetNormal = new Vector3d(initialTargetNormal);
        new Transform3D(initialBlockTransform).transform(currentTargetNormal);
        microtome.setCurrentTargetNormal(currentTargetNormal);
        microtome.setAngleKnifeTarget(0);

        calculateTargetOffsetTilt();
    }

    private void calculateTargetOffsetTilt() {
        Map<String, RealPoint> namedVertices = planeManager.getNamedVertices();
        Map<String, Vector3d> planeNormals = planeManager.getPlaneNormals();
        Vector3d blockNormal = planeNormals.get("block");
        Vector3d targetNormal = planeNormals.get("target");

        double[] topLeft = new double[3];
        double[] bottomLeft = new double[3];
        double[] bottomRight = new double[3];
        namedVertices.get("Top Left").localize(topLeft);
        namedVertices.get("Bottom Left").localize(bottomLeft);
        namedVertices.get("Bottom Right").localize(bottomRight);

        // Vector along the bottom edge of block, left to right
        Vector3d bottomEdgeVector = new Vector3d();
        bottomEdgeVector.sub(new Vector3d(bottomRight), new Vector3d(bottomLeft));

        // Vector pointing 'up' along left edge of block. Bear in mind may not be exactly perpendicular to the edge_vector
        // due to not perfectly rectangular block shape. I correct for this later
        Vector3d upLeftSideVector = new Vector3d();
        upLeftSideVector.sub(new Vector3d(topLeft), new Vector3d(bottomLeft));

        // Calculate line perpendicular to x (edge vector), in plane of block face
        Vector3d vertical = new Vector3d();
        vertical.cross(blockNormal, bottomEdgeVector);

        // But depending on orientation of block normal, this could be 'up' or 'down' relative to user
        // to force this to be 'up' do:
        if (upLeftSideVector.dot(vertical) < 0) {
            vertical.negate();
        }

        microtome.setInitialTargetOffset( GeometryUtils.rotationPlaneToPlane(vertical, targetNormal, blockNormal) );

        // Calculate initial target tilt
        // Calculate tilt (about the new axis of rotation, i.e. about line of intersection from previous calc)
        // new axis of rotation, unknown orientation
        Vector3d axisRotation = new Vector3d();
        axisRotation.cross(targetNormal, vertical);
        // I force this to be pointing in the general direction of the edge vector to give consistent clockwise vs anticlockwise
        // i.e. I look down the right side of the vector
        if (axisRotation.dot(bottomEdgeVector) < 0) {
            axisRotation.negate();
        }

        Vector3d intersectionNormal = new Vector3d();
        intersectionNormal.cross(axisRotation, vertical);
        microtome.setInitialTargetTilt( GeometryUtils.rotationPlaneToPlane(axisRotation, targetNormal, intersectionNormal) );

    }



    private void resizeMicrotomeParts () {

        // scale meshes / position them
        Point3d minArc = new Point3d();
        Point3d maxArc = new Point3d();
        universe.getContent("/arc.stl").getMax(maxArc);
        universe.getContent("/arc.stl").getMin(minArc);
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
        microtomeComponentsScaleFactor = scaleFactor;

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
        Point3d minHolderFront = new Point3d(0, 3, 0);

        // holder front after scaling
        Point3d holderFrontAfter = new Point3d(minHolderFront.getX(), minHolderFront.getY(), minHolderFront.getZ());
        trans.transform(holderFrontAfter);
        double yHolderFront = holderFrontAfter.getY();

        double translateY = maxImageDistance - yHolderFront;

        Matrix4d translateArcComponents = new Matrix4d(1, 0, 0, 0,
                0, 1, 0, translateY,
                0, 0, 1, 0,
                0, 0, 0, 1);

        translateArcComponents.mul(scaleMatrix);
        Transform3D arcComponentsTransform = new Transform3D(translateArcComponents);
        String[] arcComponents = new String[]{"/arc.stl", "/holder_front.stl", "/holder_back.stl"};
        for (String key : arcComponents) {
            universe.getContent(key).setTransform(arcComponentsTransform);
        }
        Matrix4d arcComponentsInitialTransform = translateArcComponents;
        microtome.setArcComponentsInitialTransform(arcComponentsInitialTransform);

        // arc centre after scaling
        Point3d arcC = new Point3d(initialArcCentre.getX(), initialArcCentre.getY(), initialArcCentre.getZ());
        arcComponentsTransform.transform(arcC);
        microtome.setCurrentArcCentre( new Vector3d(arcC.getX(), arcC.getY(), arcC.getZ()) );

        // holder front after scaling - save for cutting range
        arcComponentsTransform.transform(minHolderFront);
        microtome.setCurrentHolderFront( new Vector3d(minHolderFront.getX(), minHolderFront.getY(), minHolderFront.getZ()) );

        //  Point inside holder after scaling - save for end of rotation axis
        Point3d insideHolderBack = new Point3d(0, 4, 0);
        arcComponentsTransform.transform(insideHolderBack);
        currentInsideHolderBack = new Vector3d(insideHolderBack.getX(), insideHolderBack.getY(), insideHolderBack.getZ());

        // Set knife to same distance
        // holder front after scaling
        Point3d knifeCentreAfter = new Point3d(initialKnifeCentre.getX(), initialKnifeCentre.getY(), initialKnifeCentre.getZ());
        trans.transform(knifeCentreAfter);
        double yKnife = knifeCentreAfter.getY();

        double translateK = -maxImageDistance - yKnife;

        Matrix4d translateKnife = new Matrix4d(1, 0, 0, 0,
                0, 1, 0, translateK,
                0, 0, 1, 0,
                0, 0, 0, 1);

        translateKnife.mul(scaleMatrix);
        Transform3D knifeTransform = new Transform3D(translateKnife);
        universe.getContent("/knife.stl").setTransform(knifeTransform);
        microtome.setKnifeInitialTransform( translateKnife );

        // knife centre after scaling
        Point3d knifeC = new Point3d(initialKnifeCentre.getX(), initialKnifeCentre.getY(), initialKnifeCentre.getZ());
        knifeTransform.transform(knifeC);
        microtome.setCurrentKnifeCentre( new Vector3d(knifeC.getX(), knifeC.getY(), knifeC.getZ()) );
    }

    private Matrix4d setupBlockOrientation(double initialKnifeAngle) {
        String[] planeNames = {"target", "block"};
        //reset translation / rotation in case it has been modified
        imageContent.setTransform(new Transform3D());
        for (String name : planeNames) {
            universe.getContent(name).setTransform(new Transform3D());
        }

        Map<String, RealPoint> namedVertices = planeManager.getNamedVertices();

        // check normal in right orientation, coming out of block surface
        double[] topLeft = new double[3];
        double[] bottomLeft = new double[3];
        double[] bottomRight = new double[3];
        namedVertices.get("Top Left").localize(topLeft);
        namedVertices.get("Bottom Left").localize(bottomLeft);
        namedVertices.get("Bottom Right").localize(bottomRight);

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
        GeometryUtils.compose(scijavaFormMatrix, new Vector3d(bottomEdgeCentre.getX(), bottomEdgeCentre.getY(), bottomEdgeCentre.getZ()), new Vector3d(endBottomEdgeCentre.getX(), endBottomEdgeCentre.getY(), endBottomEdgeCentre.getZ()), finalSetupTransform);

        Transform3D finalTransform = new Transform3D(finalSetupTransform);
        imageContent.setTransform(finalTransform);
        for (String name : planeNames) {
            universe.getContent(name).setTransform(finalTransform);
        }

        // change so view rotates about (0,0,0)
        universe.centerAt(new Point3d());

        return finalSetupTransform;
    }


}
