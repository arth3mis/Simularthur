package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.stream.IntStream;

/**
 * Interface für die Implementierung einer physikalischen Simulation.
 * Die Datenstruktur ist immutabel aufgebaut, jede Manipulation des simulierten Raums oder der enthaltenen Objekte
 * wird in neuen Objekten abgebildet.
 */
public interface Physicable {

    /** Beginnt die Erstellung eines Körpers */
    ShapeBuilder at(Vector3D position);

    /** Fügt einen oder mehrere Körper in die Welt ein */
    Physicable spawn(Shape... entities);
    /** Ersetzt Körper in der Welt (löscht, wenn null übergeben wird) */
    Physicable replace(int atIndex, Shape entity);

    /** Simuliert die Änderungen im System, die im Zeitschritt deltaTime (Einheit Sekunde) passieren */
    Physicable update(double deltaTime);

    /** Ändert die Beschleunigung durch allgemeine Gravitation der Welt */
    Physicable setGravity(Vector3D newGravity);

    /** Gibt die Größe des simulierten Raums zurück */
    Vector3D getSize();
    /** Gibt die Gravitation des simulierten Raums zurück */
    Vector3D getGravity();
    /** Gibt ein Array der im Raum vorhandenen Körper zurück */
    Shape[] getEntities();

    /** Prüft, ob die Gravitation mit der Erdanziehung übereinstimmt */
    default boolean isEarthLike() {
        return getGravity().equals(new Vector3D(0, -9.81, 0));
    }

    /**
     * Generiert eine zufällige Position innerhalb des simulierten Raums
     * @param minDistToWall Jede Komponente liegt im Intervall [minDistToWall; size.get_X|Y|Z_() - minDistToWall[
     */
    default Vector3D randomPos(double minDistToWall) {
        return new Vector3D(IntStream.range(0,3).mapToDouble(i -> minDistToWall + Math.random() * (getSize().toArray()[i] - 2 * minDistToWall)).toArray());
    }
}
