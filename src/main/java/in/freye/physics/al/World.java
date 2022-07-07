package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    /** Dichte des Mediums, das den Raum ausfüllt (Bsp.: 0=Vakuum; 1.2≈Luft), Grundgröße für Strömungswiderstand */
    private final double airDensity;
    /** Liste aller Körper im Raum */
    private final ImmutableList<Shape> entities;

    /** Allgemeine Gravitationskonstante G */
    public static final double GRAVITY_CONSTANT = 6.674e-11;
    /**
     * Masse, ab der ein Körper andere Objekte stark genug anzieht, damit es signifikant für die Berechnung wird.
     * Signifikant := Andere Körper im Abstand = 1 m werden mit mind. 0,001 m/s² beschleunigt.
     * Nach F=G*(m1*m2)/r² (mit F=m2*a und r=1m) folgt m1=a/G*r²
     *  = (0,001m/s²)/(6,674e-11Nm²/kg²)*(1m²) = 14.983.518,13 kg ≈ 1,4984e7 kg
     */
    private static final double GRAVITY_SIGNIFICANT_MASS = 1.4984e7;

    private static final Logger LOGGER = LogManager.getLogger("monitoring");

    /**
     * Factory-Methode:
     * Erstellt eine neue Welt für physikalische Simulation
     * @param updateFrequency Anzahl, wie oft die Welt mindestens pro Sekunde aktualisiert wird
     *                        (Genauigkeitsversprechen des Systems)
     * @param size Größe des Quaders, der die Welt darstellt
     */
    public static Physicable create(double updateFrequency, Vector3D size) {
        return new World(updateFrequency, size, Vector3D.ZERO, 0, Lists.immutable.empty());
    }

    private World(double updateFrequency, Vector3D size, Vector3D gravity, double airDensity, ImmutableList<Shape> entities) {
        assert isValidVector(size, gravity) && entities != null : "Die Eigenschaften müssen initialisiert sein";
        assert DoubleStream.of(size.toArray()).allMatch(d -> d > 0) : "Der Raum muss ein realer Quader sein";
        assert updateFrequency > 0 : "Der minimale Update-Schritt muss positiv sein";
        this.updateFreq = updateFrequency;
        this.size = size;
        this.gravity = gravity;
        this.airDensity = airDensity;
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
        return new World(updateFreq, size, gravity, airDensity, entities.newWith(entity));
    }

    public Physicable replace(long id, Shape entity) {
        assert entities.select(e -> e.id == id).notEmpty() : "Die Welt muss den Körper mit der angegebenen ID enthalten";
        if (entity == null) return destroy(id);
        ImmutableList<Shape> pre = entities.takeWhile(e -> e.id != id);
        return new World(updateFreq, size, gravity, airDensity, pre.newWith(entity).newWithAll(entities.drop(pre.size()+1).dropWhile(e -> e.id != id)));
    }

    /** Löscht Objekt an angegebener Stelle */
    private Physicable destroy(long id) {
        // id wird in replace() abgesichert
        return new World(updateFreq, size, gravity, airDensity, entities.newWithout(entities.select(e -> e.id == id).getAny()));
    }

    public Physicable update(double timeStep) {
        assert Double.isFinite(timeStep) && timeStep >= 0 : "Zeit kann nur endliche Schritte und nicht rückwärts laufen";
        // Wenn eine höhere Update-Frequenz gefordert ist, als timeStep bietet, wird wiederholt aktualisiert
        World world = this;
        for (double dt = timeStep; dt > 0; dt -= 1/updateFreq)
            world = new World(updateFreq, size, gravity, airDensity, world.simulateChanges(Math.min(dt, 1/updateFreq)));
        return world;
    }

    /** Wendet physikalische Berechnungen auf jeden Körper an */
    private ImmutableList<Shape> simulateChanges(double dt) {
        // Filtern aller Körper, deren Masse eine signifikante Gravitation ausübt
        ImmutableList<Shape> gravityShapes = entities
                .select(e1 -> e1.mass >= GRAVITY_SIGNIFICANT_MASS);
        // Berechnung der Gesamtbeschleunigung, die jeder Körper zum neuen Zeitpunkt hat
        ImmutableList<Shape> result1 = Lists.immutable.fromStream(entities.parallelStream()
                .map(e -> e.calcAcceleration(gravity, airDensity, gravityShapes)));
        // Aktualisieren der Position und Geschwindigkeit durch allgemeine Gravitation oder gleichförmige Bewegung
        ImmutableList<Shape> result2 = Lists.immutable.fromStream(result1.parallelStream()
                .map(e -> e.applyMovement(dt)));
        // Kollisionen mit den Wänden (benötigt Zustand vor aktualisierter Position/Geschwindigkeit)
        ImmutableList<Shape> result3 = Lists.immutable.fromStream(result2.parallelStream()
                .map(e -> e.handleWallCollision(size, result1.select(e1 -> e1.equals(e)).getAny())));
        // Kollision zwischen Körpern (Korrektur Position/Geschwindigkeit)
        ImmutableList<Shape> result4 = Lists.immutable.fromStream(result3.parallelStream()
                .map(e -> e.calcEntityCollisionCorrections(result3, result1.select(e1 -> e1.equals(e)).getAny())));
        // Kollision zwischen Körpern (Kollisionsantwort mit Impulserhaltung, Energieerhaltung)
        return Lists.immutable.fromStream(result4.parallelStream()
                .map(e -> e.applyEntityCollisionDeflections(result3, result4)));
    }

    public Physicable setGravity(Vector3D newGravity) {
        assert isValidVector(newGravity) : "Gravitation muss in Rechnungen anwendbar sein";
        return new World(updateFreq, size, newGravity, airDensity, entities);
    }

    public Physicable setAirDensity(double newAirDensity) {
        assert newAirDensity >= 0 && Double.isFinite(airDensity) : "Dichte des Mediums im Raum muss eine endliche, positive Größe sein";
        return new World(updateFreq, size, gravity, newAirDensity, entities);
    }

    public Vector3D getSize() {
        return size;
    }

    public Vector3D getGravity() {
        return gravity;
    }

    public double getAirDensity() {
        return airDensity;
    }

    public Shape[] getEntities() {
        return entities.toArray(new Shape[0]);
    }

    /** Testet den Vektor, dass er nicht null ist und keine NaN/Infinity-Werte enthält */
    static boolean isValidVector(Vector3D... v) {
        return v != null && v.length > 0 && Arrays.stream(v).flatMapToDouble(u -> Arrays.stream(u.toArray())).allMatch(Double::isFinite);
    }

    /** Testet jede Komponente des ersten Vektors gegen die des zweiten */
    static boolean compareComponents(Vector3D v, Vector3D w, BiFunction<Double, Double, Boolean> bf) {
        assert v != null && w != null : "Zum komponentenweisen Testen darf kein Vektor null sein";
        return bf.apply(v.getX(), w.getX()) && bf.apply(v.getY(), w.getY()) && bf.apply(v.getZ(), w.getZ());
    }
}
