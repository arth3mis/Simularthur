// Gesamte AL in einer Datei
// Codezeilen (nach cloc v1.92): 318
//
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.*;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.*;


final Logger LOGGER = LogManager.getLogger("monitoring");


/**
 * Interface für die Implementierung einer physikalischen Simulation.
 * Die Datenstruktur ist immutabel aufgebaut, jede Manipulation des simulierten Raums oder der enthaltenen Objekte
 * wird in neuen Objekten abgebildet.
 */
interface Physicable {
    /** Beginnt die Erstellung eines Körpers */
    Spawner createSpawnableAt(Vector3D position);

    /** Fügt einen oder mehrere Körper in die Welt ein */
    Physicable spawn(Spawnable... entities);
    /** Ersetzt Körper in der Welt (löscht, wenn null übergeben wird) */
    Physicable replace(long id, Spawnable entity);

    /** Simuliert die Änderungen im System, die im Zeitschritt deltaTime (Einheit Sekunde) passieren */
    Physicable simulateTime(double deltaTime);

    /** Ändert die Beschleunigung durch allgemeine Gravitation des Raums */
    Physicable setGravity(Vector3D newGravity);
    /** Ändert die Dichte des Mediums im Raum */
    Physicable setAirDensity(double newAirDensity);

    /** Gibt die Größe des simulierten Raums zurück */
    Vector3D getSize();
    /** Gibt die Gravitation des simulierten Raums zurück */
    Vector3D getGravity();
    /** Gibt die Dichte des raumfüllenden Mediums zurück */
    double getAirDensity();
    /** Gibt ein Array der im Raum vorhandenen Körper zurück */
    Spawnable[] getEntities();

    /** Prüft, ob die Gravitation mit der Erdanziehung übereinstimmt */
    default boolean isEarthLike() {
        return getGravity().equals(new Vector3D(0, -9.81, 0)) && getAirDensity() == 1.2;
    }

    /**
     * Generiert eine zufällige Position innerhalb des simulierten Raums
     * @param minDistToWall Jede Komponente liegt im Intervall [minDistToWall; size.get_X|Y|Z_() - minDistToWall[
     */
    default Vector3D randomPos(double minDistToWall) {
        return new Vector3D(IntStream.range(0,3).mapToDouble(i -> minDistToWall + Math.random() * (getSize().toArray()[i] - 2 * minDistToWall)).toArray());
    }
}


interface SpawnerBase {
    /**
     * Erstellt eine neue Kugel
     * @param radius Radius der Kugel
     * @param materialDensity Dichte des Materials, daraus wird mit dem Volumen die Masse berechnet
     * @param bounciness Dämpfungs- bzw. Reflexions-Faktor für Geschwindigkeit bei Kollisionen
     */
    Spawnable ofTypeSphere(double radius, double materialDensity, double bounciness);
}


interface Spawner extends SpawnerBase {
    SpawnerBase withVelocityAndAccel(Vector3D velocity, Vector3D acceleration);
    SpawnerImmovable immovable();
}


interface SpawnerImmovable {
    Spawnable ofTypeSphere(double radius, double materialDensity);
}


interface Spawnable {
    long getId();
    ShapeType getType();
    Vector3D getPos();
    Vector3D getVel();
    Vector3D getAcc();
    Vector3D getSelfAcc();
    boolean getMovable();
    double getMass();
    double getDensity();
    double getBounciness();

    /**
     * Varies between shape types.
     * SPHERE: { (double) radius }
     */
    Object[] getTypeData();
}


enum ShapeType {
    // Kugeln haben keinen eindeutigen cw-Wert.
    // Ich nehme hier eine ideale Flüssigkeit (ohne Viskosität) an, daher führt der Wert 0.1 zu guten Simulationen.
    SPHERE(0.1);

    /** Strömungswiderstandskoeffizient, Faktor in Formel für Strömungswiderstand */
    public final double dragCoefficient;

    ShapeType(double cw) {
        dragCoefficient = cw;
    }
}


///////////////////////////////////////////////////////////////////////////////////////////////////////////
// Implementierung der Interfaces

class World implements Physicable {
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

    public Spawner createSpawnableAt(Vector3D position) {
        assert V3.compareComponents(position, size, (p, s) -> p >= 0 && p < s) : "Die Position muss im Raum liegen";
        return new WorldSpawner(position);
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
        return new World(updateFreq, size, gravity, airDensity, pre.newWith((Shape) entity).newWithAll(entities.drop(pre.size()+1)));
    }

