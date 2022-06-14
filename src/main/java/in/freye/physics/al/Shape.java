package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.collections.api.list.ImmutableList;

public abstract class Shape {

    public final ShapeType type;
    public final Vector3D pos, vel, acc;
    final boolean movable;
    final double mass, density, bounciness;

    Shape(ShapeType type, Vector3D pos, Vector3D vel, Vector3D acc, boolean movable, double mass, double density, double bounciness) {
        assert World.isValidVector(pos) && World.isValidVector(vel) && World.isValidVector(acc) : "Die Position & Bewegung des Körpers muss definiert sein";
        assert density > 0 && mass > 0 && bounciness > 0: "Dichte, Masse und Reflexionsstärke eines Körpers müssen positiv sein";
        this.type = type;
        this.pos = pos;
        this.vel = vel;
        this.acc = acc;
        this.movable = movable;
        this.mass = mass;
        this.density = density;
        this.bounciness = bounciness;
    }

    abstract Shape applyMovement(double dt, Vector3D gravity);
    abstract Shape handleWallCollision(Vector3D worldSize);
    abstract Shape handleEntityCollision(ImmutableList<Shape> entities);

    //abstract Shape copy();

    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Shape s)) return false;  // hier wird implizit auch "obj == null" überprüft
        return pos.equals(s.pos) && vel.equals(s.vel) && acc.equals(s.acc) && movable == s.movable && mass == s.mass && bounciness == s.bounciness;
    }
}
