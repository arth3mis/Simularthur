package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.DoubleStream;

public class World implements Physicable {

    /** Minimale Aktualisierungen pro Sekunde */
    private final double updateFreq;
    /** size: Größe des Raums; gravity: Vektor der Beschleunigung eines homogenen Gravitationsfelds (wie z.B. auf der Erde) */
    private final Vector3D size, gravity;
    /** Liste aller Körper im Raum */
    private final ImmutableList<Shape> entities;

    /** Allgemeine Gravitationskonstante G */
    static final double GRAVITY_CONSTANT = 6.674e-11;
    /**
     * Masse, ab der ein Körper andere Objekte stark genug anzieht, damit es signifikant für die Berechnung wird.
     * Signifikant := Andere Körper im Abstand = 1 m werden mit mind. 0,001 m/s² beschleunigt.
     * Nach F=G*(m1*m2)/r² (mit F=m2*a und r=1m) folgt m1=a/G*r²
     *  = (0,001m/s²)/(6,674e-11Nm²/kg²)*(1m²) = 14.983.518,13 kg ≈ 1,5e7 kg
     */
    private static final double GRAVITY_SIGNIFICANT_MASS = 1.5e7;

    /**
     * Erstellt eine neue Welt für physikalische Simulation
     * @param updateFrequency Anzahl, wie oft die Welt mindestens pro Sekunde aktualisiert wird
     *                        (Genauigkeitsgarantie des Systems)
     * @param size Größe des Quaders, der die Welt darstellt
     */
    public static Physicable create(double updateFrequency, Vector3D size) {
        return new World(updateFrequency, size, Vector3D.ZERO, Lists.immutable.empty());
    }

    private World(double updateFrequency, Vector3D size, Vector3D gravity, ImmutableList<Shape> entities) {
        assert isValidVector(size) && isValidVector(gravity) && entities != null : "Die Werte müssen initialisiert sein";
        assert DoubleStream.of(size.toArray()).allMatch(d -> d > 0) : "Der Raum muss ein realer Quader sein";
        this.updateFreq = updateFrequency;
        this.size = size;
        this.gravity = gravity;
        this.entities = entities;
    }

    public ShapeBuilder at(Vector3D position) {
        assert compareComponents(position, size, (d1, d2) -> d1 >= 0 && d1 < d2) : "Die Position muss im Raum liegen";
        return new ShapeBuilder(position);
    }

    public Physicable spawn(Shape... entities) {
        assert entities != null : "Liste von Körpern muss existieren";
        World w = this;
        for (Shape shape : entities)
            if (w.entities.allSatisfy(e -> !e.equals(shape) && !e.pos.equals(shape.pos)))  // Zwei Körper dürfen nicht dieselbe ID oder Position haben
                w = w.spawn(shape);
        return w;
    }

    private World spawn(Shape entity) {
        assert entity != null : "Kein Element kann auch nicht spawnen";
        return new World(updateFreq, size, gravity, entities.newWith(entity));
    }

    public Physicable replace(int atIndex, Shape entity) {
        assert atIndex >= 0 && atIndex < entities.size() : "Der Index muss in [0;Anzahl_Entities_in_Welt[ liegen";
        if (entity == null) return destroy(atIndex);
        return new World(updateFreq, size, gravity, entities.take(atIndex).newWith(entity).newWithAll(entities.drop(atIndex)));
    }

    /** Löscht Objekt an angegebener Stelle */
    private Physicable destroy(int atIndex) {
        return new World(updateFreq, size, gravity, entities.newWithout(entities.get(atIndex)));
    }

