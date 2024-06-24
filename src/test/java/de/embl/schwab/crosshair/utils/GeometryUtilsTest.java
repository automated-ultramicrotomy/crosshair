package de.embl.schwab.crosshair.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.scijava.vecmath.Vector3d;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class GeometryUtilsTest {

    /**
     * Different plane orientations to test... (same as in PlaneCreatorTest)
     * @return stream of point, normal
     */
    static Stream<Arguments> planeArgumentProvider() {
        return Stream.of(
                // Point at centre of image content, plane aligned to z axis
                arguments(
                        new Vector3d(607.3829956054688, 607.3829956054688, 309.4661560058594),
                        new Vector3d(0, 0, 1)),
                // Values from a randomly aligned plane created by tracking in the viewer
                arguments(
                        new Vector3d(-188.47561306126704, 15.353222856645068, 621.5211240735744),
                        new Vector3d(0.20791169081775954, 0.08525118065879457, 0.9744254538021788)),
                // Values from a randomly aligned plane created by tracking in the viewer
                arguments(
                        new Vector3d(0.6618597935512298, 37.48411448950936, 984.2992808834338),
                        new Vector3d(0.6156614753256584, 0.05496885144405574, 0.7860911990162179))
        );
    }

    /**
     * Test calculation of intersection points between a plane and the min/max of the image content. This function is
     * used inside the PlaneCreator, to generate points to produce the plane mesh. Assertions are similar to
     * PlaneCreatorTest.assertionsFor3DMesh but use a much lower threshold for being on the plane (as this function
     * directly uses double values, while the mesh content stores points as lower precision floats)
     */
    @ParameterizedTest
    @MethodSource("planeArgumentProvider")
    void calculateIntersections(Vector3d point, Vector3d normal) {
        // min and max of example block
        double[] min = new double[]{0.0, 0.0, 0.0};
        double[] max = new double[]{1214.7659912109375, 1214.7659912109375, 618.9323120117188};

        // Check each intersection is within 1E-12 of given plane, and within the bounds of min-max
        List<Vector3d> intersections = GeometryUtils.calculateIntersections(min, max, normal, point);
        for (Vector3d intersection: intersections) {
            assertTrue(GeometryUtils.distanceFromPointToPlane(intersection, normal, point) < 1E-12);
            assertTrue(intersection.x >= min[0] && intersection.x <= max[0]);
            assertTrue(intersection.y >= min[1] && intersection.y <= max[1]);
            assertTrue(intersection.z >= min[2] && intersection.z <= max[2]);
        }
    }
}