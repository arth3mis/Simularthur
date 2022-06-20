package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/** Hilfsklasse für Vector3D */
public class V3 {
    /** Berechnet den Parallelteil a_parallel der orthogonalen Zerlegung von a längs b */
    public static Vector3D project(Vector3D a, Vector3D b) {
        return b.scalarMultiply(a.dotProduct(b) / b.getNormSq());
    }
}
