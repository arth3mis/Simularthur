package in.freye.physics;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public abstract class Shape {

    final Vector3D pos, vel, acc;
    final boolean stationary;
    final double mass, density;
    // rotation/spin
//    protected final Vector3D angleVel;
//    protected final Quaternion orientation;

    Shape(Vector3D pos, Vector3D vel, Vector3D acc, boolean stationary, double mass, double density) {
        this.pos = pos;
        this.vel = vel;
        this.acc = acc;
        this.stationary = stationary;
        this.mass = mass;
        this.density = density;
    }

    abstract Shape copy();
}