    /** Löscht Objekt an angegebener Stelle */
    private Physicable destroy(long id) {
        // id wird in replace() abgesichert
        return new World(updateFreq, size, gravity, airDensity, entities.newWithout(entities.select(e -> e.id == id).getAny()));
    }

    public Physicable simulateTime(double timeStep) {
        assert Double.isFinite(timeStep) && timeStep >= 0 : "Zeit kann nur endliche Schritte und nicht rückwärts laufen";
        // Wenn eine höhere Update-Frequenz gefordert ist, als timeStep bietet, wird wiederholt aktualisiert
        World world = this;
        for (double dt = timeStep; dt > 0; dt -= 1/updateFreq)
            world = new World(updateFreq, size, gravity, airDensity, world.calculateChanges(Math.min(dt, 1/updateFreq)));
        return world;
    }

    /** Wendet physikalische Berechnungen auf jeden Körper an */
    private ImmutableList<Shape> calculateChanges(double dt) {
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


abstract class Shape implements Spawnable {
    /** Definiert eine (noch) nicht vorhandene ID */
    public static final long NO_ID = -1;
    private static long idCounter = 0;
    /**
     * ID des Körpers. Wird in equals() genutzt, um zu testen,
     * ob es sich bei zwei Objekten um Manipulationen desselben ursprünglichen Körpers handelt
     */
    protected final long id;

    /** Geometrische Form, entscheidend für Kollisionsdetektion */
    protected final ShapeType type;
    /** pos: Position; vel: Geschwindigkeit; acc: Gesamtbeschleunigung; selfAcc: Eigenbeschleunigung */
    protected final Vector3D pos, vel, acc, selfAcc;
    /** Eigenschaft, ob sich die Position durch Bewegungsgleichungen verändern darf */
    protected final boolean movable;
    /** mass: Masse; density: Dichte; bounciness: Reflexionsstärke bei Kollision */
    protected final double mass, density, bounciness;

    /**
     * Die ID sollte nur in privaten Konstruktoren als Parameter verfügbar sein,
     * damit nur Manipulationen desselben Objekts dieselbe ID haben, keine neuen Körper
     */
    Shape(long id, ShapeType type, Vector3D pos, Vector3D vel, Vector3D acc, Vector3D selfAcc, boolean movable, double mass, double density, double bounciness) {
        assert V3.isValidVector(pos, vel, acc, selfAcc) : "Die Position & Bewegung des Körpers muss reell definiert sein";
        assert density > 0 && mass > 0 && Double.isFinite(density) && Double.isFinite(mass) : "Dichte & Masse eines Körpers müssen endlich positiv sein";
        assert bounciness >= 0 && bounciness <= 1 : "Die Reflexionsstärke muss zwischen 0 und 1 liegen, damit die Energieerhaltung nicht verletzt wird";
        // Wenn es ein neuer Körper ist, weise eine neue ID zu; ansonsten kopiere die bisherige ID
        this.id = id == NO_ID ? idCounter++ : id;
        this.type = type;
        this.pos = pos;
        this.vel = vel;
        this.acc = acc;
        this.selfAcc = selfAcc;
        this.movable = movable;
        this.mass = mass;
        this.density = density;
        this.bounciness = bounciness;
    }

    /**
     * Addiert eigene und externe Beschleunigung
     * @param gravity Allgemeine Gravitation
     * @param airDensity Dichte des Mediums im Raum
     * @param gravityEntities Liste der anderen Körper im System, die durch Gravitation andere Körper beschleunigen
     */
    abstract Shape calcAcceleration(Vector3D gravity, double airDensity, ImmutableList<Shape> gravityEntities);
    /**
     * Wendet folgende Formeln an:
     * pos = 0.5 * acc * dt² + vel * dt;
     * vel = acc * dt;
     * @param dt Zeitdifferenz der Änderung
     */
    abstract Shape applyMovement(double dt);
    /**
     * Erkennt Kollisionen mit den Wänden des Raums, berechnet Korrekturen und Reflexion.
     * @param worldSize Raumgröße
     * @param prev Zustand vor Bewegungsupdate (benötigt für Korrekturen)
     */
    abstract Shape handleWallCollision(Vector3D worldSize, Shape prev);
    /**
     * Berechnet die exakten Kollisionszeiten, korrigiert Position und Geschwindigkeit
     * //@param entities alle Körper im Raum
     * @param prev Zustand vor Bewegungsupdate
     */
    abstract Shape calcEntityCollisionCorrections(ImmutableList<Shape> correctionEntities, Shape prev);
    /**
     * Berechnet die neue Geschwindigkeit nach Kollisionen, basierend auf Impuls- und Energieerhaltung
     * @param detectEntities Körper im Raum (vor Korrektur)
     * @param deflectionEntities Körper im Raum (nach Korrektur)
     */
    abstract Shape applyEntityCollisionDeflections(ImmutableList<Shape> detectEntities, ImmutableList<Shape> deflectionEntities/*, Shape prev*/);

    // Getter-Methoden
    public long getId() { return id; }
    public ShapeType getType() { return type; }
    public Vector3D getPos() { return pos; }
    public Vector3D getVel() { return vel; }
    public Vector3D getAcc() { return acc; }
    public Vector3D getSelfAcc() { return selfAcc; }
    public boolean getMovable() { return movable; }
    public double getMass() { return mass; }
    public double getDensity() { return density; }
    public double getBounciness() { return bounciness; }

    /** Derselbe Körper kann zu unterschiedlichen Zeitpunkten existieren, deshalb ist er nur mit einer ID eindeutig identifizierbar */
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Shape that)) return false;  // hier wird implizit auch "obj == null" überprüft
        return this.id != NO_ID && this.id == that.id;
    }
}


