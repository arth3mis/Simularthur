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
                // a = G * m / r²  * (r / |r|); r (der Abstand) ist der Vektor von this.pos bis e.pos
                .map(e -> e.pos.subtract(pos).scalarMultiply(World.GRAVITY_CONSTANT * e.mass / Math.pow(e.pos.subtract(pos).getNorm(), 3)))
                .reduce(Vector3D.ZERO, Vector3D::add);
        // acc = Summe aller Beschleunigungen
        return new Sphere(id, pos, vel, selfAcc.add(gravity).add(eGravity), selfAcc, movable, radius, density, bounciness);
    }

    Shape applyMovement(double dt) {
        if (!movable) return this;
//        System.out.println(vel);
        return new Sphere(id, pos.add(dt*dt, acc.scalarMultiply(0.5)).add(dt,vel), vel.add(dt, acc), acc, selfAcc, movable, radius, density, bounciness);
    }

    Shape handleWallCollision(Vector3D worldSize, Shape prev) {
        assert prev.id == id : "Das 'prev' Objekt muss der vorherige Zustand dieses Körpers sein";
        Vector3D p = pos, v = vel, a = selfAcc;
        // Jede Komponente einzeln auf Kollision testen
        for (int i = 0; i < 3; i++) {
            if (pos.toArray()[i] < radius || pos.toArray()[i] + radius > worldSize.toArray()[i]) {
                // Position korrigieren, falls außerhalb des Bereichs
                double correct = pos.toArray()[i] < radius ? radius : worldSize.toArray()[i] - radius;
                p = new Vector3D(i==0 ? correct : p.getX(), i==1 ? correct : p.getY(), i==2 ? correct : p.getZ());
                // Überschrittene Position kann auch zu überschrittener Geschwindigkeit führen, die korrigiert werden muss:
                // Mithilfe des "prev" Zustands kann die tatsächliche Kollisionszeit berechnet werden (Umstellung mit PQ-Formel).
                // pColl = 0.5*a*tColl² + v*tColl + p  (p,v,a sind Werte von prev)
                // <=> tColl = -(v/a) + sqrt((v/a)² - 2(p-pColl)/a)
                double tColl = -(prev.vel.toArray()[i] / prev.acc.toArray()[i]) + Math.sqrt(Math.pow((prev.vel.toArray()[i] / prev.acc.toArray()[i]), 2) - 2 * (prev.pos.toArray()[i] - correct) / prev.acc.toArray()[i]);
                double v1 = prev.acc.toArray()[i] * tColl + prev.vel.toArray()[i];
                // todo keep or remove?
//                if (Math.abs(v1) < 0.0001) v1 = 0;
                // Geschwindigkeitskomponente invertieren
                v = new Vector3D(i==0 ? -v1*bounciness : v.getX(), i==1 ? -v1*bounciness : v.getY(), i==2 ? -v1*bounciness : v.getZ());
                // Eigenbeschleunigungskomponente auf 0 setzen
                // (damit Eigenbeschleunigung nicht wie eine Gravitation, sondern eher wie ein Antrieb/Triebwerk funktioniert)
                a = new Vector3D((i==0 ? 0 : a.getX()), (i==1 ? 0 : a.getY()), (i==2 ? 0 : a.getZ()));
            }
        }
        return new Sphere(id, p, v, acc, a, movable, radius, density, bounciness);
    }

    Shape handleEntityCollision(ImmutableList<Shape> entities) {
        if (!movable) return this;
        // Berechnung abhängig vom Typ des anderen Körpers
        //
        // Kollision mit anderen Kugeln (Stream nicht parallel, da es fast immer nur eine Kollision gibt)
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
                        // Korrigiert die Hälfte des Abstands, andere Kugel "übernimmt" die andere Hälfte (nur wenn andere auch movable ist)
                        .scalarMultiply(b.movable ? 0.5 : 1)),
                // Geschwindigkeit wird über Impulserhaltung und Energieerhaltung berechnet (siehe Quelle "Leifi-Physik"):
                // newVel = (a.mass * a.vel + b.mass * (2 * b.vel - a.vel)) / (a.mass + b.mass) * bounciness
                // todo hier ist noch keine richtung drin oder?? richtung mit a.pos.subtract(b.pos) bestimmen
                // todo auch hier fix vel bug (see todo in wallColl)
                a.vel.scalarMultiply(a.mass).add(b.mass, b.vel.scalarMultiply(2).subtract(a.vel)).scalarMultiply(bounciness / (a.mass + b.mass)),
                // orthogonale Zerlegung von selfAcc: der Teil parallel zum Vektor (b.pos-a.pos) wird 0 (-> Hintergrund: siehe handleWallCollision)
                a.acc, a.selfAcc,//a.selfAcc.subtract(b.pos.subtract(a.pos).scalarMultiply(a.selfAcc.dotProduct(b.pos.subtract(a.pos)) / b.pos.subtract(a.pos).getNormSq())),  // todo test and think through
                a.movable, a.radius, a.density, a.bounciness));
    }
}
