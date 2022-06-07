package in.freye.physics.al;

import in.freye.physics.al.fluent.ShapeBuilder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public interface Physicable {

    /** Beginnt die Erstellung eines Körpers */
    ShapeBuilder at(Vector3D position);

    /** Fügt einen Körper in die Welt ein */
    Physicable spawn(Shape entity);
    /** Löscht Körper aus der Welt */
    Physicable destroy(int atIndex);

    /** Simuliert die Änderungen im System, die im Zeitschritt deltaTime (Einheit Sekunde) passieren */
    Physicable update(double deltaTime);

    /** Ändert die Gravitation der Welt */
    Physicable setGravity(Vector3D newGravity);

    /**  */
    Vector3D getSize();
    /**  */
    Vector3D getGravity();
    /**  */
    Shape[] getEntities();

    /**  */
    default boolean isEarthLike() {
        return getGravity().equals(new Vector3D(0, -9.81, 0));
    }
}
