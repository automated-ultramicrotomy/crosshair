package de.embl.cba.crosshair.microtome;

import customnode.CustomTriangleMesh;
import ij3d.Content;
import net.imglib2.RealPoint;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.*;

import java.util.ArrayList;
import java.util.Map;

import static de.embl.cba.crosshair.utils.GeometryUtils.*;
import static de.embl.cba.crosshair.utils.GeometryUtils.levelCurrentViewNormalandHorizontal;
import static java.lang.Math.*;

class Cutting {

    public void setCuttingBounds () {
//        All image vertices in microtome coordinates
        Map<String, RealPoint> vertices = planeManager.getNamedVertices();
        ArrayList<Point3d> verticesMicrotomeCoords = new ArrayList<>();
        ArrayList<Vector3d> verticesMicrotomeCoordsV = new ArrayList<>();
        ArrayList<String> vertexNames = new ArrayList<>();
        for (String key: vertices.keySet()) {
            double[] location = new double[3];
            vertices.get(key).localize(location);
            verticesMicrotomeCoords.add(new Point3d(location));
            vertexNames.add(key);
        }

        for (Point3d point : verticesMicrotomeCoords) {
            currentBlockTransform.transform(point);
            verticesMicrotomeCoordsV.add(new Vector3d(point.getX(), point.getY(), point.getZ()));
        }

//        Find one with minimum distance to Knife plane - this is the first point hit by this cutting orientation
        int indexMinDist = indexMinMaxPointsToPlane(currentKnifeCentre, currentKnifeNormal, verticesMicrotomeCoordsV,"min");
        firstTouchPointCutting = verticesMicrotomeCoordsV.get(indexMinDist);
        System.out.println(vertexNames.get(indexMinDist));
        System.out.println(firstTouchPointCutting.toString());

//        Need the coordinate where a plane identical to teh knife plane, but centred on the first touch point (imagine
//        what the knife would look like as it just touches the block) - where this intersects the midline of the microtome (NS).
//        This will be our 0, at this level of approach NS, our knife will just touch the first touch point.
        double NSZeroY;
        if (knife == 0 | firstTouchPointCutting.getX() == 0) {
            NSZeroY = firstTouchPointCutting.getY();
        } else {
            double oppositeLength = tan(convertToRadians(knife)) * firstTouchPointCutting.getX();

            if (oppositeLength > 0) {
                NSZeroY = firstTouchPointCutting.getY() - oppositeLength;
            } else {
                NSZeroY = firstTouchPointCutting.getY() + oppositeLength;
            }
        }
        NSZero = new Vector3d(0, NSZeroY,0);

//        Set cutting range so first touch point == 0
        microtomePanel.setCuttingRange(currentKnifeCentre.getY() - NSZero.getY(),
                currentHolderFront.getY() - NSZero.getY());
    }

    public void initialiseCuttingPlane () {

//        Make a plane of centred on current knife centre with width and height of 2*max distance in image (corner to corner)

//        Get maximum distance in image
        Point3d min = new Point3d();
        Point3d max = new Point3d();
        imageContent.getMax(max);
        imageContent.getMin(min);
        double maxDist = min.distance(max);
        ArrayList<Point3f> planeVertices = new ArrayList<>();

//        Add points in order top left, top right, bottom left, bottom right
        if (knife == 0) {
            planeVertices.add(new Point3f((float) -maxDist, (float) currentKnifeCentre.getY(), (float) maxDist));
            planeVertices.add(new Point3f((float) maxDist, (float) currentKnifeCentre.getY(), (float) maxDist));
            planeVertices.add(new Point3f((float) -maxDist, (float) currentKnifeCentre.getY(), (float) -maxDist));
            planeVertices.add(new Point3f((float) maxDist, (float) currentKnifeCentre.getY(), (float) -maxDist));
        } else {

            double yDist = abs(sin(convertToRadians(knife)) * maxDist);
            double xDist = abs(cos(convertToRadians(knife)) * maxDist);

            if (knife > 0) {
                planeVertices.add(new Point3f((float) -xDist, (float) (currentKnifeCentre.getY()-yDist), (float) maxDist));
                planeVertices.add(new Point3f((float) xDist, (float) (currentKnifeCentre.getY()+yDist), (float) maxDist));
                planeVertices.add(new Point3f((float) -xDist, (float) (currentKnifeCentre.getY()-yDist), (float) -maxDist));
                planeVertices.add(new Point3f((float) xDist, (float) (currentKnifeCentre.getY()+yDist), (float) -maxDist));
            } else {
                planeVertices.add(new Point3f((float) -xDist, (float) (currentKnifeCentre.getY()+yDist), (float) maxDist));
                planeVertices.add(new Point3f((float) xDist, (float) (currentKnifeCentre.getY()-yDist), (float) maxDist));
                planeVertices.add(new Point3f((float) -xDist, (float) (currentKnifeCentre.getY()+yDist), (float) -maxDist));
                planeVertices.add(new Point3f((float) xDist, (float) (currentKnifeCentre.getY()-yDist), (float) -maxDist));
            }
        }

        ArrayList<Point3f> triangles = new ArrayList<>();
        triangles.add(planeVertices.get(0));
        triangles.add(planeVertices.get(1));
        triangles.add(planeVertices.get(2));
        triangles.add(planeVertices.get(1));
        triangles.add(planeVertices.get(2));
        triangles.add(planeVertices.get(3));

        CustomTriangleMesh newMesh = new CustomTriangleMesh(triangles, new Color3f(1, 0.6f, 1), 0);

        Content meshContent = universe.addCustomMesh(newMesh, "CuttingPlane");
        meshContent.setLocked(true);
        meshContent.setVisible(true);
    }

