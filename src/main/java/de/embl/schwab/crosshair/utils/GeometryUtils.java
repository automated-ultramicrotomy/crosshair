package de.embl.schwab.crosshair.utils;

import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.util.Precision;
import org.jogamp.vecmath.AxisAngle4d;
import org.jogamp.vecmath.Matrix4d;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3d;

import java.util.*;
import java.util.stream.DoubleStream;

import static de.embl.cba.bdv.utils.BdvUtils.*;
import static de.embl.schwab.crosshair.utils.Utils.findIndexOfMaxMin;
import static java.lang.Math.*;

public final class GeometryUtils {

    /**
     * Fit a plane to a list of 3D points
     * @param points A list of 3D points
     * @return The plane definition as [planeNormal, planePoint]. The plane point is the centroid of the plane.
     */
    public static ArrayList<Vector3d> fitPlaneToPoints(ArrayList<RealPoint> points) {
        // Solution as here: https://math.stackexchange.com/questions/99299/best-fitting-plane-given-a-set-of-points
        // good explanation of svds: https://en.wikipedia.org/wiki/Singular_value_decomposition

        // convert to a real matrix
        double [] [] pointArray = new double [points.size()][3];
        for (int i=0; i<points.size(); i++) {
            double[] position = new double[3];
            points.get(i).localize(position);

            pointArray[i] = position;
        }

        // Convert to real matrix as here: http://commons.apache.org/proper/commons-math/userguide/linear.html
        RealMatrix pointMatrix = MatrixUtils.createRealMatrix(pointArray);

        // Calculate centroid
        RealVector centroid = new ArrayRealVector(new double[] {0,0,0});
        for (int i=0; i<points.size(); i++) {
            centroid = centroid.add(pointMatrix.getRowVector(i));
        }
        centroid.mapDivideToSelf(points.size());

        // subtract centroid from every row
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
        // normalise just in case
        finalPlaneNormal.normalize();
        Vector3d finalPlanePoint = new Vector3d(centroid.toArray());
        ArrayList<Vector3d> result = new ArrayList<>();
        result.add(finalPlaneNormal);
        result.add(finalPlanePoint);

        return result;

    }

    /**
     * Get centroid of a list of 3D points
     * @param points list of 3D points
     * @return centroid
     */
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

