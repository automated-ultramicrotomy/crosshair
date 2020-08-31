package de.embl.schwab.crosshair.utils;

import ij3d.Content;
import org.scijava.vecmath.Point3d;

import java.util.ArrayList;
import java.util.Collections;

public class Utils {

    public static void printImageMinMax (Content imageContent) {
        Point3d min = new Point3d();
        Point3d max = new Point3d();
        imageContent.getMax(max);
        imageContent.getMin(min);
        System.out.println(min.toString());
        System.out.println(max.toString());
    }

    public static int findIndexOfMaxMin (ArrayList<Double> values, String MinMax) {
        double chosenValue = 0;
        if (MinMax.equals("max")) {
            chosenValue = Collections.max(values);
        } else if (MinMax.equals("min")) {
            chosenValue = Collections.min(values);
        }

        int result = 0;
        for (int i=0; i<values.size(); i++) {
            if (values.get(i) == chosenValue) {
                result = i;
                break;
            }
        }
        return result;

    }
}
