package de.embl.cba.targeting.utils;

import ij3d.Content;
import org.scijava.vecmath.Point3d;

public class Utils {

    public static void printImageMinMax (Content imageContent) {
        Point3d min = new Point3d();
        Point3d max = new Point3d();
        imageContent.getMax(max);
        imageContent.getMin(min);
        System.out.println(min.toString());
        System.out.println(max.toString());
    }
}
