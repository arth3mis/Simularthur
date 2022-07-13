package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hilfsmethoden zum Berechnen
 */
class Helper {

    /** Rechnet von Masse und Radius auf die Dichte einer Kugel zurück */
    static double calcSphereDensity(double radius, double mass) {
        // m = density * 4/3 * π * r³
        // density = m * 3/4 / π / r³
        return mass * 3.0/4.0 / Math.PI / radius/radius/radius;
    }
}

/**
 * Testet die AL über das Interface.
 */
class PhysicableTest {

    Physicable world;
    double tolerance;
    double updateFreq;

    /**
     * Basisraum für Tests (Größe 10m · 10m · 10m).
     *
     * Der Raum hat ein beispielhaftes Genauigkeitsversprechen von 60 Hertz,
     * d.h. der minimale intern simulierte Zeitschritt ist 1/60s.
     * Längere Simulationszeiten werden (intern) iterativ kleinschrittig ausgeführt.
     *
     * Die Toleranz wird auf 1 Nanometer eingestellt,
     * um Problemen mit der Floating-Point-Präzision vorzubeugen, da double-Werte verglichen werden.
     */
    @BeforeEach
    void setup() {
        updateFreq = 60;
        world = World.create(updateFreq, new Vector3D(10, 10, 10));
        tolerance = 1e-9;
    }