class Sphere extends Shape {
    private final double radius;

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
                // Wenn Berechnung keinen reellen Wert ergibt, drehe Vorzeichen von (p-pColl)
                // tColl = -(v/a) + sqrt((v/a)² - 2(pColl-p)/a)
                if (Double.isNaN(tColl)) tColl = -(prev.vel.getNorm()/prev.acc.getNorm())
                        + Math.sqrt(Math.pow(prev.vel.getNorm()/prev.acc.getNorm(), 2) - 2 * (pColl - prev.pos.getNorm()) / prev.acc.getNorm());
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


abstract class WorldSpawnerBase implements SpawnerBase {
    protected Vector3D pos = Vector3D.ZERO, vel = Vector3D.ZERO, acc = Vector3D.ZERO;
    protected boolean movable = true;

    public Spawnable ofTypeSphere(double radius, double materialDensity, double bounciness) {
        return new Sphere(pos, vel, acc, movable, radius, materialDensity, bounciness);
    }
}


class WorldSpawner extends WorldSpawnerBase implements Spawner {
    public WorldSpawner(Vector3D pos) {
        this.pos = pos;
    }

    public SpawnerBase withVelocityAndAccel(Vector3D velocity, Vector3D acceleration) {
        return new WorldSpawnerMovable(pos, velocity, acceleration);
    }

    public SpawnerImmovable immovable() {
        return new WorldSpawnerImmovable(pos);
    }
}


class WorldSpawnerMovable extends WorldSpawnerBase implements SpawnerBase {
    WorldSpawnerMovable(Vector3D pos, Vector3D vel, Vector3D acc) {
        this.pos = pos;
        this.vel = vel;
        this.acc = acc;
    }
}


class WorldSpawnerImmovable extends WorldSpawnerBase implements SpawnerImmovable {
    WorldSpawnerImmovable(Vector3D pos) {
        this.pos = pos;
        movable = false;
    }

    public Spawnable ofTypeSphere(double radius, double materialDensity) {
        return super.ofTypeSphere(radius, materialDensity, 0);
    }
}


/**
 * Hilfsklasse für Vektoren
 */
class V3 {
    /** Testet den Vektor, dass er nicht null ist und keine NaN/Infinity-Werte enthält */
    public static boolean isValidVector(Vector3D... v) {
        return v != null && v.length > 0 && Arrays.stream(v).flatMapToDouble(u -> Arrays.stream(u.toArray())).allMatch(Double::isFinite);
    }

    /** Testet jede Komponente des ersten Vektors gegen die des zweiten */
    public static boolean compareComponents(Vector3D v, Vector3D w, BiFunction<Double, Double, Boolean> bf) {
        assert v != null && w != null : "Zum komponentenweisen Testen darf kein Vektor null sein";
        return bf.apply(v.getX(), w.getX()) && bf.apply(v.getY(), w.getY()) && bf.apply(v.getZ(), w.getZ());
    }

    /** Rundet Vektor für übersichtlichere Darstellung im Logging */
    static Vector3D r(Vector3D v) {
        return new Vector3D(Arrays.stream(v.toArray()).map(V3::r).toArray());
    }

    /** Rundet Dezimalzahl auf 3 Nachkommastellen */
    static double r(double d) {
        return Math.round(d*1000)/1000.0;
    }
}
