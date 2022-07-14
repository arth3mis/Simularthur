package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Testet die AL über das Interface.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PhysicableTest {

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

    // Notation in Kommentaren:
    // pX, pY, pZ: Komponenten der Position
    // vX, vY, vZ: Komponenten der Geschwindigkeit
    // aX, aY, aZ: Komponenten der Beschleunigung

    /**
     * Überprüft, dass allgemeine Gravitation des Raums korrekt auf Position und Geschwindigkeit eines Objekts wirkt
     * (auch, wenn das Objekt bereits eine Geschwindigkeit hat).
     */
    @Test
    @Order(1)
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
    @Order(2)
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
    @Order(3)
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
    @Order(4)
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
    @Order(5)
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

