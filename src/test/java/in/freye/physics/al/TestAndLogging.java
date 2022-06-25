package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PhysicableTest {

    /**
     * Basisraum für Tests (Größe 10m * 10m * 10m).
     * Besitzt ein beispielhaftes Genauigkeitsversprechen von 60 Hertz, d.h. minimal wird ein Zeitschritt von 1/60s simuliert.
     */
    Physicable world = World.create(60, new Vector3D(10,10,10));

    /**
     * Überprüft, ob allgemeine Gravitation des Raums korrekt auf Position und Geschwindigkeit eines Objekts wirkt
     */
    @Test
    void worldGravity() {
        // Gravitation definieren
        Physicable w1 = world.setGravity(new Vector3D(0, -9.81, 0));  // erdähnlich

        // Kugel erschaffen und Zeit um 1s fortschreiten
        Physicable w2 = w1
                .spawn(w1.at(new Vector3D(5,8,5)).newSphere(1, 1, 0))
                .update(1);

        // Runden mit 1 Nanometer Präzision
        int precision = 9;

        // Nur y-Werte sollten sich ändern
        assertAll(
                // pY(t)  = 0.5 * aY           * t²   + vY_0 * t  + pY_0
                // pY(1s) = 0.5 * (-9.81m/s²) * (1s)² + 0m/s * 1s + 8m
                //        = 3.095m
                () -> assertArrayEquals(
                        new Vector3D(5, 3.095, 5).toArray(),
                        roundAll(w2.getEntities()[0].pos.toArray(), precision)),
                // vY(t)  = aY          * t
                // vY(1s) = (-9.81m/s²) * 1s
                //        = -9.81m/s
                () -> assertArrayEquals(
                        new Vector3D(0, -9.81, 0).toArray(),
                        roundAll(w2.getEntities()[0].vel.toArray(), precision))
        );
    }

    /**
     * Überprüft:
     *  - Ob die Kollision mit den Wänden korrekt funktioniert
     *  - Positions- und Geschwindigkeitskorrektur bei einer Kollision mit Beschleunigung
     *  - Korrekte Anwendung reduzierter Reflexionsstärke
     */
    @Test
    void wallCollision() {
        // Gravitation definieren
        Physicable w1 = world.setGravity(new Vector3D(1,0,0));

        // Kugel mit 75% Reflexionsstärke erschaffen und 1s Zeitschritt gehen
        Physicable w2 = w1
                .spawn(w1.at(new Vector3D(8.5,5,5)).newSphere(1, 1, 0.75))
                .update(1);

        // Runden mit 1 Nanometer Präzision
        int precision = 9;

        // Nur x-Werte sollten sich ändern
        assertAll(
                // Kollision nach 1s (Radius = 1m -> pX = 9m führt zu Kollision mit Wand bei x = 10m):
                // pX(1s) = 0.5 * 1m/s² * (1s)² + 0m/s * 1s + 8.5m
                //        = 9m
                () -> assertArrayEquals(
                        new Vector3D(9, 5, 5).toArray(),
                        roundAll(w2.getEntities()[0].pos.toArray(), precision)),
                // vX(1s) = 1m/s² * 1s
                //        = 1m/s
                // (75% reflektiert, also Faktor -0.75)
                // vX' = -0.75 * 1m/s
                //     = -0.75m/s
                () -> assertArrayEquals(
                        new Vector3D(-0.75, 0, 0).toArray(),
                        roundAll(w2.getEntities()[0].vel.toArray(), precision))
        );
    }

    @Test
    void entityCollision() {
    }

    @Test
    void entityGravity() {
    }

    double[] roundAll(double[] nums, int precision) {
        return Arrays.stream(nums).map(d -> round(d, precision)).toArray();
    }

    double round(double num, int precision) {
        DecimalFormat df = new DecimalFormat("#." + "#".repeat(precision), new DecimalFormatSymbols(Locale.ENGLISH));
        df.setRoundingMode(RoundingMode.HALF_UP);
        return Double.parseDouble(df.format(num));
    }
}


class SphereTest {

    @Test
    void calcAcceleration() {
    }

    @Test
    void applyMovement() {
    }

    @Test
    void handleWallCollision() {
    }

    @Test
    void calcEntityCollisionCorrections() {
    }

    @Test
    void applyEntityCollisionDeflections() {
    }
}


class Logging {

    private static final Logger LOGGER = LogManager.getLogger("monitoring");

    public static void main(String[] args) {
        LOGGER.info("Starte Simulation: Waagerechter Wurf");
    }
}
