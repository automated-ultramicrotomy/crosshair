package de.embl.cba.crosshair.utils;

import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import bdv.viewer.animate.SimilarityTransformAnimator;
import de.embl.cba.bdv.utils.BdvUtils;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.*;
import org.scijava.vecmath.AxisAngle4d;
import org.scijava.vecmath.Matrix4d;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3d;

import java.util.*;
import java.util.stream.DoubleStream;

import static de.embl.cba.bdv.utils.BdvUtils.*;
import static java.lang.Math.*;

public final class GeometryUtils {

    // from MOBIE
    public static void moveToPosition(Bdv bdv, double[] xyz, long durationMillis )
    {

        final AffineTransform3D currentViewerTransform = new AffineTransform3D();
        bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentViewerTransform );

        AffineTransform3D newViewerTransform = currentViewerTransform.copy();

        // ViewerTransform
        // applyInverse: coordinates in viewer => coordinates in image
        // apply: coordinates in image => coordinates in viewer

        final double[] locationOfTargetCoordinatesInCurrentViewer = new double[ 3 ];
        currentViewerTransform.apply( xyz, locationOfTargetCoordinatesInCurrentViewer );

        for ( int d = 0; d < 3; d++ )
        {
            locationOfTargetCoordinatesInCurrentViewer[ d ] *= -1;
        }

        newViewerTransform.translate( locationOfTargetCoordinatesInCurrentViewer );

        newViewerTransform.translate( getBdvWindowCenter( bdv ) );

