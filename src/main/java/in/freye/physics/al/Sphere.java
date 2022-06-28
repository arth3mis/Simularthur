package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.ImmutableList;

import java.util.stream.Stream;

public class Sphere extends Shape {

    public final double radius;
    private static final Logger LOGGER = LogManager.getLogger("monitoring");

    Sphere(Vector3D pos, Vector3D vel, Vector3D selfAcc, boolean movable, double radius, double density, double bounciness) {
        this(Shape.NO_ID, pos, vel, Vector3D.ZERO, selfAcc, movable, radius, density, bounciness);
    }

    private Sphere(long id, Vector3D pos, Vector3D vel, Vector3D acc, Vector3D selfAcc, boolean movable, double radius, double density, double bounciness) {
        super(id, ShapeType.SPHERE, pos, vel, acc, selfAcc, movable, density * 4.0/3.0 * Math.PI * radius * radius * radius, density, bounciness);
        // radius >= 0 wird implizit im super() call abgesichert, da durch mass ~ radius³ die Masse negativ würde
        // (oder bei Ausgleich durch negative Dichte der "density"-Assert anschlagen würde)
        this.radius = radius;
    }

    Shape calcAcceleration(Vector3D gravity, double airDensity, ImmutableList<Shape> gravityEntities) {
        if (!movable) return this;
        // Die Beschleunigung durch massereiche Objekte wird näherungsweise als konstant in einem kleinen Zeitabschnitt angesehen
        Vector3D eGravity = gravityEntities.stream().filter(e -> !pos.equals(e.pos))
                // a = G * m / r²  * (r / |r|);   r (der Abstand) ist der Vektor von this.pos bis e.pos
                .map(e -> e.pos.subtract(pos).scalarMultiply(World.GRAVITY_CONSTANT * e.mass / Math.pow(e.pos.subtract(pos).getNorm(), 3)))
                .reduce(Vector3D.ZERO, Vector3D::add);
        // Strömungswiderstand (die Beschleunigung wird ebenfalls als konstant in einem kleinen Zeitabschnitt angesehen)
        // Fw = 0.5 * cw * rho * A * v²
        // a = Fw / m
        // Richtung: entgegen der Geschwindigkeit            |---- vereint Richtung und v² ----|
        Vector3D drag = vel.getNorm() == 0 ? Vector3D.ZERO : vel.scalarMultiply(-vel.getNorm() * 0.5 * type.dragCoefficient * airDensity * (Math.PI*radius*radius) / mass);
        // acc = Summe aller Beschleunigungen
        return new Sphere(id, pos, vel, selfAcc.add(gravity).add(eGravity).add(drag), selfAcc, movable, radius, density, bounciness);
    }

    Shape applyMovement(double dt) {
        if (!movable) return this;
//        System.out.println(vel);
        return new Sphere(id, pos.add(dt*dt, acc.scalarMultiply(0.5)).add(dt,vel), vel.add(dt, acc), acc, selfAcc, movable, radius, density, bounciness);
    }

    Shape handleWallCollision(Vector3D worldSize, Shape prev) {
        assert prev.id == id : "Das 'prev' Objekt muss der vorherige Zustand dieses Körpers sein";
        Vector3D p = pos, v = vel, a = selfAcc;
        // todo trace pColl like with entities (nicht pos._ fixen sondern ganzen vektor)
        // Jede Komponente einzeln auf Kollision testen
        for (int i = 0; i < 3; i++) {
            if (pos.toArray()[i] < radius || pos.toArray()[i] + radius > worldSize.toArray()[i]) {
                // todo debug
//                if (id == 1 && i == 1 && pos.getY() > 0.5)
//                    System.out.print("");
                // Position korrigieren, falls außerhalb des Bereichs
                double pColl = pos.toArray()[i] < radius ? radius : worldSize.toArray()[i] - radius;
                p = new Vector3D(i==0 ? pColl : p.getX(), i==1 ? pColl : p.getY(), i==2 ? pColl : p.getZ());
                // Überschrittene Position kann auch zu überschrittener Geschwindigkeit führen, die korrigiert werden muss:
                // Mithilfe des "prev" Zustands kann die tatsächliche Kollisionszeit berechnet werden (Umstellung mit PQ-Formel).
                // pColl = 0.5*a*tColl² + v*tColl + p  (p,v,a sind Werte von prev)
                // (PQ-Formel) => tColl = -(v/a) + sqrt((v/a)² - 2(p-pColl)/a)
                double tColl = prev.acc.toArray()[i] == 0 ? 0 : -(prev.vel.toArray()[i] / prev.acc.toArray()[i]) + Math.sqrt(Math.pow((prev.vel.toArray()[i] / prev.acc.toArray()[i]), 2) - 2 * (prev.pos.toArray()[i] - pColl) / prev.acc.toArray()[i]);
                // Geschwindigkeitskorrektur und Invertierung der Komponente (Impulserhaltung: Keine Geschwindigkeit auf Wand "übertragbar", 100% Reflexion)
                double v1 = prev.vel.add(tColl, prev.acc).toArray()[i] * bounciness
                        // Nur invertieren, wenn die Komponente nicht schon durch die Beschleunigung invertiert wurde
                        * (Math.signum(prev.vel.add(tColl, prev.acc).toArray()[i]) == Math.signum(prev.vel.toArray()[i]) ? -1 : 1);
                // Notfall-Berechnung, wenn andere Formel keinen reellen Wert ausgibt
                if (!Double.isFinite(v1)) v1 = vel.toArray()[i] * -bounciness;
                // Schwelle, um Zittern zu vermeiden todo keep or remove?
                if (Math.abs(v1) < 0.00001) v1 = 0;
                // Geschwindigkeitskomponente invertieren
                v = new Vector3D(i==0 ? v1 : v.getX(), i==1 ? v1 : v.getY(), i==2 ? v1 : v.getZ());
                // Eigenbeschleunigungskomponente auf 0 setzen
                // (damit Eigenbeschleunigung nicht wie eine Gravitation, sondern eher wie ein Antrieb/Triebwerk funktioniert)
                // todo change?
                a = new Vector3D((i==0 ? 0 : a.getX()), (i==1 ? 0 : a.getY()), (i==2 ? 0 : a.getZ()));
            }
        }
        return new Sphere(id, p, v, acc, a, movable, radius, density, bounciness);
    }

