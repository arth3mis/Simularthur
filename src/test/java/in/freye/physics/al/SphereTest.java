package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Testet die AL auf Klassenebene.
 *
 * Da alle physikalischen Berechnungen in der Klasse Shape (→ erweitert in Sphere) stattfinden,
 * werden nur Methoden dieser Klasse getestet.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SphereTest {

    double tolerance;

    /**
     * Die Toleranz wird auf 1 Nanometer eingestellt,
     * um Problemen mit der Floating-Point-Präzision vorzubeugen, da double-Werte verglichen werden.
     */
    @BeforeEach
    void setup() {
        tolerance = 1e-9;
    }

    // Notation in Kommentaren:
    // pX, pY, pZ: Komponenten der Position
    // vX, vY, vZ: Komponenten der Geschwindigkeit
    // aX, aY, aZ: Komponenten der Beschleunigung

    /**
     * Überprüft, dass die Gesamtbeschleunigung korrekt berechnet wird.
     */
    @Test
    @Order(1)
    void calcAcceleration() {
        Shape testObject = new Sphere(Vector3D.ZERO,
                // Geschwindigkeit 2m/s
                new Vector3D(0, 2, 0),
                // Eigenbeschleunigung -1.1m/s²
                new Vector3D(-1.1, 0, 0),
                // Radius 1m, Masse 2kg
                true, 1, Helper.calcSphereDensity(1, 2), 1);

        // Gravitation 5.3m/s²
        Vector3D gravity = new Vector3D(5.3, 0, 0);
        // Dichte des Mediums im Raum 1
        double airDensity = 1;
        Shape gravityEntity = new Sphere(
                // Abstand 5m zu testObject
                new Vector3D(0, 0, 5), Vector3D.ZERO, Vector3D.ZERO,
                // Radius 1m, Masse 10 Megatonnen
                false, 1, Helper.calcSphereDensity(1, 1e10), 0);

        // Ausführung der Methode
        Shape resultObject = testObject.calcAcceleration(gravity, airDensity, Lists.immutable.of(gravityEntity));

        // aX = 5.3m/s² [gravity] + -1.1m/s² [Eigenbeschleunigung]
        //    = 4.2m/s²
        //
        // aY = 0.5 * cw  * airDensity * A        * v²      / m [Strömungswiderstand]
        //    = 0.5 * cw  * airDensity * πr²    * v²      / m
        //    = 0.5 * 0.1 * 1          * π(1m)² * (2m/s)² / 2kg
        //    = 0.1π m/s²
        //    = -0.3141592655m/s² (Minus, weil entgegen der Geschwindigkeit)
        //
        // aZ = G                 * m      / r² [gravityEntity]
        //    = 6.674e-11N*m²/kg² * 1e10kg / (5m)²
        //    = 0.026696m/s²
        assertArrayEquals(
                new Vector3D(4.2, -0.3141592655, 0.026696).toArray(),
                resultObject.acc.toArray(),
                tolerance);
    }

    /**
     * Überprüft, dass die Bewegungsgleichungen korrekt ausgeführt werden.
     *
     * Setzt voraus, dass calcAcceleration korrekt funktioniert
     */
    @Test
    @Order(2)
    void applyMovement() {
        // Gravitation
        Vector3D gravity = new Vector3D(0, -9.81, 0);
        // Objekt mit konstanter Startgeschwindigkeit
        Shape testObject = new Sphere(Vector3D.ZERO, new Vector3D(1,0,0), Vector3D.ZERO, true, 1, 1, 1);

        // Gesamtbeschleunigung berechnen
        Shape result1 = testObject.calcAcceleration(gravity, 0, Lists.immutable.empty());
        // Ausführung der Methode
        Shape resultObject = result1.applyMovement(2);

        assertAll(
                // pX(t)  = v    * t
                // pX(2s) = 1m/s * 2s
                //        = 2m
                //
                // pY(t)  = 0.5 * a         * (2s)²
                // pX(2s) = 0.5 * -9.81m/s² * (2s)²
                //        = -19.62m
                //
                // pZ bleibt 0
                () -> assertArrayEquals(
                        new Vector3D(2, -19.62, 0).toArray(),
                        resultObject.pos.toArray(),
                        tolerance),
                // vX ist konstant
                //
                // vY(t)  = a         * t
                // vY(2s) = -9.81m/s² * 2s
                //          = 19.62m/s
                //
                // vZ bleibt 0
                () -> assertArrayEquals(
                        new Vector3D(1, -19.62, 0).toArray(),
                        resultObject.vel.toArray(),
                        tolerance));
    }

    /**
     * Überprüft, dass die Kollisionsberechnung mit der Wand korrekt ausgeführt wird.
     *
     * Setzt voraus, dass calcAcceleration und applyMovement korrekt funktionieren
     */
    @Test
    @Order(3)
    void handleWallCollision() {
        // Raumgröße festlegen
        Vector3D worldSize = new Vector3D(10, 10, 10);

        // Kugel mit Radius 1m (Einheiten der Vektoren für Übersicht weggelassen):
        // t = 0s
        //      p = {8.5; 5; 5}
        //      v = {0; 0; 0}
        //      a = {1; 0; 0}
        Shape testObject = new Sphere(
                new Vector3D(8.5, 5, 5),
                new Vector3D(0, 0, 0),
                new Vector3D(1, 0, 0),
                true, 1, 1, 1);
        // Zeitpunkt der Kollision mit der Wand bei x=10m (Bewegungsgleichungen: p(t) = 0.5*a*t² + v0*t + p0; v(t) = a*t + v0)
        // t = 1s
        //      p = {9; 5; 5}
        //      v = {1; 0; 0}
        //      a = {1; 0; 0}
        // Kugel wird auf diesen Zeitpunkt aktualisiert, danach wird erst die Kollisionserkennung ausgeführt
        // t = 1.1s
        //      p = {9.105; 5; 5}
        //      v = {1.1; 0; 0}
        //      a = {1; 0; 0}

        // Gesamtbeschleunigung berechnen
        Shape result1 = testObject.calcAcceleration(Vector3D.ZERO, 0, Lists.immutable.empty());
        // Bewegungsgleichungen anwenden
        Shape result2 = result1.applyMovement(1.1);
        // Ausführung der Methode
        Shape resultObject = result2.handleWallCollision(worldSize, result2);

        // Durch CCD (continuous collision detection) muss erkannt worden sein, dass die Kollision zu t = 1s stattgefunden hat.
        assertAll(
                // Position muss angepasst werden, sodass die Kugel nicht in der Wand hängt: {9; 5; 5}
                () -> assertArrayEquals(
                        new Vector3D(9, 5, 5).toArray(),
                        resultObject.pos.toArray(),
                        tolerance),
                // Geschwindigkeit muss negiert und auf den Betrag zurückgesetzt werden, den sie zur Kollisionszeit hatte,
                // ansonsten würde sie nach der Kollision höher sein als vorher: {-1; 0; 0}
                () -> assertArrayEquals(
                        new Vector3D(-1, 0, 0).toArray(),
                        resultObject.vel.toArray(),
                        tolerance));
    }

    /**
     * Überprüft, dass Position und Geschwindigkeit bei Kollision von zwei Kugeln korrigiert werden.
     *
     * Setzt voraus, dass calcAcceleration und applyMovement korrekt funktionieren
     */
    @Test
    @Order(4)
    void calcEntityCollisionCorrections() {
        // Kugeln, die sich beschleunigt aufeinander zu bewegen
        Shape testObject1 = new Sphere(
                new Vector3D(0, 0, 0),
                new Vector3D(1, 0, 0),
                new Vector3D(1, 0, 0),
                true, 1, 1, 1);
        Shape testObject2 = new Sphere(
                new Vector3D(4.9, 0, 0),
                new Vector3D(-1, 0, 0),
                new Vector3D(-1, 0, 0),
                true, 1, 1, 1);

        // deltaP(1s) = 0.5 * a * t² + v0 * t
        //            = 0.5 * 1m/s² * (1s)² + 1m/s * 1s
        //            = 1.5m
        // deltaV(1s) = a * t
        //            = 1m/s² * 1s
        //            = 1m/s
        // Beide Kugeln haben diese Strecken- und Geschwindigkeitsänderung, nur in entgegengesetzte Richtung.
        // Nach 1s ist Kugel 1 bei pX = 1.5m, mit vX = 2m/s.
        // Nach 1s ist Kugel 2 bei pX = 3.4m, mit vX = -2m/s.
        // (pX2 - pX1) = 1.9m < 2m = (r1 + r2)
        // -> Die Kugeln liegen ineinander und müssen vor dem Stoß korrigiert werden

        // Gesamtbeschleunigung berechnen
        Shape resultA1 = testObject1.calcAcceleration(Vector3D.ZERO, 0, Lists.immutable.empty());
        Shape resultA2 = testObject2.calcAcceleration(Vector3D.ZERO, 0, Lists.immutable.empty());
        // Bewegungsgleichungen anwenden
        Shape resultB1 = resultA1.applyMovement(1);
        Shape resultB2 = resultA2.applyMovement(1);
        // Ausführung der Methode
        Shape resultObject1 = resultB1.calcEntityCollisionCorrections(Lists.immutable.of(resultB2), resultA1);
        Shape resultObject2 = resultB2.calcEntityCollisionCorrections(Lists.immutable.of(resultB1), resultA2);

        assertAll(
                // Positionen werden je zur Hälfte korrigiert, demnach ist mit Überlappung von 0.1m
                // pX1' = 1.5m - 0.05m = 1.45m
                // pX2' = 3.4m + 0.05m = 3.45m
                () -> assertArrayEquals(
                        new Vector3D(1.45, 0, 0).toArray(),
                        resultObject1.pos.toArray(),
                        tolerance),
                () -> assertArrayEquals(
                        new Vector3D(3.45, 0, 0).toArray(),
                        resultObject2.pos.toArray(),
                        tolerance),
                // Geschwindigkeiten werden auf den Betrag zurückgesetzt, den sie zur Kollisionszeit hatten
                // tColl = -(v/a) + sqrt((v/a)² + 2(|p-pColl|)/a)
                //       = 0.974841766s
                // vX1' = vX1(0s) + aX1   * tColl
                //      = 1m/s    + 1m/s² * 0.974841766s
                //      = 1.9748417658m/s
                // vX2' = vX2(0s) + aX2    * tColl
                //      = -1m/s   + -1m/s² * 0.974841766s
                //      = -1.9748417658m/s
                () -> assertArrayEquals(
                        new Vector3D(1.9748417658, 0, 0).toArray(),
                        resultObject1.vel.toArray(),
                        tolerance),
                () -> assertArrayEquals(
                        new Vector3D(-1.9748417658, 0, 0).toArray(),
                        resultObject2.vel.toArray(),
                        tolerance));
    }

    /**
     * Überprüft, dass auf zwei Kugeln, die sich unmittelbar in einer Kollisionssituation befinden,
     * unabhängig korrekte Berechnungen für die Geschwindigkeiten nach dem Stoß berechnet werden.
     */
    @Test
    @Order(5)
    void applyEntityCollisionDeflections() {
        // 2 kollidierende Kugeln
        Shape testObject1 = new Sphere(
                // Verschiebung um 0.2 Nanometer, um außerhalb der Detektions-Toleranz von 0.1nm zu liegen
                new Vector3D(0.0000000002, 0, 0),
                new Vector3D(1, 0, 0),
                Vector3D.ZERO,
                true, 1, 1, 1);
        Shape testObject2 = new Sphere(
                // Da die Radien sich zu 2m addieren, ist die Aufteilung auf x und y bei einem 45°-Winkel einfach:
                new Vector3D(Math.sqrt(2), Math.sqrt(2), 0),
                new Vector3D(-1, 0, 0),
                Vector3D.ZERO,
                true, 1, 1, 1);

        // Liste erstellen
        ImmutableList<Shape> entities = Lists.immutable.of(testObject1, testObject2);
        // Ausführung der Methode
        Shape resultObject1 = testObject1.applyEntityCollisionDeflections(entities, entities);
        Shape resultObject2 = testObject2.applyEntityCollisionDeflections(entities, entities);

        assertAll(
                // Die Geschwindigkeiten haben einen Winkel von 45° zum Abstandsvektor zwischen den Mittelpunkten.
                // Daraus und aus der identischen Masse der Kugeln resultiert,
                // dass diese bei der Kollision um 90° abgelenkt werden.
                // Die Kugeln fliegen dann antiparallel auseinander, aber in y-Richtung statt in x-Richtung.
                () -> assertArrayEquals(
                        new Vector3D(0, -1, 0).toArray(),
                        resultObject1.vel.toArray(),
                        tolerance),
                () -> assertArrayEquals(
                        new Vector3D(0, 1, 0).toArray(),
                        resultObject2.vel.toArray(),
                        tolerance)
        );
    }
}