        if ( durationMillis <= 0 )
        {
            bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( newViewerTransform );
            return;
        }
        else
        {
            final SimilarityTransformAnimator similarityTransformAnimator =
                    new SimilarityTransformAnimator(
                            currentViewerTransform,
                            newViewerTransform,
                            0,
                            0,
                            durationMillis );

            bdv.getBdvHandle().getViewerPanel().setTransformAnimator( similarityTransformAnimator );
            bdv.getBdvHandle().getViewerPanel().transformChanged( currentViewerTransform );
        }
    }


    // from MOBIE
    public static void levelCurrentView( Bdv bdv, double[] targetNormalVector )
    {

        double[] currentNormalVector = BdvUtils.getCurrentViewNormalVector( bdv );

        AffineTransform3D currentViewerTransform = new AffineTransform3D();
        bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentViewerTransform );

        LinAlgHelpers.normalize( targetNormalVector ); // just to be sure.

        // determine rotation axis
        double[] rotationAxis = new double[ 3 ];
        LinAlgHelpers.cross( currentNormalVector, targetNormalVector, rotationAxis );
        if ( LinAlgHelpers.length( rotationAxis ) > 0 ) LinAlgHelpers.normalize( rotationAxis );

        // The rotation axis is in the coordinate system of the original data set => transform to viewer coordinate system
        double[] qCurrentRotation = new double[ 4 ];
        Affine3DHelpers.extractRotation( currentViewerTransform, qCurrentRotation );
        final AffineTransform3D currentRotation = quaternionToAffineTransform3D( qCurrentRotation );

        double[] rotationAxisInViewerSystem = new double[ 3 ];
        currentRotation.apply( rotationAxis, rotationAxisInViewerSystem );

        // determine rotation angle
        double angle = - Math.acos( LinAlgHelpers.dot( currentNormalVector, targetNormalVector ) );

        // construct rotation of angle around axis
        double[] rotationQuaternion = new double[ 4 ];
        LinAlgHelpers.quaternionFromAngleAxis( rotationAxisInViewerSystem, angle, rotationQuaternion );
        final AffineTransform3D rotation = quaternionToAffineTransform3D( rotationQuaternion );

        // apply transformation (rotating around current viewer centre position)
        final AffineTransform3D translateCenterToOrigin = new AffineTransform3D();
        translateCenterToOrigin.translate( DoubleStream.of( getBdvWindowCenter( bdv )).map(x -> -x ).toArray() );

        final AffineTransform3D translateCenterBack = new AffineTransform3D();
        translateCenterBack.translate( getBdvWindowCenter( bdv ) );

        ArrayList< AffineTransform3D > viewerTransforms = new ArrayList<>(  );

        viewerTransforms.add( currentViewerTransform.copy()
                .preConcatenate( translateCenterToOrigin )
                .preConcatenate( rotation )
                .preConcatenate( translateCenterBack ) );

        changeBdvViewerTransform( bdv, viewerTransforms, 500 );

    }



    public static ArrayList<Vector3d> fitPlaneToPoints(ArrayList<RealPoint> points) {
        // Solution as here: https://math.stackexchange.com/questions/99299/best-fitting-plane-given-a-set-of-points
        // good explanation of svds: https://en.wikipedia.org/wiki/Singular_value_decomposition
//        convert to a real matrix
        double [] [] pointArray = new double [points.size()][3];
        for (int i=0; i<points.size(); i++) {
            double[] position = new double[3];
            points.get(i).localize(position);

            pointArray[i] = position;
        }
        System.out.println(pointArray.toString());



//        Convert to real matrix as here: http://commons.apache.org/proper/commons-math/userguide/linear.html
        RealMatrix pointMatrix = MatrixUtils.createRealMatrix(pointArray);

        //        Calculate centroid
        RealVector centroid = new ArrayRealVector(new double[] {0,0,0});
        for (int i=0; i<points.size(); i++) {
            centroid = centroid.add(pointMatrix.getRowVector(i));
        }
        centroid.mapDivideToSelf(points.size());

        //subtract centroid from every row
        for (int i=0; i<points.size(); i++) {
            RealVector row = pointMatrix.getRowVector(i);
            pointMatrix.setRowVector(i, row.subtract(centroid));
        }

        RealMatrix transposedMatrix = pointMatrix.transpose();
        SingularValueDecomposition svd = new SingularValueDecomposition(transposedMatrix);
        double[] singularValues = svd.getSingularValues();

        // get index of minimum singular value
        Double minValue = null;
        int index = 0;
        for (int i=0; i<singularValues.length; i++) {
            if (minValue == null) {
                minValue = singularValues[i];
                index = i;
            } else {
                if (singularValues[i] < minValue) {
                    minValue = singularValues[i];
                    index = i;
                }
            }
        }

        // get corresponding left singular vector
        RealVector planeNormal = svd.getU().getColumnVector(index);

        // return plane normal and centroid as vector 3d
        Vector3d finalPlaneNormal = new Vector3d(planeNormal.toArray());
        // normalie just in case
        finalPlaneNormal.normalize();
        Vector3d finalPlanePoint = new Vector3d(centroid.toArray());
        ArrayList<Vector3d> result = new ArrayList<>();
        result.add(finalPlaneNormal);
        result.add(finalPlanePoint);

        return result;

    }

    public static Vector3d getCentroid(ArrayList<Vector3d> points) {
        Vector3d centroid = new Vector3d(new double[] {0,0,0});
        for (Vector3d v : points) {
            centroid.add(v);
        }
        centroid.setX(centroid.getX()/points.size());
        centroid.setY(centroid.getY()/points.size());
        centroid.setZ(centroid.getZ()/points.size());
        return centroid;
    }

    public static ArrayList<Point3f> calculateTrianglesFromPoints(ArrayList<Vector3d> intersections, Vector3d planeNormal) {
        // TODO -maybe calculate plane normal directly from points? Avoids any issues with transformations...
        Vector3d centroid = getCentroid(intersections);
        Vector3d centroidToPoint = new Vector3d();
        centroidToPoint.sub(intersections.get(0), centroid);

        Double[] signedAngles = new Double[intersections.size()];
//		angle of point to itself is zero
        signedAngles[0] = 0.0;
        for (int i=1; i<intersections.size(); i++) {
            Vector3d centroidToCurrentPoint = new Vector3d();
            centroidToCurrentPoint.sub(intersections.get(i), centroid);
            signedAngles[i] = calculateSignedAngle(centroidToPoint, centroidToCurrentPoint, planeNormal);
        }

        // convert all intersections to point3f
        ArrayList<Point3f> intersections3F = new ArrayList<>();
        for (Vector3d d : intersections) {
            intersections3F.add(vector3DToPoint3F(d));
        }

        // order intersections_without_root with respect ot the signed angles
        ArrayList<PointAngle> pointsAndAngles = new ArrayList<>();
        for (int i = 0; i<intersections3F.size(); i++) {
            pointsAndAngles.add(new PointAngle(intersections3F.get(i), signedAngles[i]));
        }

        Collections.sort(pointsAndAngles, (p1, p2) -> p1.getAngle().compareTo(p2.getAngle()));

        ArrayList<Point3f> triangles = new ArrayList<>();
        for (int i = 1; i<pointsAndAngles.size() - 1; i++) {
            triangles.add(pointsAndAngles.get(0).getPoint());
            triangles.add(pointsAndAngles.get(i).getPoint());
            triangles.add(pointsAndAngles.get(i + 1).getPoint());
        }

        return triangles;
    }

    public static Point3f vector3DToPoint3F(Vector3d vector) {
        Point3f newPoint = new Point3f((float) vector.getX(), (float) vector.getY(), (float) vector.getZ());
        return newPoint;
    }

