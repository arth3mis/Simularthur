package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.collections.api.list.ImmutableList;

import static java.lang.Math.PI;

public class Sphere extends Shape {

    public final double radius;

    Sphere(long id, Vector3D pos, Vector3D vel, Vector3D acc, boolean movable, double radius, double density, double bounciness) {
        super(id, ShapeType.SPHERE, pos, vel, acc, movable, density * 4.0/3.0 * PI * radius * radius * radius, density, bounciness);
        // radius >= 0 wird implizit im super() call abgesichert, da durch mass ~ radius³ die Masse negativ würde
        // (oder bei Ausgleich durch negative Dichte der "density"-Assert anschlagen würde)
        this.radius = radius;
    }

    // g ist die Gravitation
    Shape applyMovement(double dt, Vector3D g) {
        if (!movable) return this;
        // pos = 0.5 * acc * t² + vel * t
        // vel = acc * t
        return new Sphere(id,
                pos.add(dt*dt,acc.add(g).scalarMultiply(0.5)).add(dt,vel),
                vel.add(dt,acc.add(g)),
                acc, movable, radius, density, bounciness);
    }

    Shape applyEntityGravity(ImmutableList<Shape> entities) {
        return this;
    }

    Shape handleWallCollision(Vector3D worldSize) {
        // Wird auch für nicht bewegliche Objekte ausgeführt, um einmalige Positionskorrektur unter Verwendung des Radius auszuführen
        Vector3D p = pos, v = vel, a = acc;
        for (int i = 0; i < 3; i++) {
            if (pos.toArray()[i] < radius || pos.toArray()[i] + radius > worldSize.toArray()[i]) {
                // Position korrigieren, falls außerhalb des Bereichs
                double correct = pos.toArray()[i] < radius ? radius : worldSize.toArray()[i] - radius;
                p = new Vector3D(i==0 ? correct : p.getX(), i==1 ? correct : p.getY(), i==2 ? correct : p.getZ());
                // Geschwindigkeitskomponente invertieren
                // todo evtl über überschrittene pos zurückrechnen, wie viel zu groß die vel geworden ist, und reduzieren
                // todo implement threshold when vel is set to 0 instead of reflecting (prevent jittering)
                v = new Vector3D(v.getX() * (i==0 ? -bounciness : 1), v.getY() * (i==1 ? -bounciness : 1), v.getZ() * (i==2 ? -bounciness : 1));
                // Beschleunigungskomponente auf 0 setzen
                a = new Vector3D(a.getX() * (i==0 ? 0 : 1), a.getY() * (i==1 ? 0 : 1), a.getZ() * (i==2 ? 0 : 1));
            }
        }
        return new Sphere(id, p, v, a, movable, radius, density, bounciness);
    }

    Shape handleEntityCollision(ImmutableList<Shape> entities) {
        // Berechnung abhängig vom Typ des anderen Körpers
        //
        // Kollision mit anderen Kugeln
        // Geschwindigkeit wird über Impulserhaltung und Energieerhaltung berechnet:
        // newVelocity = (a.mass * a.vel + b.mass * (2 * b.vel - a.vel)) / (a.mass + b.mass) * bounciness
        return entities.stream()
                .filter(e -> !e.equals(this) && e.type == ShapeType.SPHERE)
                .map(e -> (Sphere) e)
                .filter(e -> pos.distance(e.pos) < radius + e.radius)
                .reduce(this, (a, b) -> new Sphere(id,
                    a.pos.add(a.pos.subtract(b.pos)
                            // Der zufällige Faktor ist zum Zerstreuen von Körpern an derselben Position
                            .add(a.pos.distance(b.pos) == 0 ? 1 : 0,
                                    new Vector3D((Math.random()*0.000002+0.000001)*(Math.random()<0.5?-1:1),
                                            (Math.random()*0.000002+0.000001)*(Math.random()<0.5?-1:1),
                                            (Math.random()*0.000002+0.000001)*(Math.random()<0.5?-1:1)))
                            .normalize()
                            .scalarMultiply(a.radius + b.radius - a.pos.distance(b.pos))
                            // Korrigiert die Hälfte des Abstands, andere Kugel übernimmt die andere Hälfte
                            .scalarMultiply(0.5)),
                    a.vel.scalarMultiply(a.mass).add(b.mass, b.vel.scalarMultiply(2).subtract(a.vel)).scalarMultiply(bounciness / (a.mass + b.mass)),
                    a.acc,  // todo what manipulation for acc? set something to 0, but angle is important
                    a.movable, a.radius, a.density, a.bounciness));
    }

    Shape indexed(long id) {
        return new Sphere(id, pos, vel, acc, movable, radius, density, bounciness);
    }
}
