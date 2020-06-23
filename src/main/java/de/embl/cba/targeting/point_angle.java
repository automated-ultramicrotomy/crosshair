package de.embl.cba.targeting;

import org.scijava.vecmath.Point3f;

public class point_angle {
    private Point3f point;
    private Double angle;

    point_angle(Point3f point, Double angle) {
        this.point = point;
        this.angle = angle;
    }

    public Point3f getPoint () {
        return point;
    }

    Double getAngle() {
        return angle;
    }
}