//    Note angle in radians
    public static double calculateSignedAngle(Vector3d vector1, Vector3d vector2, Vector3d planeNormal) {
        double unsignedAngle = vector1.angle(vector2);
        Vector3d crossVector1Vector2 = new Vector3d();
        crossVector1Vector2.cross(vector1, vector2);

        double sign = planeNormal.dot(crossVector1Vector2);
        if (sign < 0) {
            return -unsignedAngle;
        } else {
            return unsignedAngle;
        }
    }

//    Signed angle between two vectors (disregarding the orientations of the vectors) - so
//    signed angle from initial line to target line. From a viewpoint vector perpendicular to both.
    public static double calculateSignedAngleLines (Vector3d view, Vector3d targetVector, Vector3d initialVector) {
//        Make vectors point in same general direction
        Vector3d targetVectorCopy = new Vector3d(targetVector);
        if (targetVectorCopy.dot(initialVector) < 0) {
            targetVectorCopy.negate();
        }
//TODO - convert to degrees?
        double angle = targetVector.angle(initialVector);

//        Now I figure out if the direction will be anticlockwise or clockwise (relative to looking down on
//        the viewpoint vector. Anticlockwise is positive; clockwise is negative
        Vector3d initialCrossTarget = new Vector3d();
        initialCrossTarget.cross(initialVector, targetVector);
        if (initialCrossTarget.dot(view) < 0) {
            angle = -angle;
        }

        return angle;

    }

