package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.collections.api.list.ImmutableList;


public abstract class Shape {

    static final long NO_ID = -1;
    private static long idCounter = 0;
    /**
     * ID des Körpers. Wird in equals() genutzt, um zu testen,
     * ob es sich bei zwei Objekten um Manipulationen desselben ursprünglichen Körpers handelt
     */
    public final long id;

    /** Geometrische Form, entscheidend für Kollisionsdetektion */
    public final ShapeType type;
    /** pos: Position; vel: Geschwindigkeit; acc: Beschleunigung (nur eigene, keine externe) */
    public final Vector3D pos, vel, acc;
    /** Eigenschaft, ob sich die Position durch Bewegungsgleichungen verändern darf */
    final boolean movable;
    /** mass: Masse; density: Dichte; bounciness: Reflexionsstärke bei Kollision */
    final double mass, density, bounciness;

    /**
     * Die ID sollte nur in privaten Konstruktoren als Parameter verfügbar sein,
     * damit nur Manipulationen desselben Objekts dieselbe ID haben, keine neuen Körper
     */
    Shape(long id, ShapeType type, Vector3D pos, Vector3D vel, Vector3D acc, boolean movable, double mass, double density, double bounciness) {
        assert World.isValidVector(pos) && World.isValidVector(vel) && World.isValidVector(acc) : "Die Position & Bewegung des Körpers muss definiert sein";
        assert density > 0 && mass > 0 : "Dichte & Masse eines Körpers müssen positiv sein";
        assert bounciness >= 0 && bounciness <= 1 : "Die Reflexionsstärke muss zwischen 0 und 1 liegen, damit die Energieerhaltung nicht verletzt wird";
        // Wenn es ein neuer Körper ist, weise eine neue ID zu; ansonsten kopiere die bisherige ID
        this.id = id == NO_ID ? idCounter++ : id;
        this.type = type;
        this.pos = pos;
        this.vel = vel;
        this.acc = acc;
        this.movable = movable;
        this.mass = mass;
        this.density = density;
        this.bounciness = bounciness;
    }

    abstract Shape applyMovement(double dt, Vector3D gravity);
    abstract Shape handleWallCollision(Vector3D worldSize);
    abstract Shape handleEntityCollision(ImmutableList<Shape> entities);

    //abstract Shape copy();

    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Shape that)) return false;  // hier wird implizit auch "obj == null" überprüft
        return this.id != NO_ID && this.id == that.id;
    }
}
