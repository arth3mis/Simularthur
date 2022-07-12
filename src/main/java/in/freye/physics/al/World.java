package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

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
        assert V3.isValidVector(size, gravity) && entities != null : "Die Eigenschaften müssen initialisiert sein";
        assert DoubleStream.of(size.toArray()).allMatch(d -> d > 0) : "Der Raum muss ein realer Quader sein";
        assert updateFrequency > 0 : "Der minimale Update-Schritt muss positiv sein";
        this.updateFreq = updateFrequency;
        this.size = size;
        this.gravity = gravity;
        this.airDensity = airDensity;
        this.entities = entities;
    }

    public Spawner at(Vector3D position) {
        assert V3.compareComponents(position, size, (p, s) -> p >= 0 && p < s) : "Die Position muss im Raum liegen";
        return new Spawner(position);
    }

    public Physicable spawn(Spawnable... entities) {
        assert entities != null : "Liste von Körpern muss existieren";
        World w = this;
        for (Spawnable s : entities)
            w = w.spawn(s);
        return w;
    }

    private World spawn(Spawnable entity) {
        assert entity instanceof Shape : "Körper muss existieren und Instanz von Shape sein";
        // Zwei Körper im Raum dürfen nicht dieselbe ID oder Position haben
        if (entities.anySatisfy(e -> e.equals(entity) || e.pos.equals(entity.getPos())))
            return this;
        return new World(updateFreq, size, gravity, airDensity, entities.newWith((Shape) entity));
    }

    public Physicable replace(long id, Spawnable entity) {
        assert entities.anySatisfy(e -> e.id == id) : "Die Welt muss den Körper mit der angegebenen ID enthalten";
        if (entity == null) return destroy(id);
        assert entity instanceof Shape : "Körper muss existieren und Instanz von Shape sein";
        // Reihung von vorherigen Körpern, dem neuen Körper und nachfolgenden Körpern
        ImmutableList<Shape> pre = entities.takeWhile(e -> e.id != id);
        return new World(updateFreq, size, gravity, airDensity,
                pre.newWith((Shape) entity).newWithAll(entities.drop(pre.size()+1)));
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
        LOGGER.info("Zeitschritt ({}s) wird simuliert.", V3.r(dt));
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
        assert V3.isValidVector(newGravity) : "Gravitation muss in Rechnungen anwendbar sein";
        return new World(updateFreq, size, newGravity, airDensity, entities);
    }

    public Physicable setAirDensity(double newAirDensity) {
        assert newAirDensity >= 0 && Double.isFinite(airDensity) : "Dichte des Mediums im Raum muss eine endliche, positive Größe sein";
        return new World(updateFreq, size, gravity, newAirDensity, entities);
    }

    public Vector3D getSize() { return size; }
    public Vector3D getGravity() { return gravity; }
    public double getAirDensity() { return airDensity; }
    public Shape[] getEntities() { return entities.toArray(new Shape[0]); }
}