    /**
     * Calculate triangles to use for mesh creation from a series of points.
     *
     * @param intersections list of points where the plane intersects with the image bounding box
     * @param planeNormal plane normal
     * @return List of triangles to use to create a mesh of this plane. Given as a list of points, where every 3 points
     * are the vertices of a triangle e.g.
     * [triangle-1-point-1, triangle-1-point-2, triangle-1-point-3,
     * triangle-2-point-1, triangle-2-point-2, triangle-2-point-3...]
     */
    public static ArrayList<Point3f> calculateTrianglesFromPoints(ArrayList<Vector3d> intersections, Vector3d planeNormal) {
        // TODO -maybe calculate plane normal directly from points? Avoids any issues with transformations...
        Vector3d centroid = getCentroid(intersections);
        Vector3d centroidToPoint = new Vector3d();
        centroidToPoint.sub(intersections.get(0), centroid);

        Double[] signedAngles = new Double[intersections.size()];
        // angle of point to itself is zero
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

    /**
     * Calculate signed angle (anticlockwise positive) between two vectors in radians. The two vectors are assumed to
     * lie on a plane, with the angle being given with respect to looking down the given plane normal.
     * @param vector1 vector 1
     * @param vector2 vector 2
     * @param planeNormal plane normal
     * @return signed angle in radians
     */
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

    /**
     * General equation for signed angle of rotation about a viewpoint vector to get from an initial plane to a
     * target plane.
     * Imagine the plane perpendicular to the viewpoint vector, intersecting with both the initial and target plane.
     * We then take these two lines of intersection and calculate the minimal angle between them, signed as +ve or -ve
     * depending on the direction of rotation from the initial intersection to the target intersection line.
     * @param view Viewpoint vector. Angles are relative to looking down this vector - anticlockwise is positive;
     *             clockwise is negative
     * @param targetNormal normal of target plane
     * @param initialNormal normal of initial plane
     * @return Signed angle of rotation
     */
    public static double rotationPlaneToPlane(Vector3d view, Vector3d targetNormal, Vector3d initialNormal) {
        // Line of intersection of target plane and plane perpendicular to viewpoint
        Vector3d intersectionTarget = new Vector3d();
        intersectionTarget.cross(targetNormal, view);

        // Line of intersection of initial plane and plane perpendicular to viewpoint
        Vector3d intersectionInitial = new Vector3d();
        intersectionInitial.cross(initialNormal, view);

        // Issue that as we don't know which direction the normals point in, towards or away, we don't
        // know which direction the intersections will point in
        // Here I force both vectors to point in the same general direction
        if (intersectionTarget.dot(intersectionInitial) < 0) {
            intersectionTarget.negate();
        }

        double angle = convertToDegrees(intersectionTarget.angle(intersectionInitial));

        // Now I figure out if the direction will be anticlockwise or clockwise (relative to looking down on
        // the viewpoint vector. Anticlockwise is positive; clockwise is negative
        Vector3d initialCrossTarget = new Vector3d();
        initialCrossTarget.cross(intersectionInitial, intersectionTarget);
        if (initialCrossTarget.dot(view) < 0) {
            angle = -angle;
        }

        return angle;
    }

    /**
     * Find the index of the point that has the maximum or minimum distance from a given plane. Distances are signed
     * based on a given vector determining the positive direction.
     * @param planePoint plane point
     * @param planeNormal plane normal
     * @param points list of points
     * @param positiveDirection vector pointing in the 'positive' direction
     * @param MinMax "max" or "min" - whether to return index of point with max or min distance from plane
     * @return the index of the point (in the given points list) with the min/max distance to the plane
     */
    public static int indexSignedMinMaxPointsToPlane(Vector3d planePoint, Vector3d planeNormal,
                                                      ArrayList<Vector3d> points, Vector3d positiveDirection,
                                                      String MinMax) {
        ArrayList<Double> allDists = new ArrayList<>();
        for (Vector3d point: points) {
            double distance = signedDistanceFromPointToPlane(point, planeNormal, planePoint, positiveDirection);
            allDists.add(distance);
        }

        return findIndexOfMaxMin(allDists, MinMax);

    }

    /**
     * Find the index of the point that has the maximum or minimum distance from a given plane. Distances are
     * absolute (no +ve / -ve signs).
     * @param planePoint plane point
     * @param planeNormal plane normal
     * @param points list of points
     * @param MinMax "max" or "min" - whether to return index of point with max or min distance from plane
     * @return the index of the point (in the given points list) with the min/max distance to the plane
     */
    public static int indexMinMaxPointsToPlane(Vector3d planePoint, Vector3d planeNormal, ArrayList<Vector3d> points,
                                               String MinMax) {
        ArrayList<Double> allDists = new ArrayList<>();
        for (Vector3d point: points) {
            double distance = distanceFromPointToPlane(planePoint, planeNormal, point);
            allDists.add(distance);
        }

        return findIndexOfMaxMin(allDists, MinMax);
    }

    /**
     * Calculate normal of plane from 3 points assumed to lie on it
     * @param points A list of three 3D points
     * @return The normal of the plane
     */
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

    /**
     * Calculate intersection points of a plane with a bounding box.
     * @param globalMin min of bounding box
     * @param globalMax max of bounding box
     * @param planeNormal plane normal
     * @param planePoint plane point
     * @return list of 3D intersection points
     */
    public static ArrayList<Vector3d> calculateIntersections(double[] globalMin, double[] globalMax,
                                                             Vector3d planeNormal, Vector3d planePoint) {
        ArrayList<Vector3d> boundingBoxPoints = new ArrayList<>();
        boundingBoxPoints.add(new Vector3d (globalMin[0], globalMin[1], globalMin[2]));
        boundingBoxPoints.add(new Vector3d (globalMin[0], globalMin[1], globalMax[2]));
        boundingBoxPoints.add(new Vector3d (globalMin[0], globalMax[1], globalMin[2]));
        boundingBoxPoints.add(new Vector3d (globalMin[0], globalMax[1], globalMax[2]));
        boundingBoxPoints.add(new Vector3d (globalMax[0], globalMin[1], globalMin[2]));
        boundingBoxPoints.add(new Vector3d (globalMax[0], globalMin[1], globalMax[2]));
        boundingBoxPoints.add(new Vector3d (globalMax[0], globalMax[1], globalMin[2]));
        boundingBoxPoints.add(new Vector3d (globalMax[0], globalMax[1], globalMax[2]));

        // enumerate all combos of two points on edges
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

        // check for intersection of plane with all points - if four intersect, return these as the four points
        // deals with case where plane is on the bounding box edges
        for (Vector3d[] v: boundingBoxEdges) {
            if (checkVectorLiesInPlane(v[0], v[1], planeNormal, planePoint)) {
                intersectionPoints.add(v[0]);
                intersectionPoints.add(v[1]);
                // parallel but doesn't lie in plane so no intersections
            } else if (checkVectorPlaneParallel(v[0], v[1], planeNormal)) {
                ;
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

    /**
     * Find point where a line intersects a given plane. Calculation from
     * https://stackoverflow.com/questions/5666222/3d-line-plane-intersection
     * @param point1 point at one end of line
     * @param point2 point at other end of line
     * @param planeNormal plane normal
     * @param planePoint plane point
     * @return Point of intersection
     */
    public static Vector3d calculateVectorPlaneIntersection(Vector3d point1, Vector3d point2, Vector3d planeNormal,
                                                            Vector3d planePoint) {
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

    public static boolean checkVectorsParallel(Vector3d vector1, Vector3d vector2) {
        double unsignedAngle = vector1.angle(vector2);
        if (unsignedAngle == 0 | unsignedAngle == PI) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean checkPointLiesInPlane(Vector3d point, Vector3d planeNormal, Vector3d planePoint) {
        Vector3d planeNormalCopy = new Vector3d(planeNormal);
        planeNormalCopy.normalize();

        // point-normal form of a plane
        double planeEquation = (planeNormalCopy.getX()*(point.getX() - planePoint.getX())) +
                (planeNormalCopy.getY()*(point.getY() - planePoint.getY())) +
                (planeNormalCopy.getZ()*(point.getZ() - planePoint.getZ()));

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

    /**
     * Calculate absolute distance from a point to a plane.
     * @param point 3D point
     * @param planeNormal plane normal
     * @param planePoint plane point
     * @return absolute distance
     */
    public static double distanceFromPointToPlane(Vector3d point, Vector3d planeNormal, Vector3d planePoint) {
        Vector3d planeNormalCopy = new Vector3d(planeNormal);
        planeNormalCopy.normalize(); // just in case

        Vector3d pointToPlaneVector = new Vector3d();
        pointToPlaneVector.sub(planePoint, point);

        return abs(pointToPlaneVector.dot(planeNormalCopy));
    }

    /**
     * Find closest point on plane (relative to a given point)
     * @param planeNormal plane normal
     * @param planePoint plane point
     * @param point point
     * @return Closest point on plane
     */
    public static Vector3d findClosestPointOnPlane(Vector3d planeNormal, Vector3d planePoint, Vector3d point) {
        Vector3d planeNormalCopy = new Vector3d(planeNormal);
        planeNormalCopy.normalize();

        double distancePointToPlane = GeometryUtils.distanceFromPointToPlane(point, planeNormalCopy, planePoint);

        // Check unit normal points from the point to the plane
        Vector3d pointToPlane = new Vector3d();
        pointToPlane.sub(planePoint, point);
        if (pointToPlane.dot(planeNormalCopy) < 0) {
            planeNormalCopy.negate();
        }

        Vector3d toAdd = new Vector3d(
                planeNormalCopy.getX()*distancePointToPlane,
                planeNormalCopy.getY()*distancePointToPlane,
                planeNormalCopy.getZ()*distancePointToPlane
        );

        Vector3d closestPoint = new Vector3d();
        closestPoint.add(point, toAdd);

        return closestPoint;
    }

    /**
     * Find signed distance from a point to a plane. The sign is based on a given vector determining the positive
     * direction.
     * @param point point
     * @param planeNormal plane normal
     * @param planePoint plane point
     * @param positiveDirection vector pointing in the 'positive' direction
     * @return Signed distance
     */
    public static double signedDistanceFromPointToPlane(Vector3d point, Vector3d planeNormal, Vector3d planePoint,
                                                        Vector3d positiveDirection) {
        Vector3d planeNormalCopy = new Vector3d(planeNormal);
        planeNormalCopy.normalize(); // just in case
        Vector3d positiveDirectionCopy = new Vector3d(positiveDirection);
        positiveDirectionCopy.normalize(); // just in case

        // force plane normal to be in same general direction as positive direction
        if (planeNormalCopy.dot(positiveDirectionCopy) < 0) {
            planeNormalCopy.negate();
        }

        double distance = distanceFromPointToPlane(point, planeNormal, planePoint);
        Vector3d nearestPointOnPlane = findClosestPointOnPlane(planeNormal, planePoint, point);
        Vector3d nearestPointOnPlaneToPoint = new Vector3d();
        nearestPointOnPlaneToPoint.sub(point, nearestPointOnPlane);

        // If vector from nearest point on plane to point is in the general direction of the plane normal = positive,
        // otherwise negative
        if ( nearestPointOnPlaneToPoint.dot(planeNormalCopy) < 0) {
            distance *= -1;
        }

        return distance;
    }

    /**
     * Check a vector (between two points) lies within a given plane.
     * @param point1 first vector point
     * @param point2 second vector point
     * @param planeNormal plane normal
     * @param planePoint plane point
     * @return whether the vector lies within the plane
     */
    public static boolean checkVectorLiesInPlane(Vector3d point1, Vector3d point2, Vector3d planeNormal, Vector3d planePoint) {
        // vector between provided points
        boolean vectorPlaneParallel = checkVectorPlaneParallel(point1, point2, planeNormal);
        boolean pointPlaneIntersection = checkPointPlaneIntersection(point1, planeNormal, planePoint);

        return vectorPlaneParallel && pointPlaneIntersection;
    }

    /**
     * Check a vector (between two points) and a plane are parallel
     * @param point1 first vector point
     * @param point2 second vector point
     * @param planeNormal plane normal
     * @return whether the vector and plane are parallel
     */
    public static boolean checkVectorPlaneParallel(Vector3d point1, Vector3d point2, Vector3d planeNormal) {
        // vector between provided points
        Vector3d pointToPoint = new Vector3d();
        pointToPoint.sub(point1, point2);

        double dotProduct = pointToPoint.dot(planeNormal);
        return dotProduct == 0;
    }

    /**
     * Check a point intersects a given plane.
     * @param point point
     * @param planeNormal plane normal
     * @param planePoint plane point
     * @return Whether the point intersects the plane
     */
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

    /**
     * Check two 3D points are at the same location
     * @param point1 point 1
     * @param point2 point 2
     * @return whether the points are at the same location
     */
    public static boolean checkTwoRealPointsSameLocation(RealPoint point1, RealPoint point2) {
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

    /**
     * Check a given vector is horizontal in the current bigdataviewer view. For example, I may want a vector running
     * from left to right along the knife edge to be horizontal in bigdataviewer, so everything is oriented correctly.
     * @param bdv BigDataViewer window
     * @param vector 3D vector
     * @return whether the vector is horizontal
     */
    public static boolean checkVectorHorizontalInCurrentView(Bdv bdv, double[] vector) {
        AffineTransform3D currentViewerTransform = new AffineTransform3D();
        bdv.getBdvHandle().getViewerPanel().state().getViewerTransform( currentViewerTransform );

        // Convert everything to viewer coordinates
        double[] qCurrentRotation = new double[ 4 ];
        Affine3DHelpers.extractRotation( currentViewerTransform, qCurrentRotation );
        final AffineTransform3D currentRotation = quaternionToAffineTransform3D( qCurrentRotation );

        double[] vectorInViewerSystem = new double[3];
        currentRotation.apply( vector, vectorInViewerSystem);

        return checkVectorsParallel(new Vector3d(vectorInViewerSystem), new Vector3d(1, 0, 0));
    }

    /**
     * Adjust the current bigdataviewer view, so that it is perpendicular to the given normal vector, and
     * the given 'target horizontal vector' appears horizontal in this 2D view.
     * @param bdv BigDataViewer window
     * @param targetNormalVector target normal vector
     * @param targetHorizontalVector target horizontal vector
     */
    public static void levelCurrentViewNormalandHorizontal( Bdv bdv, double[] targetNormalVector,
                                                            double[] targetHorizontalVector) {

        AffineTransform3D currentViewerTransform = new AffineTransform3D();
        bdv.getBdvHandle().getViewerPanel().state().getViewerTransform( currentViewerTransform );

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
        translateCenterToOrigin.translate( DoubleStream.of( getBdvWindowCentre( bdv )).map(x -> -x ).toArray() );

        final AffineTransform3D translateCenterBack = new AffineTransform3D();
        translateCenterBack.translate( getBdvWindowCentre( bdv ) );

        ArrayList< AffineTransform3D > viewerTransforms = new ArrayList<>(  );

        viewerTransforms.add( currentViewerTransform.copy()
                .preConcatenate( translateCenterToOrigin )
                .preConcatenate( rotation )
                .preConcatenate( translateCenterBack ) );

        changeBdvViewerTransform( bdv, viewerTransforms, 300 );
    }

    /**
     * Make matrix for a rotation and a translation
     * @param angleDegrees angle of rotation
     * @param axis axis of rotation
     * @param rotationCentre centre of rotation
     * @param translation translation vector
     * @return matrix representing a rotation and translation
     */
    public static Matrix4d makeMatrix(double angleDegrees, Vector3d axis, Vector3d rotationCentre, Vector3d translation) {
        double angleRad = angleDegrees * Math.PI / 180;
        Matrix4d m = new Matrix4d();
        compose(new AxisAngle4d(axis, angleRad), rotationCentre, translation, m);
        return m;
    }

    /**
     * Make matrix combining rotation and translation
     * Based on compose from InteractiveTransformDialog/Executer in 3D viewer:
     * https://github.com/fiji/3D_Viewer/blob/master/src/main/java/ij3d/gui/InteractiveTransformDialog.java#L222
     * https://github.com/fiji/3D_Viewer/blob/master/src/main/java/ij3d/Executer.java
     * @param rot axis angle rotation
     * @param origin centre of rotation
     * @param translation translation vector
     * @param ret matrix to write result into
     */
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

    /**
     * Make matrix combining rotation and translation
     * Based on compose from InteractiveTransformDialog/Executer in 3D viewer:
     * https://github.com/fiji/3D_Viewer/blob/master/src/main/java/ij3d/gui/InteractiveTransformDialog.java#L222
     * https://github.com/fiji/3D_Viewer/blob/master/src/main/java/ij3d/Executer.java
     * @param rot rotation matrix
     * @param origin centre of rotation
     * @param translation translation vector
     * @param ret matrix to write result into
     */
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

    /**
     * Give one possible perpendicular vector (to the given vector). Based on
     * https://math.stackexchange.com/questions/133177/finding-a-unit-vector-perpendicular-to-another-vector/413235
     * @param vector vector
     * @return A possible perpendicular vector (there are many!)
     */
    public static double[] findPerpendicularVector( double[] vector ) {
        double epsilon = 1E-9;
        int nonZeroIndex = 0;
        for ( int i=0; i<vector.length; i++) {
            if ( vector[i] != 0 ) {
                nonZeroIndex = i;
                break;
            }
        }

        int otherIndex;
        if ( nonZeroIndex == 0 ) {
            otherIndex = 1;
        } else {
            otherIndex = 0;
        }

        double[] perpendicularVector = new double[3];
        perpendicularVector[otherIndex] = vector[nonZeroIndex];
        perpendicularVector[nonZeroIndex] = -vector[otherIndex];

        LinAlgHelpers.normalize(perpendicularVector);
        if ( !Precision.equals( LinAlgHelpers.dot(vector, perpendicularVector), 0, epsilon ) ) {
            // vectors should be perpendicular, if not throw error
            throw new UnsupportedOperationException();
        }

        return perpendicularVector;
    }

}