    public void removeCuttingPlane() {
        universe.removeContent("CuttingPlane");
    }

    public void updateCut(double currentDepth) {

//        Update position of cutting plane
        double depthMicrotomeCoords = currentDepth + NSZero.getY();
        double yDistFromKnife = depthMicrotomeCoords - currentKnifeCentre.getY();

        Matrix4d translateCuttingPlane = new Matrix4d(1, 0, 0, 0,
                0, 1, 0, yDistFromKnife,
                0, 0, 1, 0,
                0, 0, 0, 1);

        universe.getContent("CuttingPlane").setTransform(new Transform3D(translateCuttingPlane));

//        Convert to microtome space coordinates - not adjusted for intial point == 0
        Point3d knifePoint = new Point3d(0, depthMicrotomeCoords, 0);

//        Convert knife plane to image coordinates
        Transform3D inverseBlockTransform = new Transform3D();
        inverseBlockTransform.invert(new Transform3D(currentBlockTransform));
        inverseBlockTransform.transform(knifePoint);
        Vector3d knifeNormal = new Vector3d();
        inverseBlockTransform.transform(currentKnifeNormal, knifeNormal);
        Vector3d edgeVector = new Vector3d();
        inverseBlockTransform.transform(currentEdgeVector, edgeVector);

        double[] knifePointDouble = {knifePoint.getX(), knifePoint.getY(), knifePoint.getZ()};
        double[] knifeNormalDouble = {knifeNormal.getX(), knifeNormal.getY(), knifeNormal.getZ()};
        double[] edgeVectorDouble = {edgeVector.getX(), edgeVector.getY(), edgeVector.getZ()};

        ArrayList<Vector3d> planeDefinition = planeManager.getPlaneDefinitionOfCurrentView();
        Vector3d currentPlaneNormal = planeDefinition.get(0);
        Vector3d currentPlanePoint = planeDefinition.get(1);

//        Check if already at that plane
        boolean normalsParallel = checkVectorsParallel(knifeNormal, currentPlaneNormal);
        double distanceToPlane = distanceFromPointToPlane(currentPlanePoint, knifeNormal, new Vector3d(knifePoint.getX(), knifePoint.getY(), knifePoint.getZ()));
//        System.out.println(Arrays.toString(knifePointDouble));
        if (distanceToPlane > 1E-10) {
//            Use point that is shortest parallel distance to current point, lets position be user defined and will just show progression of cut from there
            Vector3d currentViewCentreGlobal = new Vector3d(planeManager.getGlobalViewCentre());
            Vector3d knifePointV = new Vector3d(knifePoint.getX(), knifePoint.getY(), knifePoint.getZ());
            double distanceCurrentToFinal = distanceFromPointToPlane(currentViewCentreGlobal, knifeNormal, knifePointV);

//            Check unit normal points from the current view point to the plane
            Vector3d finalNormalCopy = new Vector3d(knifeNormal);
            finalNormalCopy.normalize();
            Vector3d pointToPlane = new Vector3d();
            pointToPlane.sub(knifePointV, currentViewCentreGlobal);
            if (pointToPlane.dot(finalNormalCopy) < 0) {
                finalNormalCopy.negate();
            }
            Vector3d toAdd = new Vector3d(finalNormalCopy.getX()*distanceCurrentToFinal, finalNormalCopy.getY()*distanceCurrentToFinal,
                    finalNormalCopy.getZ()*distanceCurrentToFinal);
            currentViewCentreGlobal.add(toAdd);
            double[] currentViewCentreDouble = {currentViewCentreGlobal.getX(), currentViewCentreGlobal.getY(), currentViewCentreGlobal.getZ()};
            moveToPosition(bdvStackSource, currentViewCentreDouble, 0);
        }
//        System.out.println(Arrays.toString(knifeNormalDouble));
        // TODO - broader check, and not rotated properly
        if (!normalsParallel) {
            levelCurrentViewNormalandHorizontal(bdvStackSource, knifeNormalDouble, edgeVectorDouble);
        }

    }
}
