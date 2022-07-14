package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.ImmutableList;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class Sphere extends Shape {
    private final double radius;

    private static final Logger LOGGER = LogManager.getLogger("monitoring");

    Sphere(Vector3D pos, Vector3D vel, Vector3D selfAcc, boolean movable, double radius, double density, double bounciness) {
        this(Shape.NO_ID, pos, vel, Vector3D.ZERO, selfAcc, movable, radius, density, bounciness);
    }

    private Sphere(long id, Vector3D pos, Vector3D vel, Vector3D acc, Vector3D selfAcc, boolean movable, double radius, double density, double bounciness) {
        super(id, ShapeType.SPHERE, pos, vel, acc, selfAcc, movable, density * 4.0/3.0 * Math.PI * radius * radius * radius, density, bounciness);
        // radius >= 0 wird implizit im super() call abgesichert, da durch mass ~ radius³ die Masse negativ würde
        // (bei dennoch positiver Masse wegen negativer Dichte würde der "density"-Assert anschlagen)
        this.radius = radius;
    }

    Shape calcAcceleration(Vector3D gravity, double airDensity, ImmutableList<Shape> gravityEntities) {
        assert V3.isValidVector(gravity) && Double.isFinite(airDensity) && gravityEntities != null : "Beschleunigungsfaktoren müssen reell initialisiert sein";
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
        LOGGER.info("ID={}; a = {}m/s² = {}_selfAcc + {}_gravity + {}_eGravity + {}_drag",
                id, V3.r(selfAcc.add(gravity).add(eGravity).add(drag)), V3.r(selfAcc), V3.r(gravity), V3.r(eGravity), V3.r(drag));
        return new Sphere(id, pos, vel, selfAcc.add(gravity).add(eGravity).add(drag), selfAcc, movable, radius, density, bounciness);
    }

    Shape applyMovement(double dt) {
        if (!movable) return this;
        LOGGER.info("ID={}; p({}s) = {}m = {}m/s² * ({}s)² + {}m/s * {}s + {}m",
                id, V3.r(dt), V3.r(pos.add(dt*dt, acc.scalarMultiply(0.5)).add(dt,vel)), V3.r(acc), V3.r(dt), V3.r(vel), V3.r(dt), V3.r(pos));
        LOGGER.info("ID={}; v({}s) = {}m/s = {}m/s² * {}s + {}m/s",
                id, V3.r(dt), V3.r(vel.add(dt, acc)), V3.r(acc), V3.r(dt), V3.r(vel));
        return new Sphere(id, pos.add(dt*dt, acc.scalarMultiply(0.5)).add(dt,vel), vel.add(dt, acc), acc, selfAcc, movable, radius, density, bounciness);
    }

    Shape handleWallCollision(Vector3D worldSize, Shape prev) {
        assert prev.id == id : "Das 'prev' Objekt muss der vorherige Zustand dieses Körpers sein";
        if (!movable) return this;
        Vector3D p = pos, v = vel;
        // Jede Komponente einzeln auf Kollision testen
        for (int i = 0; i < 3; i++) {
            if (pos.toArray()[i] < radius || pos.toArray()[i] + radius > worldSize.toArray()[i]) {
                // Position korrigieren, falls außerhalb des Bereichs
                double pColl = pos.toArray()[i] < radius ? radius : worldSize.toArray()[i] - radius;
                p = new Vector3D(i==0 ? pColl : p.getX(), i==1 ? pColl : p.getY(), i==2 ? pColl : p.getZ());
                // Überschrittene Position kann auch zu überschrittener Geschwindigkeit führen, die korrigiert werden muss:
                // Mithilfe des "prev" Zustands kann die tatsächliche Kollisionszeit berechnet werden (Umstellung mit PQ-Formel).
                // pColl = 0.5*a*tColl² + v*tColl + p  (p,v,a sind Werte von prev)
                // => tColl = -(v/a) + sqrt((v/a)² - 2(p-pColl)/a)
                double tColl = prev.acc.toArray()[i] == 0 ? 0 : -(prev.vel.toArray()[i] / prev.acc.toArray()[i])
                        + Math.sqrt(Math.pow((prev.vel.toArray()[i] / prev.acc.toArray()[i]), 2) - 2 * (prev.pos.toArray()[i] - pColl) / prev.acc.toArray()[i]);
                // Geschwindigkeitskorrektur und Invertierung der Komponente
                // (Impulserhaltung: Keine Geschwindigkeit auf Wand "übertragbar" -> 100% Reflexion)
                double v1 = prev.vel.add(tColl, prev.acc).toArray()[i] * bounciness
                        // Nur invertieren, wenn die Komponente nicht schon durch die Beschleunigung invertiert wurde
                        * (Math.signum(prev.vel.add(tColl, prev.acc).toArray()[i]) == Math.signum(prev.vel.toArray()[i]) ? -1 : 1);
                // Notfall-Berechnung, wenn andere Formel keinen reellen Wert ausgibt
                if (!Double.isFinite(v1)) v1 = vel.toArray()[i] * -bounciness;
                // Schwelle, um Zittern zu vermeiden (für kleine Welten wird die Schwelle reduziert)
                if (Math.abs(v1) < Math.min(0.001, 0.001 * Arrays.stream(worldSize.toArray()).min().orElse(1))) v1 = 0;
                // Geschwindigkeitskomponente invertieren
                v = new Vector3D(i==0 ? v1 : v.getX(), i==1 ? v1 : v.getY(), i==2 ? v1 : v.getZ());
            }
        }
        if (!p.equals(pos) || !v.equals(vel))
            LOGGER.info("ID={}; Wandkollision: p'={}m; v'={}m/s", id, V3.r(p), V3.r(v));
        return new Sphere(id, p, v, acc, selfAcc, movable, radius, density, bounciness);
    }

    Shape calcEntityCollisionCorrections(ImmutableList<Shape> entities, Shape prev) {
        assert prev.id == id : "Das 'prev' Objekt muss der vorherige Zustand dieses Körpers sein";
        if (!movable) return this;
        // Kollision mit anderen Kugeln (Stream nicht parallel, da es fast immer nur eine Kollision gibt)
        return getCollidingSpheres(this, entities)
                // Auswirkungen der Kollisionen auf "this" anwenden
                .reduce(this, (a, b) -> {
                    // Position bei Kollision: Korrigiert die Hälfte des Abstands,
                    // die andere Kugel "übernimmt" die andere Hälfte (wenn sie auch movable ist)
                    Vector3D pColl = a.pos.add(a.pos.subtract(b.pos).normalize()
                            .scalarMultiply(a.radius + b.radius - a.pos.subtract(b.pos).getNorm())
                            .scalarMultiply(b.movable ? 0.5 : 1));
                    // tColl = -(v/a) + sqrt((v/a)² - 2(p-pColl)/a)
                    double tColl = -(prev.vel.getNorm()/prev.acc.getNorm())
                            + Math.sqrt(Math.pow(prev.vel.getNorm()/prev.acc.getNorm(), 2) - 2 * (prev.pos.getNorm() - pColl.getNorm()) / prev.acc.getNorm());
                    // Wenn Berechnung keinen reellen Wert ergibt, drehe Vorzeichen von (p-pColl)
                    // tColl = -(v/a) + sqrt((v/a)² - 2(pColl-p)/a)
                    if (Double.isNaN(tColl)) tColl = -(prev.vel.getNorm()/prev.acc.getNorm())
                            + Math.sqrt(Math.pow(prev.vel.getNorm()/prev.acc.getNorm(), 2) - 2 * (pColl.getNorm() - prev.pos.getNorm()) / prev.acc.getNorm());
                    LOGGER.info("ID={}; Kollision mit ID={} (p{} = {}): Korrekturen: pColl = {}m; vColl = {}m/s",
                            id, b.id, b.id, V3.r(b.pos), V3.r(pColl), Double.isNaN(tColl) ? V3.r(vel) : V3.r(prev.vel.add(tColl, prev.acc)));
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
                .reduce(this, (a, b) -> {
                    // Geschwindigkeit nach Kollision:
                    // v1' = (v1 + 2*m2/(m1+m2) * dot(v2-v1, p1-p2) / |p1-p2| * (p1-p2)) * bounciness
                    // falls b immovable ist, wird kein Massenverhältnis berechnet
                    Vector3D v = a.vel.add((b.movable ? 2*b.mass/(a.mass+b.mass) : 2)
                            * b.vel.subtract(a.vel).dotProduct(a.pos.subtract(b.pos))
                            / a.pos.subtract(b.pos).getNormSq(), a.pos.subtract(b.pos)).scalarMultiply(bounciness);
                    LOGGER.info("ID={}; Kollision mit ID={} (v{} = {}): Impulserhaltung: v' = {}m/s", id, b.id, b.id, V3.r(b.vel), V3.r(v));
                    return new Sphere(id, a.pos, v, a.acc, a.selfAcc, a.movable, a.radius, a.density, a.bounciness);});
    }

    /**
     * Findet alle mit kollidierenden Kugeln, Kollisionsdetektion: Abstand der Mittelpunkte < Summe der Radii
     * @param s Kugel, gegen die die Liste getestet wird
     * @param entities Liste aller Körper
     * @return Stream der getroffenen Kugeln (nicht parallel, da es meistens nur eine Kollision gibt)
     */
    private static Stream<Sphere> getCollidingSpheres(Sphere s, ImmutableList<Shape> entities) {
        return entities.stream().filter(e -> e.type == ShapeType.SPHERE && !e.equals(s) && !s.pos.equals(e.pos))
                // Toleranz (0.1 Nanometer), damit z.B. keine Kollision bei direkt aneinander liegenden Kugeln erkannt wird
                .map(e -> (Sphere) e).filter(e -> s.pos.distance(e.pos) + 1.0e-10 < s.radius + e.radius);
    }

    public Object[] getTypeData() {
        return new Object[]{ radius };
    }
}
