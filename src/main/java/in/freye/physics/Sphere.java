package in.freye.physics;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Sphere extends Shape {

    Sphere(float density, Vector3D pos) {
        super(density, pos);
    }

    protected Sphere(float mass, boolean stationary, double gravity, Vector3D pos, Vector3D vel, Vector3D acc) {
        super(mass, stationary, gravity, pos, vel, acc);
    }

    Sphere copy() {
        return new Sphere(mass, stationary, gravity, pos, vel, acc);
    }
}
