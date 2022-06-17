package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.collections.api.list.ImmutableList;

import static java.lang.Math.PI;

public class Sphere extends Shape {

    public final double radius;

    Sphere(Vector3D pos, Vector3D vel, Vector3D selfAcc, boolean movable, double radius, double density, double bounciness) {
        this(Shape.NO_ID, pos, vel, Vector3D.ZERO, selfAcc, movable, radius, density, bounciness);
    }

    private Sphere(long id, Vector3D pos, Vector3D vel, Vector3D acc, Vector3D selfAcc, boolean movable, double radius, double density, double bounciness) {
        super(id, ShapeType.SPHERE, pos, vel, acc, selfAcc, movable, density * 4.0/3.0 * PI * radius * radius * radius, density, bounciness);
        // radius >= 0 wird implizit im super() call abgesichert, da durch mass ~ radius³ die Masse negativ würde
        // (oder bei Ausgleich durch negative Dichte der "density"-Assert anschlagen würde)
        this.radius = radius;
    }

    Shape calcAcceleration(Vector3D gravity, ImmutableList<Shape> gravityEntities) {
        Vector3D eGravity = gravityEntities.stream().parallel().filter(e -> !pos.equals(e.pos))
                // a = G * m / r²  * (r / |r|); r (Abstand) ist der Vektor von this.pos bis e.pos
                .map(e -> e.pos.subtract(pos).scalarMultiply(World.GRAVITY_CONSTANT * e.mass / Math.pow(e.pos.subtract(pos).getNorm(), 3)))
                .reduce(Vector3D.ZERO, Vector3D::add);
        // new acc = Summe der verschiedenen Beschleunigungen
        return new Sphere(id, pos, vel, selfAcc.add(gravity).add(eGravity), selfAcc, movable, radius, density, bounciness);
    }

    Shape applyMovement(double dt) {
        if (!movable) return this;
        return new Sphere(id,
                pos.add(dt*dt, acc.scalarMultiply(0.5)).add(dt,vel),
                vel.add(dt, acc),
                acc, selfAcc, movable, radius, density, bounciness);
    }

    Shape handleWallCollision(Vector3D worldSize, Shape prev) {
        // Wird auch für nicht bewegliche Objekte ausgeführt, um einmalige Positionskorrektur unter Verwendung des Radius auszuführen
        Vector3D p = pos, v = vel, a = selfAcc;
        for (int i = 0; i < 3; i++) {
            if (pos.toArray()[i] < radius || pos.toArray()[i] + radius > worldSize.toArray()[i]) {
                // Position korrigieren, falls außerhalb des Bereichs
                double correct = pos.toArray()[i] < radius ? radius : worldSize.toArray()[i] - radius;
                p = new Vector3D(i==0 ? correct : p.getX(), i==1 ? correct : p.getY(), i==2 ? correct : p.getZ());
                // Geschwindigkeitskomponente invertieren
                // todo über überschrittene pos zurückrechnen, wie viel zu groß die vel geworden ist, und reduzieren
                // todo implement threshold under which vel is set to 0 instead of reflecting (prevent jittering)
                v = new Vector3D(v.getX() * (i==0 ? -bounciness : 1), v.getY() * (i==1 ? -bounciness : 1), v.getZ() * (i==2 ? -bounciness : 1));
                // Eigenbeschleunigungskomponente auf 0 setzen
                a = new Vector3D(a.getX() * (i==0 ? 0 : 1), a.getY() * (i==1 ? 0 : 1), a.getZ() * (i==2 ? 0 : 1));
            }
        }
        return new Sphere(id, p, v, acc, a, movable, radius, density, bounciness);
    }

    Shape handleEntityCollision(ImmutableList<Shape> entities) {
        // Berechnung abhängig vom Typ des anderen Körpers
        //
        // Kollision mit anderen Kugeln
        return entities.stream()
            // Typ-Filter und Sicherheitstests
            .filter(e -> e.type == ShapeType.SPHERE && !e.equals(this) && !pos.equals(e.pos)).map(e -> (Sphere) e)
            // Kollisionsdetektion: Abstand der Mittelpunkte < Summe der Radii
            .filter(e -> pos.distance(e.pos) < radius + e.radius)
            // Auswirkungen der Kollisionen auf "this" anwenden
            .reduce(this, (a, b) -> new Sphere(id,
                a.pos.add(a.pos.subtract(b.pos)
                        .normalize()
                        .scalarMultiply(a.radius + b.radius - a.pos.distance(b.pos))
                        // Korrigiert die Hälfte des Abstands, andere Kugel "übernimmt" die andere Hälfte
                        .scalarMultiply(0.5)),
                // Geschwindigkeit wird über Impulserhaltung und Energieerhaltung berechnet (siehe Quelle "Leifi-Physik"):
                // new vel = (a.mass * a.vel + b.mass * (2 * b.vel - a.vel)) / (a.mass + b.mass) * bounciness
                // todo hier ist noch keine richtung drin oder?? richtung über posDiff bestimmen
                // todo auch hier fix vel bug (see todo in wallColl)
                a.vel.scalarMultiply(a.mass).add(b.mass, b.vel.scalarMultiply(2).subtract(a.vel)).scalarMultiply(bounciness / (a.mass + b.mass)),
                // orthogonale Zerlegung von acc: der Teil parallel zum Vektor (b.pos-a.pos) wird 0
                a.acc,
                a.selfAcc,//a.acc.subtract(b.pos.subtract(a.pos).scalarMultiply(a.acc.dotProduct(b.pos.subtract(a.pos)) / b.pos.subtract(a.pos).getNormSq())),  // todo test and think through
                a.movable, a.radius, a.density, a.bounciness));
    }
}