    Shape calcEntityCollisionCorrections(ImmutableList<Shape> entities, Shape prev) {
        assert prev.id == id : "Das 'prev' Objekt muss der vorherige Zustand dieses Körpers sein";
        if (!movable) return this;
        // Kollision mit anderen Kugeln (Stream nicht parallel, da es fast immer nur eine Kollision gibt)
        return getCollidingSpheres(this, entities)
                // Auswirkungen der Kollisionen auf "this" anwenden
                .reduce(this, (a, b) -> {
                    // Position bei Kollision: Korrigiert die Hälfte des Abstands, andere Kugel "übernimmt" die andere Hälfte (wenn sie auch movable ist)
                    Vector3D pColl = a.pos.add(a.pos.subtract(b.pos).normalize().scalarMultiply(a.radius + b.radius - a.pos.subtract(b.pos).getNorm()).scalarMultiply(b.movable ? 0.5 : 1));
                    //System.out.println(a.pos.subtract(pColl));// todo debug
                    // tColl = -(v/a) + sqrt((v/a)² - 2(p-pColl)/a)
                    double tColl = -(prev.vel.getNorm()/prev.acc.getNorm()) + Math.sqrt(Math.pow(prev.vel.getNorm()/prev.acc.getNorm(), 2) - 2 * (prev.pos.getNorm() - pColl.getNorm()) / prev.acc.getNorm());
                    // tColl ist NaN bei konstanter Geschwindigkeit, dann muss diese nicht korrigiert werden
                    return new Sphere(id, pColl, Double.isNaN(tColl) ? vel : prev.vel.add(tColl, prev.acc), a.acc, a.selfAcc, a.movable, a.radius, a.density, a.bounciness);
                });
    }

    Shape applyEntityCollisionDeflections(ImmutableList<Shape> detectEntities, ImmutableList<Shape> deflectionEntities/*, Shape prev*/) {
        if (!movable) return this;
        // Führt erneut die Kollisionsdetektion mit vorherigen Zuständen aus
        // (benötigt weniger Rechenaufwand als mehrfache Korrekturberechnungen, daher wurden diese in eigene Funktion ausgelagert)
        return getCollidingSpheres((Sphere) detectEntities.select(this::equals).getAny(), detectEntities)
                .map(e -> (Sphere) deflectionEntities.select(e::equals).getAny())
                // todo orthogonale Zerlegung von selfAcc: der Teil parallel zum Vektor (b.pos-a.pos) wird 0 (-> Hintergrund: siehe handleWallCollision)
                //  test and think through:
                //  a.selfAcc.subtract(b.pos.subtract(a.pos).scalarMultiply(a.selfAcc.dotProduct(b.pos.subtract(a.pos)) / b.pos.subtract(a.pos).getNormSq())),
                .reduce(this, (a, b) -> new Sphere(id, a.pos,
                        // Geschwindigkeit nach Kollision:
                        // v1' = (v1 + 2*m2/(m1+m2) * dot(v2-v1, p1-p2) / |p1-p2| * (p1-p2)) * bounciness
                        // todo prevent NaN
                        a.vel.add(2*b.mass/(a.mass+b.mass) * b.vel.subtract(a.vel).dotProduct(a.pos.subtract(b.pos)) / a.pos.subtract(b.pos).getNormSq(), a.pos.subtract(b.pos)).scalarMultiply(bounciness),
                        a.acc, a.selfAcc, a.movable, a.radius, a.density, a.bounciness));
    }

    /**
     * Findet alle mit kollidierenden Kugeln, Kollisionsdetektion: Abstand der Mittelpunkte < Summe der Radii
     * @param s Kugel, gegen die die Liste getestet wird
     * @param entities Liste aller Körper
     * @return Stream der getroffenen Kugeln (nicht parallel, da es meistens nur eine Kollision gibt)
     */
    private static Stream<Sphere> getCollidingSpheres(Sphere s, ImmutableList<Shape> entities) {
        // todo zero norm error in calcEntityCollisionCorrections when parallelStream-ing
        return entities.stream().filter(e -> e.type == ShapeType.SPHERE && !e.equals(s) && !s.pos.equals(e.pos))
                // Toleranz (1 Nanometer), damit z.B. keine Kollision bei direkt aneinander liegenden Kugeln erkannt wird
                .map(e -> (Sphere) e).filter(e -> s.pos.distance(e.pos) + 1.0e-9 < s.radius + e.radius);
    }
}