    public Physicable update(double timeStep) {
        assert Double.isFinite(timeStep) && timeStep >= 0 : "Zeit kann nur endliche Schritte und nicht rückwärts laufen";
        // Wenn eine höhere Update-Frequenz gefordert ist, als timeStep bietet, wird wiederholt aktualisiert
        World world = this;
        for (double dt = timeStep; dt > 0; dt -= 1/updateFreq)
            world = new World(updateFreq, size, gravity, world.simulateChanges(Math.min(dt, 1/updateFreq)));
        return world;

            /*return Stream.iterate((Physicable) this, w -> w.update(1/updateFreq))
                    .limit((int) (timeStep * updateFreq))
                    .reduce(this, (v, w) -> w)
                    .update(timeStep % (1/updateFreq));*/
//        return update(timeStep % (1/updateFreq)).update((int) (timeStep * updateFreq));
//        Physicable p = IntStream.range(0, (int) (timeStep * updateFreq)).boxed().reduce((Physicable) this, (w, x) -> w.update(1/updateFreq), (a, b) -> b);
//        Physicable q = Stream.iterate((Physicable) this, w -> w.update(1/updateFreq)).limit((int) (timeStep * updateFreq)).reduce(this, (a, b) -> b);
//        Physicable r = Stream.generate(() -> (Physicable) this).limit((int) (timeStep * updateFreq)).reduce(this, (w, x) -> w.update(1/updateFreq));

    }

//    /**  */
//    private World update(int n) {
//        if (n == 0) return this;
//        return ((World) update(1/updateFreq)).update(n - 1);
//    }

    /** Wendet physikalische Berechnungen auf jeden Körper an */
    private ImmutableList<Shape> simulateChanges(double dt) {
        // todo check if parallel map ops change order in stream or returned list -> seems not to be the case
        // Filtern aller Körper, deren Masse eine signifikante Gravitation ausübt
        ImmutableList<Shape> gravityShapes = entities
                .select(e1 -> e1.mass >= GRAVITY_SIGNIFICANT_MASS);
        // Berechnung der Gesamtbeschleunigung, die jeder Körper zum neuen Zeitpunkt hat
        ImmutableList<Shape> result1 = Lists.immutable.fromStream(entities.stream().parallel()
                .map(e -> e.calcAcceleration(gravity, gravityShapes)));
        // Aktualisieren der Position und Geschwindigkeit durch allgemeine Gravitation oder gleichförmige Bewegung
        ImmutableList<Shape> result2 = Lists.immutable.fromStream(result1.stream().parallel()
                .map(e -> e.applyMovement(dt)));
        // Kollisionen mit den Wänden (benötigt Zustand vor aktualisierter Position/Geschwindigkeit)
        ImmutableList<Shape> result3 = Lists.immutable.fromStream(result2.stream().parallel()
                .map(e -> e.handleWallCollision(size, result1.select(e1 -> e1.equals(e)).get(0))));
        // Kollision zwischen Körpern
        //todo pass res1.e or res2.e for vel bugfix? -> res2 prev is already in list, if i use this i dont need another arg
        // i think res2 is fine, since only posDiff is used to calc correction
        return Lists.immutable.fromStream(result3.stream().parallel()
                .map(e -> e.handleEntityCollision(result3)));
    }





    public Physicable setGravity(Vector3D newGravity) {
        assert isValidVector(newGravity) : "Gravitation muss in Rechnungen anwendbar sein";
        return new World(updateFreq, size, newGravity, entities);
    }

    public Vector3D getSize() {
        return size;
    }

    public Vector3D getGravity() {
        return gravity;
    }

    public Shape[] getEntities() {
        return entities.toArray(new Shape[0]);
    }

    /** Testet den Vektor, dass er nicht null ist und keine NaN/Infinity-Werte enthält */
    static boolean isValidVector(Vector3D v) {
        return v != null && DoubleStream.of(v.toArray()).allMatch(Double::isFinite);
    }

    /** Testet jede Komponente des ersten Vektors gegen die des zweiten */
    static boolean compareComponents(Vector3D v, Vector3D w, BiFunction<Double, Double, Boolean> bf) {
        assert v != null && w != null : "Zum komponentenweisen Testen darf kein Vektor null sein";
        return bf.apply(v.getX(), w.getX()) && bf.apply(v.getY(), w.getY()) && bf.apply(v.getZ(), w.getZ());
    }
}