//    General eq for signed angle of rotation about a viewpoint vector, to get from an initial plane
//    to a target plane.
    public static double rotationPlaneToPlane (Vector3d view, Vector3d targetNormal, Vector3d initialNormal) {
//         Line of intersection of target plane and plane perpendicular to viewpoint
        Vector3d intersectionTarget = new Vector3d();
        intersectionTarget.cross(targetNormal, view);

//        Line of intersection of initial plane and plane perpendicular to viewpoint
        Vector3d intersectionInitial = new Vector3d();
        intersectionInitial.cross(initialNormal, view);

//        Issue that as we don't know which direction the normals point in, towards or away, we don't
//        know which direction the intersections will point in
//        Here I force both vectors to point in the same general direction
        if (intersectionTarget.dot(intersectionInitial) < 0) {
            intersectionTarget.negate();
        }

        double angle = convertToDegrees(intersectionTarget.angle(intersectionInitial));

//        Now I figure out if the direction will be anticlockwise or clockwise (relative to looking down on
//        the viewpoint vector. Anticlockwise is positive; clockwise is negative
        Vector3d initialCrossTarget = new Vector3d();
        initialCrossTarget.cross(intersectionInitial, intersectionTarget);
        if (initialCrossTarget.dot(view) < 0) {
            angle = -angle;
        }

        return angle;

    }

    public static int indexMinMaxPointsToPlane (Vector3d planePoint, Vector3d planeNormal, ArrayList<Vector3d> points, String MinMax) {
        Vector3d planeNormalCopy = new Vector3d(planeNormal);
        planeNormalCopy.normalize(); // just in case

        ArrayList<Double> allDists = new ArrayList<>();
        for (Vector3d point: points) {
            double distance = distanceFromPointToPlane(planePoint, planeNormalCopy, point);
            allDists.add(distance);
        }

        double chosenDist = 0;
        if (MinMax.equals("max")) {
            chosenDist = Collections.max(allDists);
        } else if (MinMax.equals("min")) {
            chosenDist = Collections.min(allDists);
        }

        int result = 0;
        for (int i=0; i<allDists.size(); i++) {
            if (allDists.get(i) == chosenDist) {
                result = i;
                break;
            }
        }
        return result;
    }

    public static Vector3d calculateNormalFromPoints(ArrayList<double[]> points) {
        double[] pointA = points.get(0);
        double[] pointB = points.get(1);
        double[] pointC = points.get(2);

        double[] vector1 = new double[3];
        double[] vector2 = new double[3];

        for ( int i = 0; i < 3; i++ ) {
            vector1[i] = pointA[i] - pointB[i];
            vector2[i] = pointC[i] - pointB[i];
        }

        Vector3d normal = new Vector3d();
        normal.cross(new Vector3d(vector1), new Vector3d(vector2));
        normal.normalize();

        return normal;
    }

    public static ArrayList<Vector3d> calculateIntersections(double[] globalMin, double[] globalMax, Vector3d planeNormal, Vector3d planePoint) {
        ArrayList<Vector3d> boundingBoxPoints = new ArrayList<>();
        boundingBoxPoints.add(new Vector3d (globalMin[0], globalMin[1], globalMin[2]));
        boundingBoxPoints.add(new Vector3d (globalMin[0], globalMin[1], globalMax[2]));
        boundingBoxPoints.add(new Vector3d (globalMin[0], globalMax[1], globalMin[2]));
        boundingBoxPoints.add(new Vector3d (globalMin[0], globalMax[1], globalMax[2]));
        boundingBoxPoints.add(new Vector3d (globalMax[0], globalMin[1], globalMin[2]));
        boundingBoxPoints.add(new Vector3d (globalMax[0], globalMin[1], globalMax[2]));
        boundingBoxPoints.add(new Vector3d (globalMax[0], globalMax[1], globalMin[2]));
        boundingBoxPoints.add(new Vector3d (globalMax[0], globalMax[1], globalMax[2]));

        //enumerate all combos of two points on edges
        ArrayList<Vector3d[]> boundingBoxEdges = new ArrayList<>();
        boundingBoxEdges.add(new Vector3d[] {boundingBoxPoints.get(0), boundingBoxPoints.get(1)});
        boundingBoxEdges.add(new Vector3d[] {boundingBoxPoints.get(0), boundingBoxPoints.get(4)});
        boundingBoxEdges.add(new Vector3d[] {boundingBoxPoints.get(1), boundingBoxPoints.get(5)});
        boundingBoxEdges.add(new Vector3d[] {boundingBoxPoints.get(4), boundingBoxPoints.get(5)});
        boundingBoxEdges.add(new Vector3d[] {boundingBoxPoints.get(7), boundingBoxPoints.get(5)});
        boundingBoxEdges.add(new Vector3d[] {boundingBoxPoints.get(3), boundingBoxPoints.get(7)});
        boundingBoxEdges.add(new Vector3d[] {boundingBoxPoints.get(7), boundingBoxPoints.get(6)});
        boundingBoxEdges.add(new Vector3d[] {boundingBoxPoints.get(6), boundingBoxPoints.get(4)});
        boundingBoxEdges.add(new Vector3d[] {boundingBoxPoints.get(2), boundingBoxPoints.get(0)});
        boundingBoxEdges.add(new Vector3d[] {boundingBoxPoints.get(2), boundingBoxPoints.get(6)});
        boundingBoxEdges.add(new Vector3d[] {boundingBoxPoints.get(2), boundingBoxPoints.get(3)});
        boundingBoxEdges.add(new Vector3d[] {boundingBoxPoints.get(1), boundingBoxPoints.get(3)});

        ArrayList<Vector3d> intersectionPoints = new ArrayList<>();

        // check for intersection of plane with all points - if four intersect, return these as teh four points
        // deals with case where plane is on the bounding box edges
        for (Vector3d[] v: boundingBoxEdges) {
            if (checkVectorLiesInPlane(v[0], v[1], planeNormal, planePoint)) {
                intersectionPoints.add(v[0]);
                intersectionPoints.add(v[1]);
                continue;
                // parallel but doesn't lie in plane so no intersections
            } else if (checkVectorPlaneParallel(v[0], v[1], planeNormal)) {
                continue;
            } else {
                Vector3d intersection = calculateVectorPlaneIntersection(v[0], v[1], planeNormal, planePoint);
                if (intersection.length() > 0) {
                    intersectionPoints.add(intersection);
                }
            }
        }

        // get rid of any repeat points
        Set<Vector3d> set = new HashSet<>(intersectionPoints);
        intersectionPoints.clear();
        intersectionPoints.addAll(set);

        return intersectionPoints;

    }
    //	https://stackoverflow.com/questions/5666222/3d-line-plane-intersection
    public static Vector3d calculateVectorPlaneIntersection(Vector3d point1, Vector3d point2, Vector3d planeNormal, Vector3d planePoint) {
        Vector3d pointToPoint = new Vector3d();
        pointToPoint.sub(point2, point1);
        double dotProductVectorPlaneNormal = pointToPoint.dot(planeNormal);

        Vector3d planeToPointVector = new Vector3d();
        planeToPointVector.sub(point1, planePoint);
        double dotProductPlaneToPointPlaneNormal = planeToPointVector.dot(planeNormal);
        double factor = -dotProductPlaneToPointPlaneNormal / dotProductVectorPlaneNormal;

        Vector3d result = new Vector3d();

        if (factor < 0 || factor > 1) {
            return result;
        }

        pointToPoint.setX(pointToPoint.getX()*factor);
        pointToPoint.setY(pointToPoint.getY()*factor);
        pointToPoint.setZ(pointToPoint.getZ()*factor);
        result.add(point1, pointToPoint);
        return result;
    }

    public static boolean checkVectorsParallel (Vector3d vector1, Vector3d vector2) {
        double unsignedAngle = vector1.angle(vector2);
        if (unsignedAngle == 0 | unsignedAngle == PI) {
            System.out.println("true");
            return true;
        } else {
            System.out.println("false");
            return false;
        }
    }

    public static boolean checkPointLiesInPlane (Vector3d point, Vector3d planeNormal, Vector3d planePoint) {
        Vector3d planeNormalCopy = new Vector3d(planeNormal);
        planeNormalCopy.normalize();

        // point-normal form of a plane
        double planeEquation = (planeNormalCopy.getX()*(point.getX() - planePoint.getX())) +
                (planeNormalCopy.getY()*(point.getY() - planePoint.getY())) +
                (planeNormalCopy.getZ()*(point.getZ() - planePoint.getZ()));
        System.out.println(planeEquation);

        // ideally this should be 0 to lie on plane, I set to a very small epsilon here to give wriggle room
        // for precision errors in the doubles
        if (abs(planeEquation) < 1E-13) {
            return true;
        } else {
            return false;
        }

    }

    public static double convertToRadians (double angleDegrees) {
        return angleDegrees * (PI / 180);
    }

    public static double convertToDegrees (double angleRadians) {
        return angleRadians * (180 / PI);
    }

    public static double distanceFromPointToPlane (Vector3d point, Vector3d planeNormal, Vector3d planePoint) {
        Vector3d planeNormalCopy = new Vector3d(planeNormal);
        planeNormalCopy.normalize(); // just in case

        Vector3d pointToPlaneVector = new Vector3d();
        pointToPlaneVector.sub(planePoint, point);

        return abs(pointToPlaneVector.dot(planeNormalCopy));
    }

    public static boolean checkVectorLiesInPlane(Vector3d point1, Vector3d point2, Vector3d planeNormal, Vector3d planePoint) {
        // vector between provided points
        boolean vectorPlaneParallel = checkVectorPlaneParallel(point1, point2, planeNormal);
        boolean pointPlaneIntersection = checkPointPlaneIntersection(point1, planeNormal, planePoint);

        return vectorPlaneParallel && pointPlaneIntersection;
    }

    public static boolean checkVectorPlaneParallel(Vector3d point1, Vector3d point2, Vector3d planeNormal) {
        // vector between provided points
        Vector3d pointToPoint = new Vector3d();
        pointToPoint.sub(point1, point2);

        double dotProduct = pointToPoint.dot(planeNormal);
        return dotProduct == 0;
    }

    public static boolean checkPointPlaneIntersection(Vector3d point, Vector3d planeNormal, Vector3d planePoint) {
        Vector3d pointToPlaneVector = new Vector3d();
        pointToPlaneVector.sub(planePoint, point);

        double dotProduct = pointToPlaneVector.dot(planeNormal);
        return dotProduct == 0;
    }

    public static double distanceBetweenPoints(double[] point1, double[] point2) {
        double sum = 0;
        for ( int j = 0; j < 3; j++ )
        {
            double diff =  point1[j] - point2[j];
            sum += diff*diff;
        }
        return sqrt(sum);
    }

    public static boolean checkTwoRealPointsSameLocation (RealPoint point1, RealPoint point2) {
        double[] point1Position = new double[3];
        point1.localize(point1Position);
        double[] point2Position = new double[3];
        point2.localize(point2Position);
        if (Arrays.equals(point1Position, point2Position)) {
            return true;
        } else {
            return false;
        }
    }

    public static void levelCurrentViewNormalandHorizontal( Bdv bdv, double[] targetNormalVector, double[] targetHorizontalVector)
    {

        AffineTransform3D currentViewerTransform = new AffineTransform3D();
        bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentViewerTransform );

        LinAlgHelpers.normalize( targetNormalVector ); // just to be sure.

        // Convert everything to viewer coordinates
        double[] qCurrentRotation = new double[ 4 ];
        Affine3DHelpers.extractRotation( currentViewerTransform, qCurrentRotation );
        final AffineTransform3D currentRotation = quaternionToAffineTransform3D( qCurrentRotation );

        double[] targetNormalInViewerSystem = new double[3];
        currentRotation.apply( targetNormalVector, targetNormalInViewerSystem);

        double[] targetHorizontalVectorInViewerSystem = new double[3];
        currentRotation.apply( targetHorizontalVector, targetHorizontalVectorInViewerSystem);

        // Rotation to bring target normal and horizontal, to be viewer normal and horizontal
        Rotation endRotation = new Rotation(new Vector3D(targetNormalInViewerSystem),
                new Vector3D(targetHorizontalVectorInViewerSystem),
                new Vector3D(0,0,-1),
                new Vector3D(1, 0, 0));

        final AffineTransform3D rotation = matrixAsAffineTransform3D(endRotation.getMatrix());

        // apply transformation (rotating around current viewer centre position)
        final AffineTransform3D translateCenterToOrigin = new AffineTransform3D();
        translateCenterToOrigin.translate( DoubleStream.of( getBdvWindowCenter( bdv )).map(x -> -x ).toArray() );

        final AffineTransform3D translateCenterBack = new AffineTransform3D();
        translateCenterBack.translate( getBdvWindowCenter( bdv ) );

        ArrayList< AffineTransform3D > viewerTransforms = new ArrayList<>(  );

        viewerTransforms.add( currentViewerTransform.copy()
                .preConcatenate( translateCenterToOrigin )
                .preConcatenate( rotation )
                .preConcatenate( translateCenterBack ) );

        changeBdvViewerTransform( bdv, viewerTransforms, 300 );

    }

    //        The two methods below are adapted from the imagej 3d viewer
    //        Interactive transform setter in 3d viewer: https://github.com/fiji/3D_Viewer/blob/master/src/main/java/ij3d/gui/InteractiveTransformDialog.java
    //        setting of transform: https://github.com/fiji/3D_Viewer/blob/ed05e4b2275ad6ad7c94b0e22f4789ebd3472f4d/src/main/java/ij3d/Executer.java
    public static Matrix4d makeMatrix(double angleDegrees, Vector3d axis, Vector3d rotationCentre, Vector3d translation) {
        double angleRad = angleDegrees * Math.PI / 180;
        Matrix4d m = new Matrix4d();
        compose(new AxisAngle4d(axis, angleRad), rotationCentre, translation, m);
        return m;
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

}
