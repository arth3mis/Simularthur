package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;

import static java.lang.Math.PI;

public class Sphere extends Shape {

    public final double radius;

    public Sphere(Vector3D pos, Vector3D vel, Vector3D acc, boolean movable, double radius, double density) {
        super(pos, vel, acc, movable, density * 4.0/3.0 * PI * radius * radius * radius, density);
        // radius >= 0 wird implizit im super() call abgesichert, da durch mass ~ radius³ die Masse negativ würde
        // (oder bei Ausgleich durch negative Dichte der density assert anschlagen würde)
        this.radius = radius;
    }

    // g ist die Gravitation
    Shape update(double dt, Vector3D g) {
        if (!movable) return this;
        // todo make wall collision here (maybe as 6 cuboids with AABB) -> walls are !movable, treat with priority
        return new Sphere(pos.add(dt*dt,acc.add(g).scalarMultiply(0.5)).add(dt,vel), vel.add(dt,acc.add(g)), acc, movable, radius, density);
    }

    ImmutableSet<Shape> detectCollisions(ImmutableList<Shape> entities, int skipN) {
        return null;
    }

    Shape handleCollisions(ImmutableSet<Shape> collisions) {
        collisions.stream();
        return null;
    }
}
