package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.Arrays;
import java.util.function.BiFunction;

/**
 * Hilfsklasse für Vektoren
 */
public class V3 {
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