    /**
     * Überprüft, dass allgemeine Gravitation des Raums korrekt auf Position und Geschwindigkeit eines Objekts wirkt
     * (auch, wenn das Objekt bereits eine Geschwindigkeit hat).
     */
    @Test
    @DisplayName("Raum-Gravitation wirkt korrekt auf bereits bewegtes Objekt")
    void worldGravity() {
        Physicable w1 = world
                // Gravitation definieren, hier erdähnlich
                .setGravity(new Vector3D(0, -9.81, 0))
                // Kugel mit konstanter Geschwindigkeit erschaffen
                .spawn(world.createSpawnableAt(new Vector3D(5, 8, 5))
                        .withVelocityAndAccel(new Vector3D(1, -1, 0), Vector3D.ZERO)
                        .ofTypeSphere(1, 1, 1))
                // Zeit um 1s fortschreiten
                .simulateTime(1);

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
                        w1.getEntities()[0].getPos().toArray(),
                        tolerance),
                // vX ändert sich nicht.
                //
                // vY(t)  = aY        * t  + vY(0s)
                // vY(1s) = -9.81m/s² * 1s + -1m/s
                //        = -10.81m/s
                //
                // vZ ändert sich nicht.
                () -> assertArrayEquals(
                        new Vector3D(1, -10.81, 0).toArray(),
                        w1.getEntities()[0].getVel().toArray(),
                        tolerance)
        );
    }

    /**
     * Überprüft, dass sich bei beschleunigter Bewegung und Strömungswiderstand nach einiger Zeit eine konstante Geschwindigkeit einstellt.
     */
    @Test
    @DisplayName("Strömungswiderstand wirkt korrekt auf beschleunigtes Objekt")
    void drag() {
        Physicable w1 = world
                // Gravitation definieren
                .setGravity(new Vector3D(0, -1, 0))
                // Raum mit Luft füllen
                .setAirDensity(1.2)
                // leichte, große Kugel erschaffen, die viel Luftwiderstand erfahren kann
                .spawn(world.createSpawnableAt(new Vector3D(5, 8, 5))
                        .ofTypeSphere(2, Helper.calcSphereDensity(2, 0.1), 1));

        // Konstante Geschwindigkeit mit Strömungswiderstand (Fw) und Gravitation (Fg) tritt ein für:
        // Fw = |Fg| (→ durch Masse m teilen)
        // a = |g|
        // 0.5 * dragCoefficient * airDensity * A * v² / m = |g|
        // <=> v = sqrt(2 * m     * |g|   / dragCoefficient / airDensity / A)
        //     v = sqrt(2 * 0.1kg * 1m/s² / 0.1             / 1.2        / (π(2m)²))
        //     v = 0.36418281m/s

        // Aktualisieren, bis a und g denselben Betrag haben (Gesamtbeschleunigung = 0)
        do {
            w1 = w1.simulateTime(1/updateFreq);
        } while (w1.getEntities()[0].getAcc().getNorm() > tolerance);

        // Die Geschwindigkeit muss jetzt den Wert v haben (siehe oben)
        assertArrayEquals(
                new Vector3D(0, -0.36418281, 0).toArray(),
                w1.getEntities()[0].getVel().toArray(),
                tolerance);
    }

    /**
     * Überprüft, dass die Kollision mit den Wänden korrekt funktioniert
     * (auch, wenn die Geschwindigkeit nicht konstant ist).
     */
    @Test
    @DisplayName("Beschleunigtes Objekt kollidiert korrekt mit Raumgrenze")
    void wallCollision() {
        Physicable w1 = world
                // Gravitation definieren
                .setGravity(new Vector3D(1, 0, 0))
                // Kugel mit 75% Reflexionsstärke erschaffen
                .spawn(world.createSpawnableAt(new Vector3D(8.5, 5, 5)).ofTypeSphere(1, 1, 0.75))
                // Zeit um 1s fortschreiten
                .simulateTime(1);

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
                        w1.getEntities()[0].getPos().toArray(),
                        tolerance),
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
                        w1.getEntities()[0].getVel().toArray(),
                        tolerance)
        );
    }

    /**
     * Überprüft korrekte Kollision zwischen zwei Kugeln:
     *  - Frontalkollision, verschiedene Massen (testet Impulserhaltung)
     *  - versetzte Positionen, gleiche Massen (testet Reflexionswinkel).
     * Die Aufspaltung in zwei Teil-Tests ermöglicht leichter berechenbare & nachvollziehbare erwartete Werte.
     */
    @Test
    @DisplayName("Korrekte Kollisionen zwischen zwei Kugeln")
    void entityCollision() {
        // Werte für 1. Kugelpaar (Massen & Geschwindigkeiten)
        double m1 = 1, m2 = 2;
        double v1 = 1, v2 = -1;

        // Gravitation ist 0 (Standard)
        Physicable w1 = world
                // Die Kugelpaare haben einen großen z-Abstand und bewegen sich nicht in z-Richtung; keine Interaktion zwischen den Paaren
                .spawn(
                        // 1. Kugelpaar (frontal, verschiedene Massen)
                        world.createSpawnableAt(new Vector3D(3, 5, 3))
                                .withVelocityAndAccel(new Vector3D(v1, 0, 0), Vector3D.ZERO)
                                .ofTypeSphere(1, m1, 1),  // Masse wird als Dichte übergeben, aber durch gleichen Radius stimmt das Verhältnis
                        world.createSpawnableAt(new Vector3D(7, 5, 3))
                                .withVelocityAndAccel(new Vector3D(v2, 0, 0), Vector3D.ZERO)
                                .ofTypeSphere(1, m2, 1),
                        // 2. Kugelpaar (versetzt, gleiche Massen)
                        // Der Aufbau soll dem in der Grafik von Wikipedia gleichen: https://upload.wikimedia.org/wikipedia/commons/2/2c/Elastischer_sto%C3%9F_2D.gif.
                        world.createSpawnableAt(new Vector3D(5, 5, 7))
                                .withVelocityAndAccel(new Vector3D(1, 0, 0), Vector3D.ZERO)
                                .ofTypeSphere(1, 1, 1),
                        world.createSpawnableAt(new Vector3D(6 + Math.sqrt(2),5 + Math.sqrt(2),7))
                                .ofTypeSphere(1, 1, 1))
                // Zeit um etwas mehr als 1s fortschreiten.
                // Kollisionen passieren ca. dort, durch sqrt() und andere Berechnungen nicht exakt bei 1s
                .simulateTime(1.0000001);

        assertAll(
                // 1. Kollision:
                // Geschwindigkeiten nach dem Stoß: aus Impuls-/Energieerhaltung hergeleitet, siehe: https://www.leifiphysik.de/mechanik/impulserhaltung-und-stoesse/grundwissen/zentraler-elastischer-stoss
                // v1' = (m1*v1 + m2*(2*v2-v1)) / (m1+m2)
                () -> assertArrayEquals(
                        new double[]{(m1*v1 + m2*(2*v2-v1)) / (m1+m2), 0, 0},
                        w1.getEntities()[0].getVel().toArray(),
                        tolerance),
                // v2' = (m2*v2 + m1*(2*v1-v2)) / (m1+m2)
                () -> assertArrayEquals(
                        new double[]{(m2*v2 + m1*(2*v1-v2)) / (m1+m2), 0, 0},
                        w1.getEntities()[1].getVel().toArray(),
                        tolerance),
                // 2. Kollision:
                // Sollte stattfinden wie in der Grafik von Wikipedia beschrieben: https://upload.wikimedia.org/wikipedia/commons/2/2c/Elastischer_sto%C3%9F_2D.gif.
                // Demnach müssen die Geschwindigkeiten nach dem Stoß senkrecht zueinander sein,
                // überprüfbar mit dem Skalarprodukt zwischen ihnen (muss 0 ergeben)
                () -> assertEquals(
                        0.0,
                        w1.getEntities()[3].getVel().dotProduct(w1.getEntities()[2].getVel()),
                        tolerance)
        );
    }

    /**
     * Überprüft, dass ein Körper, der mit konstanter Geschwindigkeit startet,
     * eine Kreisbahn um ein massereiches Objekt fliegt und nach einer Umdrehung wieder an derselben Stelle ist.
     *
     * Aufgrund der genäherten Gravitationsberechnung gibt es eine Abweichung bei Position und Geschwindigkeit,
     * daher ist die Toleranz beim Vergleichen der Werte größer als die global definierte.
     */
    @Test
    @DisplayName("Korrekter Orbital-Flug um anziehendes Objekt")
    void entityGravity() {
        // Kugel im Zentrum
        Vector3D center = new Vector3D(5, 5, 5);
        double centerMass = 1.5e7;  // 15000 Tonnen
        double centerDensity = Helper.calcSphereDensity(1, centerMass);

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
        // v = 2 * π * r / T
        // <=> T = 2 * π * r / v
        double orbitalPeriod = 2 * Math.PI * distance / startVel.getNorm();

        // Gravitation ist 0 (Standard)
        Physicable w1 = world
                // massereiche Kugel im Zentrum erschaffen
                .spawn(world.createSpawnableAt(center).ofTypeSphere(1, centerDensity, 1))
                // Kugel im Orbit mit Startgeschwindigkeit (senkrecht zum Abstandsvektor) erschaffen
                .spawn(world.createSpawnableAt(startPos)
                        .withVelocityAndAccel(startVel, Vector3D.ZERO)
                        .ofTypeSphere(1, 1, 1));
        // Zeit um orbitale Umlaufzeit fortschreiten
        Physicable w2 = w1.simulateTime(orbitalPeriod/2);  // Halber Umlauf
        Physicable w3 = w2.simulateTime(orbitalPeriod/2);  // Ganzer Umlauf

        // Geschwindigkeit weicht weniger ab als Position
        double posTolerance = 1e-2;
        double velTolerance = 1e-4;

        // Umkreisende Kugel hat den Index 1 in der Entity-Liste des Systems
        assertAll(
                // Halber Umlauf
                // Position ist auf der anderen Seite des Zentrums
                () -> assertArrayEquals(
                        center.add(new Vector3D(0, 0, -distance)).toArray(),
                        w2.getEntities()[1].getPos().toArray(),
                        posTolerance),
                // Geschwindigkeit ist die Negation der Startgeschwindigkeit (außer y-Wert, da der Orbit in der xz-Ebene liegt)
                () -> assertArrayEquals(
                        new double[]{-startVel.getX(), startVel.getY(), -startVel.getZ()},
                        w2.getEntities()[1].getVel().toArray(),
                        velTolerance),
                // Ganzer Umlauf
                // Position ist wie vor der Umlaufzeit
                () -> assertArrayEquals(
                        startPos.toArray(),
                        w3.getEntities()[1].getPos().toArray(),
                        posTolerance),
                // Geschwindigkeit ist wie vor der Umlaufzeit
                () -> assertArrayEquals(
                        startVel.toArray(),
                        w3.getEntities()[1].getVel().toArray(),
                        velTolerance)
        );
    }
}


