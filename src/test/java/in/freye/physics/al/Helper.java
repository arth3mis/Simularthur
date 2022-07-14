package in.freye.physics.al;

/**
 * Hilfsmethoden zum Berechnen
 */
public class Helper {

    /** Rechnet von Masse und Radius auf die Dichte einer Kugel zurück */
    static double calcSphereDensity(double radius, double mass) {
        // m = density * 4/3 * π * r³
        // density = m * 3/4 / π / r³
        return mass * 3.0/4.0 / Math.PI / radius/radius/radius;
    }
}
