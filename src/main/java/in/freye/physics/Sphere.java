package in.freye.physics;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import static java.lang.Math.PI;

public class Sphere extends Shape {

    final double radius;

    Sphere(Vector3D pos, Vector3D vel, Vector3D acc, boolean stationary, double radius, double density) {
        super(pos, vel, acc, stationary, density * 4.0/3.0 * PI * radius * radius * radius, density);
        this.radius = radius;
    }

    Sphere copy() {
        return new Sphere(pos, vel, acc, stationary, radius, density);
    }
}