/**
 * Testet die AL auf Klassenebene.
 *
 * Da alle physikalischen Berechnungen in der Klasse Shape (→ erweitert in Sphere) stattfinden,
 * werden nur Methoden dieser Klasse getestet.
 */
class SphereTest {

    double tolerance;

    /**
     * Die Toleranz wird auf 1 Nanometer eingestellt,
     * um Problemen mit der Floating-Point-Präzision vorzubeugen, da double-Werte verglichen werden.
     */
    @BeforeEach
    void setup() {
        tolerance = 1e-9;
    }

    /**
     * Überprüft, dass die Gesamtbeschleunigung korrekt berechnet wird.
     */
    @Test
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


/**
 * Erzeugt eine Simulation, die mithilfe von Monitoring nachvollzogen werden kann.
 * Um eine übersichtliche Anzahl von Log-Ausgaben zu erhalten, wird das Genauigkeitsversprechen reduziert.
 * Die simulierten Werte geben trotzdem das erwartete Verhalten korrekt wieder.
 *
 * Das Monitoring muss in der Datei "/src/main/resources/log4j2.xml" eingeschaltet werden
 */
class LoggingScenario {

    private static final Logger LOGGER = LogManager.getLogger("monitoring");

    public static void main(String[] args) {
        LOGGER.info("Starte Simulation: Waagerechter Wurf mit Kollisionen");

        // Welt mit Gravitation erschaffen
        Physicable w0 = World.create(10, new Vector3D(10, 10, 10))
                .setGravity(new Vector3D(0, -1, 0));
        // Kugeln erschaffen
        Physicable w1 = w0.spawn(
                w0.createSpawnableAt(new Vector3D(5, 3, 2))
                        .withVelocityAndAccel(new Vector3D(1, 0, 0), Vector3D.ZERO)
                        .ofTypeSphere(1, 1, 1),
                w0.createSpawnableAt(new Vector3D(8, 2.5, 2))
                        .immovable()
                        .ofTypeSphere(1, 1));
        // Zeit simulieren: Die Kollisionen treten zu t=1s und t=2s ein
        w1.simulateTime(2.1);
    }
}
