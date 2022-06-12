package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;

import java.util.Arrays;

import static java.lang.Math.PI;

public class Sphere extends Shape {

    public final double radius;

    public Sphere(Vector3D pos, Vector3D vel, Vector3D acc, boolean movable, double radius, double density) {
        super(ShapeType.SPHERE, pos, vel, acc, movable, density * 4.0/3.0 * PI * radius * radius * radius, density);
        // radius >= 0 wird implizit im super() call abgesichert, da durch mass ~ radius³ die Masse negativ würde
        // (oder bei Ausgleich durch negative Dichte der density assert anschlagen würde)
        this.radius = radius;
    }

    // g ist die Gravitation
    Shape applyMovement(double dt, Vector3D g) {
        if (!movable) return this;
        // pos = 0.5 * acc * t² + vel * t
        // vel = acc * t
        return new Sphere(pos.add(dt*dt,acc.add(g).scalarMultiply(0.5)).add(dt,vel), vel.add(dt,acc.add(g)), acc, movable, radius, density);
    }

    Shape applyEntityGravity(ImmutableList<Shape> entities) {
        return this;
    }

    Shape handleWallCollision(Vector3D worldSize) {
        Vector3D p = pos, v = vel, a = acc;
        for (int i = 0; i < 3; i++) {
            if (pos.toArray()[i] < radius || pos.toArray()[i] + radius > worldSize.toArray()[i]) {
                // Position korrigieren, falls außerhalb des Bereichs
                double correct = pos.toArray()[i] < radius ? radius : worldSize.toArray()[i] - radius;
                p = new Vector3D(i==0 ? correct : p.getX(), i==1 ? correct : p.getY(), i==2 ? correct : p.getZ());
                // Geschwindigkeitskomponente invertieren
                v = new Vector3D(v.getX() * (i==0 ? -bounciness : 1), v.getY() * (i==1 ? -bounciness : 1), v.getZ() * (i==2 ? -bounciness : 1));
                // Beschleunigungskomponente auf 0 setzen
                a = new Vector3D(a.getX() * (i==0 ? 0 : 1), a.getY() * (i==1 ? 0 : 1), a.getZ() * (i==2 ? 0 : 1));
            }
        }
        return new Sphere(p, v, a, movable, radius, density);
    }

    Shape handleEntityCollision(ImmutableList<Shape> entities) {
        // Berechnung abhängig vom Typ des anderen Körpers
        entities.stream().filter(e -> e != this && e.type == ShapeType.SPHERE)
                .map(e -> (Sphere) e)
                .filter(e -> pos.distance(e.pos) < radius + e.radius)
                .toList();
        return this;
    }
}
