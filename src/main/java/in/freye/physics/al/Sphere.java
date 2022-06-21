package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.collections.api.list.ImmutableList;

import java.util.stream.Stream;

public class Sphere extends Shape {

    public final double radius;

    Sphere(Vector3D pos, Vector3D vel, Vector3D selfAcc, boolean movable, double radius, double density, double bounciness) {
        this(Shape.NO_ID, pos, vel, Vector3D.ZERO, selfAcc, movable, radius, density, bounciness);
    }

    private Sphere(long id, Vector3D pos, Vector3D vel, Vector3D acc, Vector3D selfAcc, boolean movable, double radius, double density, double bounciness) {
        super(id, ShapeType.SPHERE, pos, vel, acc, selfAcc, movable, density * 4.0/3.0 * Math.PI * radius * radius * radius, density, bounciness);
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
        // todo trace pColl like with entities (nicht pos._ fixen sondern ganzen vektor)
        // Jede Komponente einzeln auf Kollision testen
        for (int i = 0; i < 3; i++) {
            if (pos.toArray()[i] < radius || pos.toArray()[i] + radius > worldSize.toArray()[i]) {
                // Position korrigieren, falls außerhalb des Bereichs
                double pColl = pos.toArray()[i] < radius ? radius : worldSize.toArray()[i] - radius;
                p = new Vector3D(i==0 ? pColl : p.getX(), i==1 ? pColl : p.getY(), i==2 ? pColl : p.getZ());
                // Überschrittene Position kann auch zu überschrittener Geschwindigkeit führen, die korrigiert werden muss:
                // Mithilfe des "prev" Zustands kann die tatsächliche Kollisionszeit berechnet werden (Umstellung mit PQ-Formel).
                // pColl = 0.5*a*tColl² + v*tColl + p  (p,v,a sind Werte von prev)
                // (PQ-Formel) => tColl = -(v/a) + sqrt((v/a)² - 2(p-pColl)/a)
                double tColl = -(prev.vel.toArray()[i] / prev.acc.toArray()[i]) + Math.sqrt(Math.pow((prev.vel.toArray()[i] / prev.acc.toArray()[i]), 2) - 2 * (prev.pos.toArray()[i] - pColl) / prev.acc.toArray()[i]);
                double v1 = prev.vel.add(tColl, prev.acc).toArray()[i] * -bounciness;
                // Schwelle, um Zittern zu vermeiden todo keep or remove?
                if (Math.abs(v1) < 0.00001) v1 = 0;
                // Geschwindigkeitskomponente invertieren
                v = new Vector3D(i==0 ? v1 : v.getX(), i==1 ? v1 : v.getY(), i==2 ? v1 : v.getZ());
                // Eigenbeschleunigungskomponente auf 0 setzen
                // (damit Eigenbeschleunigung nicht wie eine Gravitation, sondern eher wie ein Antrieb/Triebwerk funktioniert)
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
                // todo check with new method structure
                Vector3D d = a.pos.subtract(b.pos);  // Zeigt in Richtung von "this"
                // Korrekte Position bei Kollision: Korrigiert die Hälfte des Abstands, andere Kugel "übernimmt" die andere Hälfte (wenn sie auch movable ist)
                Vector3D pColl = a.pos.add(d.normalize().scalarMultiply(a.radius + b.radius - d.getNorm()).scalarMultiply(b.movable ? 0.5 : 1));
                // tColl = -(v/a) + sqrt((v/a)² - 2(p-pColl)/a)
                double tColl = -(prev.vel.getNorm()/prev.acc.getNorm()) + Math.sqrt(Math.pow(prev.vel.getNorm()/prev.acc.getNorm(), 2) - 2 * (prev.pos.getNorm() - pColl.getNorm()) / prev.acc.getNorm());
                return new Sphere(id, pColl,
                    prev.vel.add(tColl, prev.acc),
                    // orthogonale Zerlegung von selfAcc: der Teil parallel zum Vektor (b.pos-a.pos) wird 0 (-> Hintergrund: siehe handleWallCollision)
                    a.acc, a.selfAcc,//a.selfAcc.subtract(b.pos.subtract(a.pos).scalarMultiply(a.selfAcc.dotProduct(b.pos.subtract(a.pos)) / b.pos.subtract(a.pos).getNormSq())),  // todo test and think through
                    a.movable, a.radius, a.density, a.bounciness);
            });
    }

    Shape applyEntityCollisionDeflections(ImmutableList<Shape> deflectionEntities, ImmutableList<Shape> detectEntities/*, Shape prev*/) {
        if (!movable) return this;
        return getCollidingSpheres(this, detectEntities).map(e -> (Sphere) deflectionEntities.select(e::equals).getAny())
                // Geschwindigkeit nach Kollision:
                // v1' = v1 - 2*m2/(m1+m2) * todo insert formula
                .reduce(this, (a, b) -> {
                    Vector3D d = b.pos.subtract(a.pos);
                    return new Sphere(id, a.pos, a.vel.add(2*b.mass/(a.mass+b.mass) * b.vel.subtract(a.vel).dotProduct(d) / d.getNormSq(), d), a.acc, a.selfAcc, a.movable, a.radius, a.density, a.bounciness);
                });
    }

    /**
     * Findet alle mit kollidierenden Kugeln, Kollisionsdetektion: Abstand der Mittelpunkte < Summe der Radii
     * @param s Kugel, gegen die die Liste getestet wird
     * @param entities Liste aller Körper
     * @return Stream der getroffenen Kugeln (nicht parallel, da es meistens nur eine Kollision gibt)
     */
    private static Stream<Sphere> getCollidingSpheres(Sphere s, ImmutableList<Shape> entities) {
        return entities.stream().filter(e -> e.type == ShapeType.SPHERE && !e.equals(s) && !s.pos.equals(e.pos)).map(e -> (Sphere) e).filter(e -> s.pos.distance(e.pos) < s.radius + e.radius);
    }
}
