package in.freye.physics;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public abstract class Shape {

    protected final float mass;
    protected final boolean stationary;
    protected final double gravity;
    protected final Vector3D pos, vel, acc;
    // rotation/spin
//    protected final Vector3D angleVel;
//    protected final Quaternion orientation;

    Shape(float density, Vector3D pos) {
        this.pos = pos;
        vel = Vector3D.ZERO;
        acc = Vector3D.ZERO;
        mass = 0;
        stationary = false;
        gravity = 0;
    }

    protected Shape(float mass, boolean stationary, double gravity, Vector3D pos, Vector3D vel, Vector3D acc) {
        this.mass = mass;
        this.stationary = stationary;
        this.gravity = gravity;
        this.pos = pos;
        this.vel = vel;
        this.acc = acc;
    }

    abstract Shape copy();
}
