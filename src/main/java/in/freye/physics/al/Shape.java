package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.collections.api.list.ImmutableList;

public abstract class Shape {

    final ShapeType type;
    public final Vector3D pos, vel, acc;
    final boolean movable;
    final double mass, density, bounciness;

    Shape(ShapeType type, Vector3D pos, Vector3D vel, Vector3D acc, boolean movable, double mass, double density) {
        assert World.isValidVector(pos) && World.isValidVector(vel) && World.isValidVector(acc) : "Die Position & Bewegung des Körpers muss definiert sein";
        assert density >= 0 && mass >= 0 : "Dichte & Masse eines Materials dürfen nicht negativ sein";
        this.type = type;
        this.pos = pos;
        this.vel = vel;
        this.acc = acc;
        this.movable = movable;
        this.mass = mass;
        this.density = density;
        this.bounciness = 0.9;//todo
    }

    abstract Shape update(double dt, Vector3D gravity);
    abstract Shape handleWallCollision(Vector3D worldSize);
    abstract Shape handleEntityCollision(ImmutableList<Shape> entities);
}
