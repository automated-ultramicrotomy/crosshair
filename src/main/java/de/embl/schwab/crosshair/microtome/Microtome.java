package de.embl.schwab.crosshair.microtome;

import bdv.util.BdvStackSource;
import de.embl.schwab.crosshair.PlaneManager;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij3d.Content;
import ij3d.Image3DUniverse;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Matrix4d;
import org.scijava.vecmath.Vector3d;

class Microtome {
    private final Image3DUniverse universe;
    private final PlaneManager planeManager;
    private final BdvStackSource bdvStackSource;
    private final Content imageContent;

    // current angles in degrees for knife, tilt and rotation
    private double knife;
    private double tilt;
    private double rotation;

    // rotation axis of holder (== y axis), tilt axis of holder (== x axis)
    private Vector3d rotationAxis;
    private Vector3d tiltAxis;

    // centres before (initial) and after (current) resizing of microtome components
    private Vector3d currentArcCentre;
    private Vector3d currentKnifeCentre;
    private Vector3d currentHolderFront;

    // Knife normal at 0 degrees (initial)
    private Vector3d initialKnifeNormal;
    // Knife normal at current rotation
    private Vector3d currentKnifeNormal;

    // Target normal (in image space)
    private Vector3d initialTargetNormal;
    // Target normal (in microtome space) under current microtome settings
    private Vector3d currentTargetNormal;

    // Vector from left to right along knife edge (initially just the x axis)
    private Vector3d initialEdgeVector;
    // Vector from left to right along knife edge under current knife angle
    private Vector3d currentEdgeVector;

    // Transformation matrix for block, arc components and knife, to get them in starting position
    // for microtome
    // For the block this is a rotation and translation to bring the block face vertical and aligned to the knife edge
    // For the arc and knife, this is a scaling to an appropriate size for that image, then translations to sensible
    // positions for that size of image
    private Matrix4d initialBlockTransform;
    private Matrix4d arcComponentsInitialTransform;
    private Matrix4d knifeInitialTransform;

    // Transformation matrix for block to current position (under current microtome settings)
    private Matrix4d currentBlockTransform;

    // Setup parameters for microtome
    private double initialKnifeAngle;
    private double initialTiltAngle;

    // Calculated from the input planes & points, offset and tilt from block face to target
    private double initialTargetOffset;
    private double initialTargetTilt;

    private double angleKnifeTarget;
    private String[] microtomeObjectNames;

    Microtome (Image3DUniverse universe, PlaneManager planeManager, BdvStackSource bdvStackSource, Content imageContent) {
        this.universe = universe;
        this.planeManager = planeManager;
        this.bdvStackSource = bdvStackSource;
        this.imageContent = imageContent;

        rotationAxis = new Vector3d(0, 1, 0);
        tiltAxis = new Vector3d(1, 0, 0);

        initialKnifeNormal = new Vector3d(0, -1, 0);
        currentKnifeNormal = new Vector3d(0, -1, 0);
        initialEdgeVector = new Vector3d(1, 0, 0);
        currentEdgeVector = new Vector3d(1, 0,0);

    }

    double getKnife() {
        return knife;
    }

    double getTilt() {
        return tilt;
    }

    double getRotation() {
        return rotation;
    }

    double getInitialTiltAngle() {
        return initialTiltAngle;
    }

    double getInitialKnifeAngle() {
        return initialKnifeAngle;
    }

    Image3DUniverse getUniverse() {
        return universe;
    }

    PlaneManager getPlaneManager() {
        return planeManager;
    }

    Vector3d getCurrentKnifeCentre() {
        return currentKnifeCentre;
    }

    Vector3d getCurrentKnifeNormal() {
        return currentKnifeNormal;
    }

    Vector3d getCurrentArcCentre() {
        return currentArcCentre;
    }

    Vector3d getCurrentHolderFront() {
        return currentHolderFront;
    }

    Content getImageContent() {
        return imageContent;
    }

    double getInitialTargetOffset() {
        return initialTargetOffset;
    }

    double getInitialTargetTilt() {
        return initialTargetTilt;
    }

    Matrix4d getCurrentBlockTransform() {
        return currentBlockTransform;
    }

    Vector3d getCurrentEdgeVector() {
        return currentEdgeVector;
    }

    BdvStackSource getBdvStackSource() {
        return bdvStackSource;
    }

    double getAngleKnifeTarget() {
        return angleKnifeTarget;
    }

    void setArcComponentsInitialTransform(Matrix4d arcComponentsInitialTransform) {
        this.arcComponentsInitialTransform = arcComponentsInitialTransform;
    }

    void setCurrentArcCentre(Vector3d currentArcCentre) {
        this.currentArcCentre = currentArcCentre;
    }

    void setCurrentHolderFront(Vector3d currentHolderFront) {
        this.currentHolderFront = currentHolderFront;
    }

    void setInitialTargetNormal(Vector3d initialTargetNormal) {
        this.initialTargetNormal = initialTargetNormal;
    }

    void setCurrentTargetNormal(Vector3d currentTargetNormal) {
        this.currentTargetNormal = currentTargetNormal;
    }

    void setAngleKnifeTarget(double angleKnifeTarget) {
        this.angleKnifeTarget = angleKnifeTarget;
    }

    void setInitialTargetOffset(double initialTargetOffset) {
        this.initialTargetOffset = initialTargetOffset;
    }

    void setInitialTargetTilt(double initialTargetTilt) {
        this.initialTargetTilt = initialTargetTilt;
    }

