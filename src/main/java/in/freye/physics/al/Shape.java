package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.collections.api.list.ImmutableList;


public abstract class Shape implements Spawnable {

    static final long NO_ID = -1;
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
