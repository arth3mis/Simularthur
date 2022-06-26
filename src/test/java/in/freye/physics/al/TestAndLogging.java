package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PhysicableTest {

    Physicable world;
    int roundingPrecision;

    /**
     * Basisraum für Tests (Größe 10m * 10m * 10m).
     *
     * Der Raum hat ein beispielhaftes Genauigkeitsversprechen von 60 Hertz,
     * d.h. der minimale intern simulierte Zeitschritt ist 1/60s.
     * Längere Simulationszeiten werden iterativ kleinschrittig ausgeführt.
     *
     * Die Rundung wird auf 1 Nanometer Genauigkeit eingestellt,
     * um Problemen mit Floating-Point vorzubeugen, da double-Werte verglichen werden.
     */
    @BeforeEach
    void setup() {
        world = World.create(60, new Vector3D(10,10,10));
        roundingPrecision = 9;
    }

    /**
     * Überprüft, ob allgemeine Gravitation des Raums korrekt auf Position und Geschwindigkeit eines Objekts wirkt
     * (auch, wenn das Objekt bereits eine Geschwindigkeit hat)
     */
    @Test
    @DisplayName("Raum-Gravitation wirkt korrekt auf bereits bewegtes Objekt")
    void worldGravity() {
        Physicable w1 = world
                // Gravitation definieren, hier erdähnlich
                .setGravity(new Vector3D(0, -9.81, 0))
                // Kugel mit konstanter Geschwindigkeit erschaffen
                .spawn(world.at(new Vector3D(5,8,5))
                        .withVelocityAndAccel(new Vector3D(1,-1,0), Vector3D.ZERO)
                        .newSphere(1, 1, 1))
                // Zeit um 1s fortschreiten
                .update(1);

        // pX: x-Komponente der Position; vY: y-Komponente der Geschwindigkeit; ...
        assertAll(
                // pX(t)  = 0.5 * aX    * t²    + vX(0s) * t  + pX(0s)
                // pX(1s) = 0.5 * 0m/s² * (1s)² + 1m/s   * 1s + 5m
                //        = 6m
                //
                // pY(t)  = 0.5 * aY        * t²    + vY(0s)  * t  + pY(0s)
                // pY(1s) = 0.5 * -9.81m/s² * (1s)² + -1m/s   * 1s + 8m
                //        = 2.095m
                //
                // pZ ändert sich nicht.
                () -> assertArrayEquals(
                        new Vector3D(6, 2.095, 5).toArray(),
                        roundAll(w1.getEntities()[0].pos.toArray(), roundingPrecision)),
                // vX ändert sich nicht.
                //
                // vY(t)  = aY        * t
                // vY(1s) = -9.81m/s² * 1s
                //        = -9.81m/s
                //
                // vZ ändert sich nicht.
                () -> assertArrayEquals(
                        new Vector3D(1, -10.81, 0).toArray(),
                        roundAll(w1.getEntities()[0].vel.toArray(), roundingPrecision))
        );
    }

    /**
     * Überprüft, ob die Kollision mit den Wänden korrekt funktioniert
     * (auch, wenn die Geschwindigkeit nicht konstant ist)
     */
    @Test
    @DisplayName("Beschleunigtes Objekt kollidiert korrekt mit Raumgrenze")
    void wallCollision() {
        Physicable w1 = world
                // Gravitation definieren
                .setGravity(new Vector3D(1,0,0))
                // Kugel mit 75% Reflexionsstärke erschaffen
                .spawn(world.at(new Vector3D(8.5,5,5)).newSphere(1, 1, 0.75))
                // Zeit um 1s fortschreiten
                .update(1);

        // pX: x-Komponente der Position; vY: y-Komponente der Geschwindigkeit; ...
        assertAll(
                // pX(t)  = 0.5 * aX    * t²    + vX(0s) * t  + pX(0s)
                // pX(1s) = 0.5 * 1m/s² * (1s)² + 0m/s   * 1s + 8.5m
                //        = 9m
                // Radius = 1m -> Kollision mit Wand bei x = 10m tritt ein.
                //
                // pY ändert sich nicht.
                // pZ ändert sich nicht.
                () -> assertArrayEquals(
                        new Vector3D(9, 5, 5).toArray(),
                        roundAll(w1.getEntities()[0].pos.toArray(), roundingPrecision)),
                // vX(1s) = 1m/s² * 1s
                //        = 1m/s
                // Reflexionsstärke = 75% -> Faktor -0.75)
                // vX' = -0.75 * 1m/s
                //     = -0.75m/s
                //
                // vY ändert sich nicht.
                // vZ ändert sich nicht.
                () -> assertArrayEquals(
                        new Vector3D(-0.75, 0, 0).toArray(),
                        roundAll(w1.getEntities()[0].vel.toArray(), roundingPrecision))
        );
    }

    /**
     *
     */
    @Test
    @DisplayName("")
    void entityCollision() {
    }

    /**
     * Überprüft, ob ein Körper, der mit konstanter Geschwindigkeit startet,
     * eine Kreisbahn um ein massereiches Objekt fliegt und nach einer Umdrehung wieder an derselben Stelle ist.
     *
     * Aufgrund der genäherten Gravitationsberechnung gibt es eine Abweichung, die proportional zur anziehenden Masse ist.
     * Daher wird mit einer "geringeren" Masse gearbeitet, was zu einer längeren Umlaufzeit führt.
     */
    @Test
    @DisplayName("Korrekter Orbitalflug um anziehendes Objekt")
    void entityGravity() {
        // Kugel im Zentrum
        Vector3D center = new Vector3D(5,5,5);
        double centerMass = 1.5e7;  // 15000 Tonnen

        // Von Masse zur Dichte zurückrechnen (Radius = 1m)
        // m = density * 4/3 * PI * r³
        // <=> density = m    * 3/4 / PI / r³
        //     density = 1e10 * 3/4 / PI / 1
        double centerDensity = centerMass * 3.0/4.0 / Math.PI;

        // Umkreisende Kugel
        double distance = 3; // Abstand zur Mitte
        Vector3D startPos = center.add(new Vector3D(0, 0, distance));

        // Startgeschwindigkeit v0 (startVel) der umkreisenden Kugel:
        // (perfekte Kreisbahn existiert dann, wenn die Anziehungskraft die Zentripetalkraft "ist")
        // Zentripetalkraft: Fz = m1 * v0² / r
        // Gravitationskraft: Fg = G * m1 * m2 / r²
        // Fz = Fg
        // v0 = sqrt(G * m2 / r)
        Vector3D startVel = new Vector3D(Math.sqrt(World.GRAVITY_CONSTANT * centerMass / distance), 0, 0);
        
        // Umlaufzeit T der umkreisenden Kugel:
        // v = 2 * PI * r / T
        // <=> T = 2 * PI * r / v
        double orbitalPeriod = 2 * Math.PI * distance / startVel.getNorm();

        // Gravitation ist 0 (Standard)
        Physicable w1 = world
                // massereiche Kugel im Zentrum erschaffen
                .spawn(world.at(center).newSphere(1, centerDensity, 1))
                // Kugel im Orbit mit Startgeschwindigkeit (senkrecht zum Abstandsvektor) erschaffen
                .spawn(world.at(startPos)
                        .withVelocityAndAccel(startVel, Vector3D.ZERO)
                        .newSphere(1, 1, 1))
                // Zeit um orbitale Umlaufzeit fortschreiten
                .update(orbitalPeriod);

        // Geschwindigkeit weicht weniger ab als Position
        int posRoundingPrecision = 1;
        int velRoundingPrecision = 3;

        assertAll(
                // Position ist wie vor der Umlaufzeit
                () -> assertArrayEquals(
                        startPos.toArray(),
                        roundAll(w1.getEntities()[1].pos.toArray(), posRoundingPrecision)),
                // Geschwindigkeit ist wie vor der Umlaufzeit
                () -> assertArrayEquals(
                        roundAll(startVel.toArray(), velRoundingPrecision),
                        roundAll(w1.getEntities()[1].vel.toArray(), velRoundingPrecision))
        );
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
        LOGGER.info("Starte Simulation: Waagerechter Wurf mit Kollisionen");
    }
}