    void setInitialBlockTransform(Matrix4d initialBlockTransform) {
        this.initialBlockTransform = initialBlockTransform;
    }

    void setInitialKnifeAngle(double initialKnifeAngle) {
        this.initialKnifeAngle = initialKnifeAngle;
    }

    void setInitialTiltAngle(double initialTiltAngle) {
        this.initialTiltAngle = initialTiltAngle;
    }

    void setKnifeInitialTransform(Matrix4d knifeInitialTransform) {
        this.knifeInitialTransform = knifeInitialTransform;
    }

    void setCurrentKnifeCentre(Vector3d currentKnifeCentre) {
        this.currentKnifeCentre = currentKnifeCentre;
    }

    void setMicrotomeObjectNames(String[] microtomeObjectNames) {
        this.microtomeObjectNames = microtomeObjectNames;
    }

    void setRotation(double rotation) {
        this.rotation = rotation;
        updateTiltRotationBlock();
    }

    void setTilt(double tilt) {
        this.tilt = tilt;
        updateTiltRotationBlock();
    }

    void setKnife(double knife) {
        this.knife = knife;
        Vector3d axis = new Vector3d(new double[] {0, 0, 1});
        Vector3d translation = new Vector3d(new double[] {0,0,0});

        Matrix4d fullTransform = GeometryUtils.makeMatrix(knife, axis, currentKnifeCentre, translation);

        // Update normal and edge vector
        Transform3D knifeTiltTransform = new Transform3D(fullTransform);
        knifeTiltTransform.transform(initialKnifeNormal, currentKnifeNormal);
        knifeTiltTransform.transform(initialEdgeVector, currentEdgeVector);

        // Update angle to target plane
        updateAngleKnifeTarget();

        // Account for initial scaling of knife
        Matrix4d initialTransformForKnife = new Matrix4d(knifeInitialTransform);
        fullTransform.mul(initialTransformForKnife);
        universe.getContent("/knife.stl").setTransform(new Transform3D(fullTransform));
    }

    private void updateTiltRotationBlock () {
        Vector3d translation = new Vector3d(new double[] {0,0,0});
        Matrix4d initialTransformForArc = new Matrix4d(arcComponentsInitialTransform);
        Matrix4d initBlockTransform = new Matrix4d(initialBlockTransform);

        Matrix4d tiltTransform = GeometryUtils.makeMatrix(tilt, tiltAxis, currentArcCentre, translation);
        Vector3d rotAxisAfterTilt = new Vector3d();
        new Transform3D(tiltTransform).transform(rotationAxis, rotAxisAfterTilt);
        Matrix4d rotationTransform = GeometryUtils.makeMatrix(rotation, rotAxisAfterTilt, currentArcCentre, translation);

        // Tilt the rotation axis
        universe.getContent("rotationAxis").setTransform(new Transform3D(tiltTransform));

        // tilt holder components, accounting for scaling. Scale around global, tilt around global, rotate around global
        // see https://stackoverflow.com/questions/21923482/rotate-and-translate-object-in-local-and-global-orientation-using-glm
        Matrix4d holderBackTransform = new Matrix4d(tiltTransform);
        holderBackTransform.mul(initialTransformForArc);
        universe.getContent("/holder_back.stl").setTransform(new Transform3D(holderBackTransform));

        Matrix4d holderFrontTransform = new Matrix4d(rotationTransform);
        holderFrontTransform.mul(holderBackTransform);
        universe.getContent("/holder_front.stl").setTransform(new Transform3D(holderFrontTransform));

        // transform for block, global initial rotate & translate, then global tilt and global rotate
        Matrix4d blockTiltTransform = GeometryUtils.makeMatrix(tilt - initialTiltAngle, tiltAxis, currentArcCentre, translation);
        Matrix4d blockTransform = new Matrix4d(rotationTransform);
        blockTransform.mul(blockTiltTransform);
        blockTransform.mul(initBlockTransform);

        Transform3D finalTransform = new Transform3D(blockTransform);
        currentBlockTransform = blockTransform;

        // Update target normal
        finalTransform.transform(initialTargetNormal, currentTargetNormal);
        updateAngleKnifeTarget();

        imageContent.setTransform(finalTransform);
        for (String planeName : new String[] {"target", "block"}) {
            universe.getContent(planeName).setTransform(finalTransform);
        }
    }

    private void updateAngleKnifeTarget() {
        double angle = GeometryUtils.convertToDegrees(currentKnifeNormal.angle(currentTargetNormal));
        //  want smallest possible angle between planes, disregard orientation of normals
        if (angle > 90) {
            angle = 180 - angle;
        }
        angleKnifeTarget = angle;

        // check angle - do colour change
        //TODO - make threshold adjustable
        if (angleKnifeTarget < 0.1) {
            planeManager.setTargetPlaneAlignedColour();
        } else {
            planeManager.setTargetPlaneNotAlignedColour();
        }
    }

    void resetMicrotome () {
        initialBlockTransform.setIdentity();

        // make microtome models invisible
        for (String name : microtomeObjectNames) {
            if (universe.contains(name)) {
                universe.getContent(name).setVisible(false);
            }
        }

        imageContent.setTransform(new Transform3D());
        universe.centerSelected(imageContent);
        universe.getContent("rotationAxis").setVisible(false);
        planeManager.redrawCurrentPlanes();

        //TODO - reset other variables
    }

}
