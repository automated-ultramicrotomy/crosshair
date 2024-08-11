package de.embl.schwab.crosshair.microtome;

import de.embl.schwab.crosshair.points.VertexPoint;
import org.junit.jupiter.params.provider.Arguments;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Point3d;

import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;


public class MicrotomeManagerTestProviders {

    /**
     * Different initial knife / tilt angles to test initialising microtome mode with...
     * @return stream of initial knife angle, initial tilt angle, expected image content translation,
     * expected image content rotation, expected target plane translation, expected target plane rotation,
     * expected block plane translation, expected block plane rotation.
     *
     * All expected values were read from the debugger after setting the initial knife/tilt angle.
     */
    static Stream<Arguments> initialAngleProvider() {
        return Stream.of(
                arguments(
                        10,
                        10,
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, -672.8691470692755,
                                0.0, 1.0, 0.0, -296.08032474502943,
                                0.0, 0.0, 1.0, 37.176310308519305,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                0.9553266167640686, 0.047536421567201614, 0.2917042672634125, -64.3711034129874,
                                0.2955193519592285, -0.16834300756454468, -0.9403876662254333, 753.557969501444,
                                0.004403707571327686, 0.9845815896987915, -0.17487049102783203, -373.3532778222293,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, -615.003516242835,
                                0.0, 1.0, 0.0, -461.1177917505993,
                                0.0, 0.0, 1.0, -220.8019322290777,
                                0.0, 0.0, 0.0, 1.0

                        }),
                        new Transform3D(new double[]{
                                0.9553266167640686, 0.047536421567201614, 0.2917042672634125, -122.23673423942796,
                                0.2955193519592285, -0.16834300756454468, -0.9403876662254333, 918.5954365070138,
                                0.004403707571327686, 0.9845815896987915, -0.17487049102783203, -115.3750352846323,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, -590.4076868508683,
                                0.0, 1.0, 0.0, -540.4091055794233,
                                0.0, 0.0, 1.0, -319.8642957182012,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                0.9553266167640686, 0.047536421567201614, 0.2917042672634125, -146.83256363139458,
                                0.2955193519592285, -0.16834300756454468, -0.9403876662254333, 997.8867503358379,
                                0.004403707571327686, 0.9845815896987915, -0.17487049102783203, -16.312671795508777,
                                0.0, 0.0, 0.0, 1.0
                        })
                )
        );
    }

    /**
     * Different knife angles to test...
     * @return stream of knife angle, expected angle between knife and target, expected knife model translation
     * and expected knife model rotation. All expected values were read from the debugger after setting the given
     * knife angle.
     */
    static Stream<Arguments> knifeAngleProvider() {
        return Stream.of(
                arguments(
                        -10,
                        28.902061339214328,
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, -3.7375830004293675E-6,
                                0.0, 1.0, 0.0, -1824.0312175965944,
                                0.0, 0.0, 1.0, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                355.02252197265625, 62.60005187988281, 0.0, 125.20010375976562,
                                -62.60005187988281, 355.02252197265625, 0.0, 708.0450439453125,
                                0.0, 0.0, 360.49932861328125, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        })
                ),
                arguments(
                        5,
                        15.796863886874052,
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, 3.309421288122394E-7,
                                0.0, 1.0, 0.0, -1824.0312066123636,
                                0.0, 0.0, 1.0, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                359.12750244140625, -31.419586181640625, 0.0, -62.83917236328125,
                                31.419586181640625, 359.12750244140625, 0.0, 716.2550048828125,
                                0.0, 0.0, 360.49932861328125, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        })
                )
        );
    }

    /**
     * Different tilt angles to test...
     * @return stream of tilt angle, expected angle between knife and target, expected holder back model translation,
     * expected holder back model rotation, expected holder front model translation,
     * expected holder front model rotation, expected image content translation and expected image content rotation.
     * All expected values were read from the debugger after setting the given tilt angle.
     */
    static Stream<Arguments> tiltAngleProvider() {
        return Stream.of(
                arguments(
                        -10,
                        19.35477128729581,
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, 0.0,
                                0.0, 1.0, 0.0, 2272.7838620374478,
                                0.0, 0.0, 1.0, -206.6645113495451,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                360.49932861328125, 0.0, 0.0, 0.0,
                                0.0, 355.02252197265625, 62.60005187988281, -1522.7738033454807,
                                0.0, -62.60005187988281, 355.02252197265625, 269.26456136063643,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, 0.0,
                                0.0, 1.0, 0.0, 1966.335851194579,
                                0.0, 0.0, 1.0, -152.47682501152119,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                360.49932861328125, 0.0, 0.0, 0.0,
                                0.0, 355.02252197265625, 62.60005187988281, -1216.3257925026119,
                                0.0, -62.60005187988281, 355.02252197265625, 215.0768750226125,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, -672.8691470692755,
                                0.0, 1.0, 0.0, -163.7479223831221,
                                0.0, 0.0, 1.0, 285.12225256306226,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                0.9553266167640686, 0.047536421567201614, 0.2917042672634125, -64.3711034129874,
                                0.27920350432395935, 0.17855605483055115, -0.9434845447540283, 545.2986138243214,
                                -0.0969354435801506, 0.9827807545661926, 0.15730701386928558, -379.5484350048954,
                                0.0, 0.0, 0.0, 1.0
                        })
                ),
                arguments(
                        5,
                        17.814844077104315,
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, 0.0,
                                0.0, 1.0, 0.0, 2286.3358225505704,
                                0.0, 0.0, 1.0, 103.72696478167323,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                360.49932861328125, 0.0, 0.0, 0.0,
                                0.0, 359.12750244140625, -31.419586181640625, -1540.4307498194685,
                                0.0, 31.419586181640625, 359.12750244140625, -135.1465507978428,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, 0.0,
                                0.0, 1.0, 0.0, 1976.3344696994368,
                                0.0, 0.0, 1.0, 76.52962820201479,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                360.49932861328125, 0.0, 0.0, 0.0,
                                0.0, 359.12750244140625, -31.419586181640625, -1230.429396968335,
                                0.0, 31.419586181640625, 359.12750244140625, -107.94921421818435,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, -672.8691470692755,
                                0.0, 1.0, 0.0, -271.2531023531313,
                                0.0, 0.0, 1.0, 103.26484164510538,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                0.9553266167640686, 0.047536421567201614, 0.2917042672634125, -64.3711034129874,
                                0.29477861523628235, -0.08189047873020172, -0.9520501494407654, 701.8951358581002,
                                -0.02136925980448723, 0.995507001876831, -0.09224487096071243, -381.7244225833481,
                                0.0, 0.0, 0.0, 1.0
                        })
                )
        );
    }

    /**
     * Different rotation angles to test...
     * @return stream of rotation angle, expected angle between knife and target, expected holder back model translation,
     * expected holder back model rotation, expected holder front model translation,
     * expected holder front model rotation, expected image content translation and expected image content rotation.
     * All expected values were read from the debugger after setting the given rotation angle.
     */
    static Stream<Arguments> rotationAngleProvider() {
        return Stream.of(
                arguments(
                        50,
                        16.968177510800736,
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, 0.0,
                                0.0, 1.0, 0.0, 2290.8647145390105,
                                0.0, 0.0, 1.0, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                360.49932861328125, 0.0, 0.0, 0.0,
                                0.0, 360.49932861328125, 0.0, -1546.3314505591698,
                                0.0, 0.0, 360.49932861328125, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, 0.0,
                                0.0, 1.0, 0.0, 1979.6758852548783,
                                0.0, 0.0, 1.0, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                231.72450256347656, 0.0, 276.15850830078125, 0.0,
                                0.0, 360.49932861328125, 0.0, -1235.1426212750375,
                                -276.15850830078125, 0.0, 231.72450256347656, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, -365.4601247195973,
                                0.0, 1.0, 0.0, -240.76034707574877,
                                0.0, 0.0, 1.0, 64.10692728832532,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                0.5780836343765259, 0.7957239747047424, 0.18067289888858795, -275.9036687293484,
                                0.29179444909095764, 0.005185296759009361, -0.9564669728279114, 649.699288628415,
                                -0.7620205283164978, 0.60563725233078, -0.22919030487537384, 360.12374714690407,
                                0.0, 0.0, 0.0, 1.0
                        })
                ),
                arguments(
                        -20,
                        16.968177510800736,
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, 0.0,
                                0.0, 1.0, 0.0, 2290.8647145390105,
                                0.0, 0.0, 1.0, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                360.49932861328125, 0.0, 0.0, 0.0,
                                0.0, 360.49932861328125, 0.0, -1546.3314505591698,
                                0.0, 0.0, 360.49932861328125, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, 0.0,
                                0.0, 1.0, 0.0, 1979.6758852548783,
                                0.0, 0.0, 1.0, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                338.758544921875, 0.0, -123.29802703857422, 0.0,
                                0.0, 360.49932861328125, 0.0, -1235.1426212750375,
                                123.29802703857422, 0.0, 338.758544921875, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, -800.5711519332033,
                                0.0, 1.0, 0.0, -240.76034707574877,
                                0.0, 0.0, 1.0, 129.1002510930071,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new Transform3D(new double[]{
                                0.9137813448905945, -0.29695925116539, 0.27716219425201416, 182.565381096086,
                                0.29179444909095764, 0.005185296759009361, -0.9564669728279114, 649.699288628415,
                                0.28259456157684326, 0.9548760652542114, 0.09138929098844528, -586.6896389627566,
                                0.0, 0.0, 0.0, 1.0
                        })
                )
        );
    }

    /**
     * Different solution angles to test...
     * @return stream of solution angle, expected tilt angle, expected knife angle, expected distance to cut,
     * expected first touch point, expected valid solution
     * All expected values were read from the debugger after setting the given solution angle.
     */
    static Stream<Arguments> solutionAngleProvider() {
        return Stream.of(
                arguments(
                        50,
                        -13.475239909290694,
                        10.408956799771714,
                        207.0893731985671,
                        VertexPoint.BottomRight,
                        true
                ),
                arguments(
                        -100,
                        16.802561557999795,
                        -2.399800365591036,
                        203.86014312134841,
                        VertexPoint.BottomRight,
                        true
                ),
                arguments(
                        3,
                        -1.4476575703788224,
                        16.90812022706116,
                        212.88381464028384,
                        VertexPoint.BottomRight,
                        true
                )
        );
    }

    /**
     * Different knife angles to test initialising cutting mode with...
     * @return stream of knife angle, expected cutting plane mesh min, expected cutting plane mesh max,
     * expected cutting depth min, expected cutting depth max
     *
     * All expected values were read from the debugger after setting the knife angle.
     */
    static Stream<Arguments> cuttingProvider() {
        return Stream.of(
                arguments(
                        10,
                        new Point3d(-1798.2896728515625, -2143.1181640625, -1826.03125),
                        new Point3d(1798.2896728515625, -1508.9442138671875, 1826.03125),
                        -1826.0312247214051,
                        1826.0312247214051
                ),
                arguments(
                        -15,
                        new Point3d(-1763.8106689453125, -2298.642822265625, -1826.03125),
                        new Point3d(1763.8106689453125, -1353.4195556640625, 1826.03125),
                        -1716.5558314867285,
                        1935.5066179560818
                )
        );
    }

    /**
     * Different cutting depths to test cutting mode with...
     * @return stream of cutting depth, expected cutting plane translation, expected cutting plane rotation,
     * expected bdv viewer transform
     *
     * All expected values were read from the debugger after setting the cutting depth.
     */
    static Stream<Arguments> cuttingDepthProvider() {
        return Stream.of(
                arguments(
                        100,
                        // Plane is translated as part of update cut
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, 0.0,
                                0.0, 1.0, 0.0, 1926.0312247214051,
                                0.0, 0.0, 1.0, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        // Plane is not rotated as part of update cut (still identity transform)
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, 0.0,
                                0.0, 1.0, 0.0, 0.0,
                                0.0, 0.0, 1.0, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new double[]{
                                0.47261361983944955, 0.0, 0.0, 116.06497420285308,
                                0.0, 0.47261361983944955, 0.0, -4.09554300763665,
                                0.0, 0.0, 0.47261361983944955, -216.52746976593548
                        }
                ),
                arguments(
                        -400,
                        // Plane is translated as part of update cut
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, 0.0,
                                0.0, 1.0, 0.0, 1426.0312247214051,
                                0.0, 0.0, 1.0, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        // Plane is not rotated as part of update cut (still identity transform)
                        new Transform3D(new double[]{
                                1.0, 0.0, 0.0, 0.0,
                                0.0, 1.0, 0.0, 0.0,
                                0.0, 0.0, 1.0, 0.0,
                                0.0, 0.0, 0.0, 1.0
                        }),
                        new double[]{
                                0.46889388694350453, 0.008309389170377327, 0.05859275122722182, 86.31113859559343,
                                -0.0020812521104147785, -0.46532667205089073, 0.08264617550403355, 539.9702886238906,
                                0.059142411469374344, -0.08225372932217626, -0.4616276993811455, 459.6030671891897
                        }
                )
        );
    }


}
